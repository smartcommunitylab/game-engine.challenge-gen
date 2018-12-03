package eu.fbk.das.rs;

import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.calculator.DifficultyCalculator;

import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GameStatisticsSet;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.PointConcept;
import eu.trentorise.game.model.GameStatistics;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

import static eu.fbk.das.rs.Utils.pf;


public class TargetPrizeChallengesCalculator {

    private GamificationEngineRestFacade facade;

    private DateTime execDate;
    private DateTime lastMonday;

    private GameStatistics gs;

    private DifficultyCalculator dc;

    private String gameId;

    public static void test() {

        TargetPrizeChallengesCalculator tpcc = new TargetPrizeChallengesCalculator();
        tpcc.prepare("https://dev.smartcommunitylab.it/gamification-copia/", "long-rovereto", "long_RoVg@me",  "5b7a885149c95d50c5f9d442");

        String pId_1 = "200";
        String pId_2 = "236";
        tpcc.targetPrizeChallengesCompute(pId_1, pId_2, "Walk_Km", "CompetitivaTempo");
    }


    public void prepare(String host, String username, String password, String gameId) {

        execDate = new DateTime();

        facade = new GamificationEngineRestFacade(host, username, password);

        this.gameId = gameId;
    }


    public Map<String, Double> targetPrizeChallengesCompute(String pId_1, String pId_2, String counter, String type) {

        prepare();

        // Da sistemare richiesta per dati della settimana precedente, al momento non presenti
        GameStatisticsSet stats = facade.readGameStatistics(gameId, counter);
        if (stats == null || stats.isEmpty()) {
            pf("Nope \n");
            return null;
        }

        gs = stats.iterator().next();

        Content player1 = facade.getPlayerState(gameId, pId_1);
        Double player1_tgt = forecast(player1, counter);

        Content player2 = facade.getPlayerState(gameId, pId_2);
        Double player2_tgt = forecast(player1, counter);

        Map<String, Double> res = new HashMap<>();

        double target;
        if (type.equals("CompetitivaTempo")) {
            target = (player1_tgt + player2_tgt) / 2.0;

            res.put("target", target);
                    res.put("player1_prz", evaluate(target, player1, counter));
            res.put("player2_prz",  evaluate(target, player2, counter));
        }
        else {
            target = player1_tgt + player2_tgt;

            res.put("target", target);
            res.put("player1_prz", evaluate(player1_tgt, player1, counter));
            res.put("player2_prz", evaluate(player2_tgt, player2, counter));
        }

        return res;
    }

    private void prepare() {

            // Set next monday as start, and next sunday as end
            int week_day = execDate.getDayOfWeek();
            int d = (7 - week_day) + 1;

            lastMonday = execDate.minusDays(week_day-1).minusDays(7);

        dc = new DifficultyCalculator();
    }

    private Double forecast(Content state, String counter) {

            Double lastWeek = getWeeklyContentMode(state, counter, lastMonday);
            Double previousWeek = getWeeklyContentMode(state, counter, lastMonday.minusDays(7));

        double slope = (previousWeek - lastWeek) / previousWeek;
        slope = Math.abs(slope) * 0.8;
        if (slope > 0.3)
            slope = 0.3;

        return (lastWeek * (1 + slope));
    }

    public Double getWeeklyContentMode(Content cnt, String mode, DateTime execDate) {


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



        public Double evaluate(Double target, Content player, String counter) {

            Double baseline = getWeeklyContentMode(player, counter, lastMonday);

            Integer difficulty = DifficultyCalculator.computeDifficulty(gs.getQuantiles(),
                    baseline, target);

            double d = target * 1.0 / baseline;

            int prize = dc.calculatePrize(difficulty, d, counter);

            return Math.ceil(prize * ChallengesConfig.competitiveChallengesBooster);
        }
    
}
