package eu.fbk.das.rs;

import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.ChallengeUtil;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemStatistics;
import eu.fbk.das.rs.utils.ArrayUtils;
import eu.fbk.das.rs.utils.Pair;
import eu.trentorise.game.challenges.model.GroupChallengeDTO;
import eu.trentorise.game.challenges.rest.ChallengeConcept;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.Player;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.joda.time.DateTime;

import java.util.*;

import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.getWeeklyContentMode;
import static eu.fbk.das.rs.utils.Utils.*;

public class GroupChallengesAssigner extends ChallengeUtil {

    private final int timelimit;
    private RecommendationSystem rs;

    private String[] groupCha = new String[] {"groupCooperative", "groupCompetitiveTime", "groupCompetitivePerformance"};

    private double bcost = 3;
    private List<GroupChallengeDTO> groupChallenges;

    public GroupChallengesAssigner(RecommendationSystemConfig cfg, GamificationEngineRestFacade facade) {
        super(cfg);
        setFacade(facade);

        rs = new RecommendationSystem();
        execDate = new DateTime();

        // TODO
        // TOREMOVE!!!
        execDate = execDate.minusDays(7);

        Arrays.sort(groupCha);

        playerLimit = 0;
        timelimit = 600;
        minLvl = 3;
    }

    public void execute() {

        prepare(execDate);

        groupChallenges = new ArrayList<>();

        // String type = "groupCompetitiveTime";
        String type = "groupCooperative";

        List<String> players = getPlayers();

        RecommendationSystemStatistics stats = rs.getStats();
        rs.facade = facade;
        stats.checkAndUpdateStats(facade, execDate, cfg, cfg.get("HOST"));

        String[] modeList = new String[]{ChallengesConfig.WALK_KM, ChallengesConfig.BIKE_KM, ChallengesConfig.GREEN_LEAVES};

        HashMap<String, HashMap<String, Double>> playersCounterAssignment = getPlayerCounterAssignment(players, stats, modeList);

        TargetPrizeChallengesCalculator tpcc = new TargetPrizeChallengesCalculator();
        tpcc.prepare(rs, cfg.get("GAME_ID"));

        for (String mode : modeList) {

            // Make sure they are all even
            Map<String, Double> res = checkEven(playersCounterAssignment.get(mode));

            Map<String, Integer> playersQuant = getPlayersQuantile(res, stats.getQuantiles(mode));

            List<Pair<String, String>> reduced = reduce(playersQuant);

            List<Pair<String, String>> pairs = chocoModel(playersQuant);
            pairs.addAll(reduced);

            for (Pair<String, String> p: pairs) {
                prepareGroupChallenge(type, tpcc, mode, p);
            }
        }

        for (GroupChallengeDTO gcd: groupChallenges) {
            // facade.assignGroupChallenge(gcd, cfg.get("GAME_ID"));
        }

    }

    private Map<String, Integer> getPlayersQuantile(Map<String, Double> res, Map<Integer, Double> quantiles) {
        Map<String, Integer> playersQuant = new HashMap<>();
        for (String player: res.keySet()) {
            int level = getQuantile(res.get(player), quantiles);
            playersQuant.put(player, level);
        }
        return playersQuant;
    }

    private void prepareGroupChallenge(String type, TargetPrizeChallengesCalculator tpcc, String mode, Pair<String, String> p) {
        Map<String, Double> result = tpcc.targetPrizeChallengesCompute(p.getFirst(), p.getSecond(), mode, type);
        pf("%.2f, %s, %.2f, %.0f, %s, %.2f, %.0f, %s, %s\n",
                result.get("target"),
                p.getFirst(), result.get("player1_bas"), result.get("player1_prz"),
                p.getSecond(), result.get("player2_bas"), result.get("player2_prz"),
                mode, type);



        GroupChallengeDTO gcd = facade.makeGroupChallengeDTO(
                type, mode, p.getFirst(),  p.getSecond(),
                startDate, endDate, result
        );

        groupChallenges.add(gcd);


    }

    private HashMap<String, HashMap<String, Double>> getPlayerCounterAssignment(List<String> players, RecommendationSystemStatistics stats, String[] modeList) {

        HashMap<String, HashMap<String, Double>> playersToConsider = new HashMap<String, HashMap<String, Double>>();

        for (String mode : modeList)
            playersToConsider.put(mode, new HashMap<String, Double>());

        for (String pId : players) {

            HashMap<String, Double> vl = new HashMap<>();
            List<String> aux = new ArrayList<String>();

            for (String counter : modeList) {
                Player pla = facade.getPlayerState(gameId, pId);
                Double baseline = getWMABaseline(pla, counter, lastMonday);
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

    private boolean hasGroupChallenge(Player state, String counter) {
        for (ChallengeConcept cha: state.getState().getChallengeConcept()) {
           DateTime start = new DateTime(cha.getStart());

           if (start.getWeekOfWeekyear() != execDate.getWeekOfWeekyear())
               continue;

            String s = cha.getModelName();
            Map<String, Object> fields = cha.getFields();

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

        return false;
    }

    private Map<String, Double> checkEven(HashMap<String, Double> originalList) {

        Map<String, Double> list = sortByValues(originalList);

        if (list.size() % 2 == 0)
            return  list;

        // worst
        String s = list.keySet().iterator().next();
        list.remove(s);

        return list;
    }

    private Double getWMABaseline(Player state, String counter, DateTime lastMonday) {

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

    // Get string array with all player IDs combined (make sure it's even)
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
