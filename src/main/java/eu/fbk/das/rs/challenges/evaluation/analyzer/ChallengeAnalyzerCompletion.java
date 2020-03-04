package eu.fbk.das.rs.challenges.evaluation.analyzer;

import eu.fbk.das.rs.challenges.evaluation.ChallengeAnalyzer;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.trentorise.game.challenges.rest.ChallengeConcept;
import eu.trentorise.game.challenges.rest.Player;
import org.joda.time.DateTime;

import java.util.*;

import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.getWeeklyContentMode;
import static eu.fbk.das.rs.utils.Utils.p;
import static eu.fbk.das.rs.utils.Utils.pf;

public class ChallengeAnalyzerCompletion extends ChallengeAnalyzer {

    private Map<Integer, int[]> cache;

    public ChallengeAnalyzerCompletion(RecommendationSystemConfig cfg) {
        super(cfg);
    }

    public static void main(String[] args) {
        ChallengeAnalyzerCompletion cdg = new ChallengeAnalyzerCompletion(new RecommendationSystemConfig());

        cdg.execute();
    }

    protected void prepare() {
        super.prepare();

        playerLimit = 0;

        cache = new TreeMap<>();

    }

    private void execute() {

        prepare();
        createFacade();

        int[] l;

        List<String> pl = getPlayers();
        for (String pId: pl) {
            Player player = facade.getPlayerState(cfg.get("GAME_ID"), pId);
            for (ChallengeConcept cha: player.getState().getChallengeConcept()) {

                if (!cha.getModelName().equals("percentageIncrement") && !cha.getModelName().equals("absoluteIncrement"))
                    continue;

                if (cha.getName().contains("initial") ||cha.getName().contains("survey")  ||cha.getName().contains("recommendation"))
                    continue;

                Integer w = getChallengeWeek(new DateTime(cha.getStart()));

                // does not consider if he was not active during that week
                if (getWeeklyContentMode(player, "green leaves", new DateTime(cha.getStart())) <= 0)
                    continue;


                if (!cache.containsKey(w)) {
                    l = new int[2];
                    cache.put(w, l);
                }

                l = cache.get(w);
                l[0] ++;
                if (cha.isCompleted())
                    l[1]++;

            }
        }

        for (Integer week: cache.keySet()) {
            l = cache.get(week);
            pf ("%d,%.2f, (%d / %d) \n", week, l[1] * 1.0 / l[0], l[1], l[0]);
        }
    }


}
