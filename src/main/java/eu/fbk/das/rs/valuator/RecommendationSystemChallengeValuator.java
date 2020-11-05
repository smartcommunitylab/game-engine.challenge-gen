package eu.fbk.das.rs.valuator;

import static eu.fbk.das.utils.Utils.err;
import static eu.fbk.das.utils.Utils.p;
import static eu.fbk.das.utils.Utils.warn;

import java.util.Map;

import org.apache.log4j.Logger;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.calculator.DifficultyCalculator;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemStatistics;

public class RecommendationSystemChallengeValuator {

    private static final Logger logger =
            Logger.getLogger(RecommendationSystemChallengeValuator.class);

    private final DifficultyCalculator dc;

    private RecommendationSystemStatistics stats;
    private double def_prize = 100.0;

    /**
     * Create a new recommandation system challenge valuator
     */
    public RecommendationSystemChallengeValuator() {
        this.dc = new DifficultyCalculator();
        // dbg(logger, "RecommendationSystemChallengeValuator init complete");
    }

    // Updates a challenge, evaluating its difficulty
    public void valuate(ChallengeExpandedDTO challenge) {

        String counterName = (String) challenge.getData("counterName");
        if (counterName == null) return;

        boolean found = false;
        for (String mode : ChallengesConfig.defaultMode)
            if (counterName.equals(mode)) found = true;
        if (!found) {
            err(logger, "Unknown mode: %s!", counterName);
            return;
        }

        Double v = (Double) challenge.getData("bonusScore");
        if (v != null) return;

        Map<Integer, Double> quantiles = stats.getQuantiles(counterName);

        switch (challenge.getModelName()) {
            case "percentageIncrement":
                Double baseline = (Double) challenge.getData("baseline");
                Double target = (Double) challenge.getData("target");
                computeReward(challenge, counterName, quantiles, baseline, target);
                break;
            case "absoluteIncrement":
                challenge.setData("difficulty", DifficultyCalculator.MEDIUM);
                double tryOnceBonus = dc.getTryOnceBonus(counterName);
                challenge.setData("bonusScore", tryOnceBonus);
                break;
            case "repetitiveBehaviour":
                baseline = (Double) challenge.getData("baseline");
                target = (Double) challenge.getData("target");
                Double periodTarget = (Double) challenge.getData("periodTarget");
                target *= periodTarget * 0.9;
                computeReward(challenge, counterName, quantiles, baseline, target);
                break;
            default:
                challenge.setData("bonusScore", def_prize);
                // err(logger, "Unknown model for the challenge: %s!", challenge.getModelName());
                break;
        }

    }

    private void computeReward(ChallengeExpandedDTO challenge, String counterName, Map<Integer, Double> quantiles, double baseline, double target) {
        double prize = def_prize;
        if (baseline != 0 && target != 0) {

            Integer difficulty = DifficultyCalculator.computeDifficulty(quantiles,
                    baseline, target);
            // + ", target=" + target + " difficulty="
            // + difficulty);
            challenge.setData("difficulty", difficulty);

            double d = (double) challenge.getData("percentage");

            prize = dc.calculatePrize(difficulty, d, counterName);

        }
        challenge.setData("bonusScore", prize);
    }

    public void prepare(RecommendationSystemStatistics stats) {
        this.stats = stats;
    }
}
