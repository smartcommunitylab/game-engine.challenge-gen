package eu.fbk.das.rs;

import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.getWeeklyContentMode;
import static eu.fbk.das.rs.challenges.generation.RecommendationSystem.getChallengeWeek;
import static eu.fbk.das.utils.Utils.f;
import static eu.fbk.das.utils.Utils.p;
import static eu.fbk.das.utils.Utils.pf;
import static eu.fbk.das.utils.Utils.rand;
import static eu.fbk.das.utils.Utils.sortByValues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.joda.time.DateTime;

import eu.fbk.das.GamificationEngineRestFacade;
import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.rs.challenges.ChallengeUtil;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemStatistics;
import eu.fbk.das.utils.ArrayUtils;
import eu.fbk.das.utils.Pair;
import it.smartcommunitylab.JSON;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.ChallengeAssignmentDTO;
import it.smartcommunitylab.model.ext.ChallengeConcept;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.GroupChallengeDTO.AttendeeDTO;
import it.smartcommunitylab.model.ext.GroupChallengeDTO.PointConceptDTO;
import it.smartcommunitylab.model.ext.PointConcept;

public class GroupChallengesAssigner extends ChallengeUtil {

    private final int timelimit;

    private String[] groupCha = new String[] {"groupCooperative", "groupCompetitiveTime", "groupCompetitivePerformance"};

    private double bcost = 3;
    private List<GroupExpandedDTO> groupChallenges;
    private Set<String> modelTypes;

    private DateTime execDate;
    private DateTime startDate;
    private DateTime endDate;

    public GroupChallengesAssigner(RecommendationSystem rs) {
        super(rs);

        Arrays.sort(groupCha);

        playerLimit = 0;
        timelimit = 600;
        // minLvl = 4;
        minLvl = 1; // REMOVE ME!
    }

    public List<GroupExpandedDTO> execute(Set<String> players, Set<String> modelTypes, String assignmentType, Map<String, Object> challengeValues) {

        System.out.println("**td**");
    	execDate = new DateTime(challengeValues.get("exec"));
        Pair<Date, Date> challengeDates = GamificationEngineRestFacade
                .getDates(challengeValues.get("start"), challengeValues.get("duration"));
        startDate = new DateTime(challengeDates.getFirst());
        endDate = new DateTime(challengeDates.getSecond());

        prepare(getChallengeWeek(execDate));

        groupChallenges = new ArrayList<>();

        players = filterLevel(players);

        players = filterPlayersAlreadyAssignedToGroupChallenge(players);

        players = removeNotPartecipating(players);

        if (players.size() == 0)
            return new ArrayList<>();

        RecommendationSystemStatistics stats = rs.getStats();

        // TODO CHECK
        stats.checkAndUpdateStats(execDate);

        HashMap<String, HashMap<String, Double>> playersCounterAssignment = getPlayerCounterAssignment(players, stats, modelTypes);
        
        System.out.println("#####################");
        System.out.println("startDate -> " + startDate);
        System.out.println("endDate -> " + endDate);
        System.out.println("execDate -> " + execDate);
        System.out.println("playersCounterAssignment");
        playersCounterAssignment.entrySet().forEach(entry -> {
            System.out.println(entry.getKey() + " " + entry.getValue());
        });
        System.out.println("#####################");

        TargetPrizeChallengesCalculator tpcc = new TargetPrizeChallengesCalculator();
        tpcc.prepare(rs, rs.gameId, execDate);

        for (String mode : modelTypes) {

            // Make sure they are all even
            Map<String, Double> res = checkEven(playersCounterAssignment.get(mode));

            Map<String, Integer> playersQuant = getPlayersQuantile(res, stats.getQuantiles(mode));

            List<Pair<String, String>> reduced = reduce(playersQuant);

            List<Pair<String, String>> pairs = chocoModel(playersQuant);
            pairs.addAll(reduced);

            for (Pair<String, String> p: pairs) {
                prepareGroupChallenge(assignmentType, tpcc, mode, p, rs.gameId);
            }
        }

        return groupChallenges;
    }

    private Set<String> filterLevel(Set<String> pl) {
        Set<String> players = new HashSet<>();
        for (String pId: pl) {
            PlayerStateDTO p = rs.facade.getPlayerState(rs.gameId, pId);
            int lvl = getLevel(p);
            if (minLvl < 0 || lvl >=  minLvl) {
                players.add(pId);
                if (playerLimit != 0 && players.size() >= playerLimit)
                    break;
            }
        }
        return players;
    }

