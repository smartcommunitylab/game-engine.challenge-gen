package eu.fbk.das.rs.valuator;

import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemStatistics;
import eu.fbk.das.rs.challenges.calculator.DifficultyCalculator;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static eu.fbk.das.rs.Utils.dbg;
import static eu.fbk.das.rs.Utils.err;

public class RecommendationSystemChallengeValuator {

    private static final Logger logger =
            LogManager.getLogger(RecommendationSystemChallengeValuator.class);

    private final DifficultyCalculator dc;

    private RecommendationSystemStatistics stats;

    /**
     * Create a new recommandation system challenge valuator
     */
    public RecommendationSystemChallengeValuator(RecommendationSystemConfig configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException(
                    "Recommandation system cfg must be not null");
        }

        this.dc = new DifficultyCalculator();

        dbg(logger, "RecommendationSystemChallengeValuator init complete");
    }

    // Prepare quantiles given statistic
    public void prepare(RecommendationSystemStatistics stats) {
        this.stats = stats;
    }

    // Updates a challenge, evaluating its difficulty
    public void valuate(ChallengeDataDTO challenge) {

        String counterName = (String) challenge.getData().get("counterName");

        boolean found = false;
        for (String mode : ChallengesConfig.defaultMode)
            if (counterName.equals(mode)) found = true;
        if (!found) {
            err(logger, "Unknown mode: %s!", counterName);
            return;
        }

        Map<Integer, Double> quantiles = stats.getQuantiles(counterName);

        switch (challenge.getModelName()) {
            case "percentageIncrement":
                Double baseline = (Double) challenge.getData().get("baseline");
                Double target = (Double) challenge.getData().get("target");
                Integer difficulty = DifficultyCalculator.computeDifficulty(quantiles,
                        baseline, target);
                // + ", target=" + target + " difficulty="
                // + difficulty);
                challenge.getData().put("difficulty", difficulty);

                double d = (double) challenge.getData().get("percentage");

                int prize = dc.calculatePrize(difficulty, d, counterName);
                challenge.getData().put("bonusScore", prize);
                break;
            case "absoluteIncrement":
                challenge.getData().put("difficulty", DifficultyCalculator.MEDIUM);
                int tryOnceBonus = (int) dc.getTryOnceBonus(counterName);
                challenge.getData().put("bonusScore", tryOnceBonus);
                break;
            default:
                err(logger, "Unknown model for the challenge: %s!", challenge.getModelName());
                break;
        }

    }

}
