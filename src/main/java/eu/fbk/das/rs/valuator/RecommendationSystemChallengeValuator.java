package eu.fbk.das.rs.valuator;

import eu.fbk.das.rs.challengeGeneration.RecommendationSystemConfig;
import eu.fbk.das.rs.challengeGeneration.RecommendationSystemStatistics;
import eu.fbk.das.rs.challengeGeneration.SingleModeConfig;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static eu.fbk.das.rs.ArrayUtils.find;
import static eu.fbk.das.rs.Utils.dbg;
import static eu.fbk.das.rs.Utils.err;

public class RecommendationSystemChallengeValuator {

    private static final Logger logger =
            LogManager.getLogger(RecommendationSystemChallengeValuator.class);

    // Prize Matrix for each mode
    private Map<String, PlanePointFunction> prizeMatrixMap = new HashMap<String, PlanePointFunction>();
    private RecommendationSystemConfig cfg;
    private RecommendationSystemStatistics stats;

    /**
     * Create a new recommandation system challenge valuator
     *
     * @param configuration
     * @throws IllegalArgumentException if cfg is null
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
        if (!find(counterName, cfg.getDefaultMode())) {
            err(logger, "Unknown mode: %s!", counterName);
            return;
        }

        Map<Integer, Double> quartiles = stats.getQuartiles(counterName);

        if (challenge.getModelName().equals("percentageIncrement")) {
            Double baseline = (Double) challenge.getData().get("baseline");
            Double target = (Double) challenge.getData().get("target");
            Integer difficulty = DifficultyCalculator.computeDifficulty(quartiles,
                    baseline, target);
            // + ", target=" + target + " difficulty="
            // + difficulty);
            challenge.getData().put("difficulty", difficulty);

            double d = (double) challenge.getData().get("percentage");

            Double prize = calculatePrize(difficulty, d, counterName);
            challenge.getData().put("bonusScore", prize);
        } else if (challenge.getModelName().equals("absoluteIncrement")) {
            challenge.getData().put("difficulty", DifficultyCalculator.MEDIUM);
            Double tryOnceBonus = prizeMatrixMap.get(counterName).getTryOncePrize(
                    RecommendationSystemConfig.PRIZE_MATRIX_TRY_ONCE_ROW_INDEX,
                    RecommendationSystemConfig.PRIZE_MATRIX_TRY_ONCE_COL_INDEX);
            challenge.getData().put("bonusScore", tryOnceBonus);
        } else {
            err(logger, "Unknown model for the challenge: %s!", challenge.getModelName());
        }

    }

    private Double calculatePrize(Integer difficulty, double percent, String modeName) {
        // TODO: config!
        int y = 0;
        if (percent == 0.1) {
            y = 0;
        } else if (percent == 0.2) {
            y = 1;
        } else if (percent == 0.3) {
            y = 2;
        } else if (percent == 0.5) {
            y = 4;
        } else if (percent == 1) {
            y = 9;
        }

        Double prize = prizeMatrixMap.get(modeName).get(difficulty - 1, y);

        return prize;
    }

}