    private Set<String> removeNotPartecipating(Set<String> pl) {
        Set<String> players = new HashSet<>();

        for (String pId: pl) {

            boolean active = false;

            PlayerStateDTO player = rs.facade.getPlayerState(rs.gameId, pId);

            Set<GameConcept> scores =
                    player.getState().get("PointConcept");

            for (GameConcept gc : scores) {
                PointConcept pc = (PointConcept) gc;
                if (!pc.getName().equals("green leaves"))
                    continue;

                Double sc = getPeriodScore(pc, "weekly", execDate);

                if (sc > 20)
                    active = true;

            }

            if (active)
                players.add(pId);
        }

        return players;
    }

    private Set<String> filterPlayersAlreadyAssignedToGroupChallenge(Set<String> playersList) {
        Set<String> players = new HashSet<>();

        for (String playerId: playersList) {
            List<it.smartcommunitylab.model.ChallengeConcept> currentChallenges = rs.facade.getChallengesPlayer(rs.gameId, playerId);
            boolean missingAssignedGroupChallenges =
                    currentChallenges.stream().filter(c -> c.getModelName().contains("group"))
                    .filter(c -> startDate.isEqual(new DateTime(c.getStart())))
                            .count() == 0;

            if (missingAssignedGroupChallenges) {
                players.add(playerId);

            }
        }
        return players;
    }

    private Map<String, Integer> getPlayersQuantile(Map<String, Double> res, Map<Integer, Double> quantiles) {
        Map<String, Integer> playersQuant = new HashMap<>();
        for (String player: res.keySet()) {
            int level = getQuantile(res.get(player), quantiles);
            playersQuant.put(player, level);
        }
        return playersQuant;
    }

    private void prepareGroupChallenge(String type, TargetPrizeChallengesCalculator tpcc, String mode, Pair<String, String> p, String gameId) {

        GroupExpandedDTO gcd;
        if (type.equals("groupCompetitivePerformance"))
            gcd = createPerfomanceChallenge(mode, p.getFirst(),  p.getSecond(),
                    startDate, endDate);
        else {
            Map<String, Double> result = tpcc.targetPrizeChallengesCompute(p.getFirst(), p.getSecond(), mode, type);
            pf("%.2f, %s, %.2f, %.0f, %s, %.2f, %.0f, %s, %s\n",
                    result.get("target"),
                    p.getFirst(), result.get("player1_bas"), result.get("player1_prz"),
                    p.getSecond(), result.get("player2_bas"), result.get("player2_prz"),
                    mode, type);



            gcd = rs.facade.makeGroupChallengeDTO(rs.gameId,
                    type, mode, p.getFirst(), p.getSecond(),
                    startDate, endDate, result
            );
        }

        gcd.setInfo("gameId", gameId);

        groupChallenges.add(gcd);


    }

    public GroupExpandedDTO createPerfomanceChallenge(String counter, String pId1, String pId2, DateTime start, DateTime end) {

        GroupExpandedDTO gcd = new GroupExpandedDTO();
        gcd.setChallengeModelName("groupCompetitivePerformance");

        List<AttendeeDTO> attendee = new ArrayList<>();
        AttendeeDTO att1 = new AttendeeDTO();
        att1.setRole("GUEST");
        att1.setPlayerId(pId1);
        attendee.add(att1);
        AttendeeDTO att2 = new AttendeeDTO();
        att2.setRole("GUEST");
        att2.setPlayerId(pId2);
        attendee.add(att2);
        gcd.setAttendees(attendee);

        PointConceptDTO pc = new PointConceptDTO();
        pc.setName(counter);
        pc.setPeriod("weekly");
        gcd.setChallengePointConcept(pc);

        // TODO FIX
        // RewardDTO rw = new RewardDTO();
        // rw.setBonusScore(250);

        gcd.setOrigin("gca");
        gcd.setState("ASSIGNED");
        gcd.setStart(start.toDate());
        gcd.setEnd(end.toDate());

        return gcd;
    }

