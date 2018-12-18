package eu.fbk.das.rs;

import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.calculator.DifficultyCalculator;

import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.utils.Pair;
import eu.trentorise.game.challenges.rest.Player;
import eu.trentorise.game.challenges.rest.GameStatisticsSet;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.PointConcept;
import eu.trentorise.game.model.GameStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

import static eu.fbk.das.rs.challenges.generation.RecommendationSystemChallengeGeneration.roundTarget;
import static eu.fbk.das.rs.utils.Utils.*;


public class TargetPrizeChallengesCalculator {

    private GamificationEngineRestFacade facade;

    private DateTime execDate;
    private DateTime lastMonday;

    private GameStatistics gs;

    private DifficultyCalculator dc;

    private String gameId;
    private RecommendationSystem rs;


    public void prepare(String host, String username, String password, String gameId) {

        execDate = new DateTime();

        facade = new GamificationEngineRestFacade(host, username, password);

        this.gameId = gameId;
    }

    // TODO remove
    public void prepare(RecommendationSystem rs, String gameId) {

        execDate = new DateTime();

        facade = rs.facade;

        this.rs = rs;
        this.gameId = gameId;
    }


    public Map<String, Double> targetPrizeChallengesCompute(String pId_1, String pId_2, String counter, String type) {

        prepare();

        Map<Integer, Double> quantiles = getQuantiles2(gameId, counter);

        Map<String, Double> res = new HashMap<>();

        Player player1 = facade.getPlayerState(gameId, pId_1);
        Pair<Double, Double> res1 = forecast(res, "player1", player1, counter);
        double player1_tgt = res1.getFirst();
        double player1_bas = res1.getSecond();

        Player player2 = facade.getPlayerState(gameId, pId_2);
        Pair<Double, Double> res2 = forecast(res, "player2", player2, counter);
        double player2_tgt = res2.getFirst();
        double player2_bas = res2.getSecond();

        double target;
        if (type.equals("groupCompetitiveTime")) {
            target = roundTarget(counter,(player1_tgt + player2_tgt) / 2.0);

            target = checkMaxTargetCompetitive(counter, target);

            res.put("target", target);
                    res.put("player1_prz", evaluate(target, player1_bas, counter, quantiles));
            res.put("player2_prz",  evaluate(target, player2_bas, counter, quantiles));
        }
        else if (type.equals("groupCooperative")) {
            target =roundTarget(counter, player1_tgt + player2_tgt);

            target = checkMaxTargetCooperative(counter, target);

            double player1_prz = evaluate(player1_tgt, player1_bas, counter, quantiles);
            double player2_prz = evaluate(player2_tgt, player2_bas, counter, quantiles);
            double prz = Math.max(player1_prz, player2_prz);

            res.put("target", target);
            res.put("player1_prz", prz);
            res.put("player2_prz", prz);
        }  else if (type.equals("groupCompetitivePerformance")) {
            p("WRONG TYPE");
        } else
            p("UNKOWN TYPE");

        return res;
    }

    private double checkMaxTargetCompetitive(String counter, double v) {
            if ("Walk_Km".equals(counter))
                return Math.min(70, v);
            if ("Bike_Km".equals(counter))
                return Math.min(210, v);
            if ("green leaves".equals(counter))
                return Math.max(3000, v);

            p("WRONG COUNTER");
            return 0.0;
        }

    private double checkMaxTargetCooperative(String counter, double v) {
        if ("Walk_Km".equals(counter))
            return Math.min(140, v);
        if ("Bike_Km".equals(counter))
            return Math.min(420, v);
        if ("green leaves".equals(counter))
            return Math.max(6000, v);

        p("WRONG COUNTER");
        return 0.0;
    }

    private Double checkMinTarget(String counter, Double v) {
        if ("Walk_Km".equals(counter))
            return Math.max(1, v);
        if ("Bike_Km".equals(counter))
            return Math.max(5, v);
        if ("green leaves".equals(counter))
            return Math.max(50, v);

        p("WRONG COUNTER");
        return 0.0;
    }

    private Map<Integer, Double> getQuantiles2(String gameId, String counter) {
        return rs.getStats().getQuantiles(counter);
    }

    private Map<Integer, Double> getQuantiles(String gameId, String counter) {

        // Da sistemare richiesta per dati della settimana precedente, al momento non presenti
        GameStatisticsSet stats = facade.readGameStatistics(gameId, counter);
        if (stats == null || stats.isEmpty()) {
            pf("Nope \n");
            return null;
        }

        gs = stats.iterator().next();
        return gs.getQuantiles();
    }

    private void prepare() {

            // Set next monday as start, and next sunday as end
            int week_day = execDate.getDayOfWeek();
            int d = (7 - week_day) + 1;

            lastMonday = execDate.minusDays(week_day-1).minusDays(7);

        dc = new DifficultyCalculator();
    }

    private Pair<Double, Double> forecast(Map<String, Double> res, String nm, Player state, String counter) {

        // Last 3 values?
        int v = 3;
        double[][] d = new double[v][];

        DateTime date = lastMonday;

        double wma = 0;
        int wma_d = 0;

        for (int i = 0 ; i < v; i++) {
            int ix = v - (i+1);
            d[ix] = new double[2];
            Double c = getWeeklyContentMode(state, counter, date);
            d[ix][1] = c;
            d[ix][0] = ix + 1;
            date = date.minusDays(7);
            res.put(f("%s_base_%d", nm, ix), c);

            wma += (v-i) * c;
            wma_d += (v-i);
        }

        wma /= wma_d;

        SimpleRegression simpleRegression = new SimpleRegression(true);
        simpleRegression.addData(d);

        double slope = simpleRegression.getSlope();
        double intercept =  simpleRegression.getIntercept();
        double pv;
        if (slope < 0)
            pv = wma * 1.1;
        else
            pv = intercept + slope * (v+1) * 0.9;

        pv = checkMinTarget(counter, pv);

        res.put(f("%s_tgt", nm), pv);

        return new Pair<Double, Double>(pv, wma);
    }

    public Double getWeeklyContentMode(Player cnt, String mode, DateTime execDate) {


        for (PointConcept pc : cnt.getState().getPointConcept()) {

            String m = pc.getName();
            if (!m.equals(mode))
                continue;

            return pc.getPeriodScore("weekly", execDate);
        }

        return 0.0;
    }

    /*
    private GameStatistics getGameStatistics(Set<GameStatistics> stats, String mode) {
        for (GameStatistics gs: stats) {
            if (gs.getPointConceptName().equals(mode))
                return gs;
        }

        pf("ERROR COUNTER '%s' NOT FOUND", mode);
        return null;
    } */



        public Double evaluate(Double target, Double baseline, String counter, Map<Integer, Double> quantiles) {

            if (baseline == 0)
                return 100.0;

            Integer difficulty = DifficultyCalculator.computeDifficulty(quantiles,
                    baseline, target);

            double d = (target  / Math.max(1, baseline)) - 1;

            int prize = dc.calculatePrize(difficulty, d, counter);

            return Math.ceil(prize * ChallengesConfig.competitiveChallengesBooster / 10.0) * 10;
        }


}
