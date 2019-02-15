package eu.fbk.das.rs;

import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemStatistics;
import eu.fbk.das.rs.utils.ArrayUtils;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.Player;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.joda.time.DateTime;

import java.util.*;

import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.getWeeklyContentMode;
import static eu.fbk.das.rs.utils.Utils.*;

public class GroupChallengesAssigner {

    private final GamificationEngineRestFacade facade;
    private final RecommendationSystemConfig cfg;
    private final String gameId;
    private final DateTime execDate;
    private RecommendationSystem rs;
    private DateTime lastMonday;

    public GroupChallengesAssigner(RecommendationSystemConfig cfg, GamificationEngineRestFacade facade) {
        this.facade = facade;
        this.cfg = cfg;
        gameId = cfg.get("GAME_ID");
        rs = new RecommendationSystem();
        execDate = new DateTime();
    }

    private void prepare() {

        // Set next monday as start, and next sunday as end
        int week_day = execDate.getDayOfWeek();
        int d = (7 - week_day) + 1;

        lastMonday = execDate.minusDays(week_day-1).minusDays(7);
    }

    public void execute() {

        prepare();

        List<String> players = getPlayers();

        RecommendationSystemStatistics stats = rs.getStats();
        stats.checkAndUpdateStats(facade, execDate, cfg, cfg.get("HOST"));

        HashMap<String, HashMap<Integer, ArrayList<String>>> playerQuantile = new HashMap<String, HashMap<Integer, ArrayList<String>>>();

        Set<String> playersToConsider = new HashSet<>();

        for (String counter: new String[] {ChallengesConfig.WALK_KM, ChallengesConfig.BIKE_KM, ChallengesConfig.GREEN_LEAVES}) {
            HashMap<Integer, ArrayList<String>> counterQuantiles = new HashMap<Integer, ArrayList<String>>();
            Map<Integer, Double> quant = stats.getQuantiles(counter);

            for (String pId: players) {
                Double c = getWeeklyContentMode(facade.getPlayerState(gameId, pId), counter, lastMonday);
                int q = getQuantile(c, quant);
                if (q == -1) {
                    p("this is reeeeeaaaally strange");
                    continue;
                }

                if (q < 2)
                    continue;

                if (!counterQuantiles.containsKey(q))
                    counterQuantiles.put(q, new ArrayList<String>());
                counterQuantiles.get(q).add(pId);

                playersToConsider.add(pId);

            }

            playerQuantile.put(counter, counterQuantiles);
        }

        String counter = "Walk_Km";
        chocoModel(playerQuantile.get(counter));
    }



    private int getQuantile(Double c, Map<Integer, Double> quant) {
        for (int i = 0; i < 10; i++) {
            if (c < quant.get(i))
                return i;
        }

        return 9;
    }

    private List<String> getPlayers() {

        int limit = 40;

        Set<String> players = facade.getGamePlayers(gameId);
        List<String> playersToConsider = new ArrayList<String>(limit);
        int ix = 0;
        for (String pId: players) {
            Player p = facade.getPlayerState(gameId, pId);
            int lvl = rs.getLevel(p);
            if (lvl >= 3) {
                playersToConsider.add(pId);
                if (ix++ > limit)
                    break;
            }
        }
        return playersToConsider;
    }

    public void chocoModel(HashMap<Integer, ArrayList<String>> playerQuantile) {

        String[] players = getActualPlayers(playerQuantile);

        int N = players.length;
        Model model = new Model("group assignment");

        int[][] cost = getMatrixCost(players, playerQuantile);

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

        Solver solver = model.getSolver();
        // Find a solution that minimizes 'tot_cost'
        solver.limitTime("30s");
        Solution best = solver.findOptimalSolution(tot_cost, false);
        solver.printStatistics();
        for(int i  = 0; i < N; i++) {
            for(int j  =  i +1; j < N; j++) {
                if (best.getIntVal(vs[i][j]) > 0)
                    pf("Pair %s - %s: cost %d \n", players[i], players[j], cost[i][j]);
            }
        }

        p("completed");
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

    private int[][] getMatrixCost(String[] players, HashMap<Integer, ArrayList<String>> playerQuantile) {
        int N = players.length;

        ArrayList<String[]> rankPosition = new ArrayList<String[]>(10);

        for (int i = 0; i < 10; i++) {
            ArrayList<String> list = playerQuantile.get(i);
            if (list != null) {
                Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
                rankPosition.add(list.toArray(new String[list.size()]));
            } else
                rankPosition.add(new String[0]);

            pf("%d: %s\n", i, String.join(", ", rankPosition.get(i)));
        }

        int[][] cost = new int[N][];
        for (int i = 0; i < N; i++) {
            cost[i] = new int[N];
            for (int j = 0; j < N; j++) {
                int dist = Math.abs(position(players[i], rankPosition) - position(players[j], rankPosition));
                cost[i][j] = (int) Math.pow(2, dist);
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