    private HashMap<String, HashMap<String, Double>> getPlayerCounterAssignment(Set<String> players, RecommendationSystemStatistics stats, Set<String> modeList) {

        HashMap<String, HashMap<String, Double>> playersToConsider = new HashMap<String, HashMap<String, Double>>();

        for (String mode : modeList)
            playersToConsider.put(mode, new HashMap<String, Double>());

        for (String pId : players) {

            HashMap<String, Double> vl = new HashMap<>();
            List<String> aux = new ArrayList<String>();

            for (String counter : modeList) {
                PlayerStateDTO pla = rs.facade.getPlayerState(rs.gameId, pId);
                Double baseline = getWMABaseline(pla, counter, execDate);
                Map<Integer, Double> quant = stats.getQuantiles(counter);
                int q = getQuantile(baseline, quant);
                if (q == -1) {
                    p("this is reeeeeaaaally strange");
                    continue;
                }

                // if (q < 2)
                //    continue;

                // check if it has already a group challenge with the same counter accepted
                if (hasGroupChallenge(pla, counter))
                    continue;

                vl.put(counter, baseline);
                aux.add(counter);
            }

            if (vl.size() == 0)
                continue;

            String chosen = aux.get(rand(aux.size()));
            playersToConsider.get(chosen).put(pId, vl.get(chosen));
        }
        return playersToConsider;
    }

    private boolean hasGroupChallenge(PlayerStateDTO state, String counter) {
        Set<GameConcept> scores =  state.getState().get("ChallengeConcept");
        if (scores!= null) {
            for (GameConcept gc : scores) {
                ChallengeConcept cha = (ChallengeConcept) gc;
                DateTime start = new DateTime(cha.getStart());

                if (start.getWeekOfWeekyear() != execDate.getWeekOfWeekyear())
                    continue;

                 String s = cha.getModelName();
                 Map<String, Object> fields = (Map<String, Object>) cha.getFields();

                 if (!ArrayUtils.find(s, groupCha))
                     continue;

                 if (!fields.get("challengeScoreName").equals(counter))
                     continue;

                 if (!cha.getStateDate().keySet().contains("ASSIGNED"))
                     continue;

                //  p(start.getWeekOfWeekyear());
                //  p(execDate.getWeekOfWeekyear());

                 // p(fields.keySet());

                    return true;


             }        	
        }
        return false;
    }

    private Map<String, Double> checkEven(HashMap<String, Double> originalList) {

        Map<String, Double> list = sortByValues(originalList);

        if (list.size() % 2 == 0)
            return  list;

        // worst
        String pId = list.keySet().iterator().next();
        list.remove(pId);

        pf("Assigning repetitive(single challenge) to: %s \n", pId);

        // ASSIGN repetitive behaviour
        ChallengeAssignmentDTO rep = rs.rscg.getRepetitive(pId);
        System.out.println("********* single challenge ******");
        System.out.println(rep.getInstanceName());
        rep.getData().entrySet().forEach(entry -> {
            System.out.println(entry.getKey() + " " + entry.getValue());
        });
        System.out.println(rep.getStart());
        System.out.println(rep.getEnd());
        System.out.println("***********************************");
        rs.facade.assignChallengeToPlayer(rep, rs.gameId, pId);

        return list;
    }

    private Double getWMABaseline(PlayerStateDTO state, String counter, DateTime lastMonday) {

        DateTime date = lastMonday;
        int v = 5;

        double den = 0;
        double num = 0;
        for (int ix = 0; ix < v; ix++) {
            // weight * value
            Double c = getWeeklyContentMode(state, counter, date);
            den += (v -ix) * c;
            num += (v -ix);

            date = date.minusDays(7);
        }

        double baseline = den / num;
        return baseline;
    }


    private int getQuantile(Double c, Map<Integer, Double> quant) {
        for (int i = 0; i < 10; i++) {
            if (c < quant.get(i))
                return i;
        }

        return 9;
    }

