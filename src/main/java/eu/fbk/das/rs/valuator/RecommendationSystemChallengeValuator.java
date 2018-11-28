package eu.fbk.das.rs.valuator;

import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemStatistics;
import eu.fbk.das.rs.challenges.generation.SingleModeConfig;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static eu.fbk.das.rs.Utils.dbg;
import static eu.fbk.das.rs.Utils.err;

public class RecommendationSystemChallengeValuator {

    private static final Logger logger =
            LogManager.getLogger(RecommendationSystemChallengeValuator.class);

    // Prize Matrix for each mode
    private Map<String, PlanePointFunction> prizeMatrixMap = new HashMap<>();
    private RecommendationSystemConfig cfg;
    private RecommendationSystemStatistics stats;

    /**
     * Create a new recommandation system challenge valuator
     */
    public RecommendationSystemChallengeValuator(RecommendationSystemConfig configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException(
                    "Recommandation system cfg must be not null");
        }
        this.cfg = configuration;
        for (String mode : cfg.defaultMode) {
            SingleModeConfig config = configuration.getModeConfig(mode);

            PlanePointFunction matrix = new PlanePointFunction(
                    RecommendationSystemConfig.PRIZE_MATRIX_NROW,
                    RecommendationSystemConfig.PRIZE_MATRIX_NCOL, config.getPrizeMatrixMin(),
                    config.getPrizeMatrixMax(), config.getPrizeMatrixIntermediate(),
                    RecommendationSystemConfig.PRIZE_MATRIX_APPROXIMATOR);
            prizeMatrixMap.put(mode, matrix);
        }

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
        for (String mode : cfg.getDefaultMode())
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

                int prize = calculatePrize(difficulty, d, counterName);
                challenge.getData().put("bonusScore", prize);
                break;
            case "absoluteIncrement":
                challenge.getData().put("difficulty", DifficultyCalculator.MEDIUM);
                int tryOnceBonus = (int) Math.ceil(prizeMatrixMap.get(counterName).getTryOncePrize(
                        RecommendationSystemConfig.PRIZE_MATRIX_TRY_ONCE_ROW_INDEX,
                        RecommendationSystemConfig.PRIZE_MATRIX_TRY_ONCE_COL_INDEX));
                challenge.getData().put("bonusScore", tryOnceBonus);
                break;
            default:
                err(logger, "Unknown model for the challenge: %s!", challenge.getModelName());
                break;
        }

    }

    private int calculatePrize(Integer difficulty, double percent, String modeName) {
        // TODO: config!
        int y = 0;
        if (percent <= 0.1) {
            y = 0;
        } else if (percent <= 0.2) {
            y = 1;
        } else if (percent <= 0.3) {
            y = 2;
        } else if (percent <= 0.4) {
            y = 4;
        } else if (percent <= 1) {
            y = 9;
        }

        return (int) Math.ceil(prizeMatrixMap.get(modeName).get(difficulty - 1, y));
    }

}
