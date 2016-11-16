package eu.fbk.das.rs.valuator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.math.Quantiles;

import eu.fbk.das.rs.challengeGeneration.RecommendationSystemConfig;
import eu.fbk.das.rs.challengeGeneration.SingleModeConfig;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.PointConcept;
import eu.trentorise.game.challenges.rest.PointConcept.PeriodInstanceImpl;
import eu.trentorise.game.challenges.rest.PointConcept.PeriodInternal;

public class RecommendationSystemChallengeValuator {

	private static final Logger logger = LogManager
			.getLogger(RecommendationSystemChallengeValuator.class);

	/**
	 * Prize Matrix for each mode
	 */
	private Map<String, PlanePointFunction> prizeMatrixMap = new HashMap<String, PlanePointFunction>();
	private RecommendationSystemConfig configuration;

	/**
	 * Create a new recommandation system challenge valuator
	 * 
	 * @param configuration
	 * @throws IllegalArgumentException
	 *             if configuration is null
	 */
	public RecommendationSystemChallengeValuator(
			RecommendationSystemConfig configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException(
					"Recommandation system configuration must be not null");
		}
		this.configuration = configuration;
		for (String mode : configuration.getModeKeySet()) {
			SingleModeConfig config = configuration.getModeConfig(mode);
			PlanePointFunction matrix = new PlanePointFunction(
					RecommendationSystemConfig.PRIZE_MATRIX_NROW,
					RecommendationSystemConfig.PRIZE_MATRIX_NCOL,
					config.getPrizeMatrixMin(), config.getPrizeMatrixMax(),
					config.getPrizeMatrixIntermediate(),
					RecommendationSystemConfig.PRIZE_MATRIX_APPROXIMATOR);
			prizeMatrixMap.put(mode, matrix);
		}
		logger.debug("RecommendationSystemChallengeValuator init complete");
	}

	public Map<String, List<ChallengeDataDTO>> valuate(
			Map<String, List<ChallengeDataDTO>> combinations,
			List<Content> input) {

		for (int i = 0; i < configuration.getDefaultMode().length; i++) {
			String mode = configuration.getDefaultMode()[i];
			List<Double> activePlayersvalues = new ArrayList<Double>();
			for (Content content : input) {
				for (PointConcept pc : content.getState().getPointConcept()) {
					// Adding filter for users
					// if
					// (RecommendationSystemConfig.getPlayerIds().contains(content.getPlayerId()))
					// {
					if (pc.getName().equals(mode)) {
						for (String period : pc.getPeriods().keySet()) {
							PeriodInternal periodInstance = pc.getPeriods()
									.get(period);
							for (PeriodInstanceImpl p : periodInstance
									.getInstances()) {
								if (p.getScore() > 0) {
									activePlayersvalues.add(p.getScore());
								}
							}
						}
					}
				}
			}
			Collections.sort(activePlayersvalues);

			// finding the percentiles of mode walk "weekly" from start to
			// now
			Map<Integer, Double> quartiles = Quantiles.scale(10)
					.indexes(4, 7, 9).compute(activePlayersvalues);

			for (String playerId : combinations.keySet()) {

				List<ChallengeDataDTO> challenges = combinations.get(playerId);

				for (ChallengeDataDTO challenge : challenges) {
					String counterName = (String) challenge.getData().get(
							"counterName");
					if (counterName.equals(mode)) {
						if (challenge.getModelName() == "percentageIncrement") {
							Double baseline = (Double) challenge.getData().get(
									"baseline");
							Double target = (Double) challenge.getData().get(
									"target");
							Integer difficulty = DifficultyCalculator
									.computeDifficulty(quartiles, baseline,
											target);
							// + ", target=" + target + " difficulty="
							// + difficulty);
							challenge.getData().put("difficulty", difficulty);

							double d = (double) challenge.getData().get(
									"percentage");

							Double prize = calculatePrize(difficulty, d,
									counterName);
							challenge.getData().put("bonusScore", prize);
						} else if (challenge.getModelName() == "absoluteIncrement") {
							challenge.getData().put("difficulty",
									DifficultyCalculator.MEDIUM);
							Double tryOnceBonus = prizeMatrixMap
									.get(counterName)
									.getTryOncePrize(
											RecommendationSystemConfig.PRIZE_MATRIX_TRY_ONCE_ROW_INDEX,
											RecommendationSystemConfig.PRIZE_MATRIX_TRY_ONCE_COL_INDEX);
							challenge.getData().put("bonusScore", tryOnceBonus);
						}
					}
				}
				combinations.put(playerId, challenges);
			}
		}
		logger.info("Challenge valuation completed ");
		return combinations;

	}

	private Double calculatePrize(Integer difficulty, double percent,
			String modeName) {
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