    public List<Pair<String, String>> chocoModel(Map<String, Integer> playersQuant) {

        List<String>  players = new ArrayList<>(playersQuant.keySet());
        Collections.sort(players);
        int N = players.size();
        Model model = new Model("group assignment");

        int[][] cost = getMatrixCost(players, playersQuant);

        // variables
        IntVar[][] vs = model.intVarMatrix("vs", N, N, 0, 1);

        // only one value for row
        for(int i  = 0; i < N; i++) {
            model.sum(vs[i], "=", 1).post();
        }

        // only one value for column
        for(int j  = 0; j < N; j++) {
            IntVar[] aux = new IntVar[N];
            for (int i = 0; i < N; i++)
                aux[i] = vs[i][j];
            model.sum(aux, "=", 1).post();
        }

        // not with same
        for(int i  = 0; i < N; i++) {
            vs[i][i].eq(0).post();
        }

        for(int i  = 0; i < N; i++) {
            for(int j  = i + 1; j < N; j++) {
                vs[i][j].eq(vs[j][i]).post();
            }
        }

        IntVar[] aux_cost = model.intVarArray("ac", N, 0, 99999,true);
        for(int i  = 0; i < N; i++) {
            model.scalar(vs[i], cost[i], "=", aux_cost[i]).post();
        }
        IntVar tot_cost = model.intVar("C", 0, 99999, true);
        model.sum(aux_cost, "=", tot_cost).post();

        model.setObjective(Model.MINIMIZE, tot_cost);

        Solver solver = model.getSolver();
        // Find a solution that minimizes 'tot_cost'
        solver.limitTime(f("%ds", timelimit));


        Solution best = solver.findOptimalSolution(tot_cost, false);
        solver.printStatistics();
        List<Pair<String, String>> pr = new ArrayList<>();
        for(int i  = 0; i < N; i++) {
            for(int j  =  i +1; j < N; j++) {
                if (best.getIntVal(vs[i][j]) > 0) {
                    pr.add(new Pair<>(players.get(i), players.get(j)));
                    pf("Pair %s (%d) - %s (%d): cost %d \n", players.get(i), playersQuant.get(players.get(i)),
                            players.get(j), playersQuant.get(players.get(j)),  cost[i][j]);
                }
            }
        }

         p("completed");

        return pr;
    }

    // already assign as many as possible, in order to reduce complexity
    protected List<Pair<String, String>> reduce(Map<String, Integer> playersQuant) {

        Map<Integer, ArrayList<String>> quant = new HashMap<>();

        List<Pair<String, String>> reduced = new ArrayList<>();

        for (String player: playersQuant.keySet()) {
            Integer lvl = playersQuant.get(player);
            if (!quant.containsKey(lvl))
                quant.put(lvl, new ArrayList<>());
            quant.get(lvl).add(player);
        }

        for (Integer lvl: quant.keySet()) {
            ArrayList<String> list = quant.get(lvl);
            while (list.size() > 2) {
                int first = rand(list.size());
                int second = first;
                while (second == first)
                    second = rand(list.size());

                String p_1 = list.get(first);
                String p_2 = list.get(second);

                Pair<String, String> p = new Pair<String, String>(p_1, p_2);
                reduced.add(p);

                pf("Pair %s (%d) - %s (%d): cost %d \n", p_1, playersQuant.get(p_1),
                        p_2, playersQuant.get(p_2), 1);

                list.remove(p_1);
                playersQuant.remove(p_1);
                list.remove(p_2);
                playersQuant.remove(p_2);


            }
        }

        return reduced;

    }

    // Get string array with all PlayerStateDTO IDs combined (make sure it's even)
    private String[] getActualPlayers(HashMap<Integer, ArrayList<String>> playerQuantile) {
        HashSet<String> playersToConsider = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            ArrayList<String> list = playerQuantile.get(i);
            if (list != null)
            playersToConsider.addAll(list);
        }

        String[] a_playersToConsider = playersToConsider.toArray(new String[playersToConsider.size()]);
        if (a_playersToConsider.length % 2 == 1) {
            String[] aux = new String[a_playersToConsider.length - 1];
            ArrayUtils.cloneArray(a_playersToConsider, aux);
            a_playersToConsider = aux;
        }

        return a_playersToConsider;
    }

    private int[][] getMatrixCost(List<String> players, Map<String, Integer> playersPerf) {
        int N = players.size();

        int[][] cost = new int[N][];
        for (int i = 0; i < N; i++) {
            cost[i] = new int[N];
        }

        for (int i = 0; i < N; i++) {
            int rank_i = playersPerf.get(players.get(i));
            for (int j = i + 1; j < N; j++) {
                int rank_j = playersPerf.get(players.get(j));
                int dist = Math.abs(rank_i - rank_j);
                cost[i][j] = (int) Math.pow(bcost, dist);
                cost[j][i] = cost[i][j];
            }
        }
        return cost;
    }

    private int position(String pId, ArrayList<String[]> rankPosition) {
        for (int i = 0; i < 10; i++) {
            if (ArrayUtils.binarySearch(rankPosition.get(i), pId) >= 0)
                return i;
        }

        p("STRAAAAANGE");
        return 0;

    }

}
