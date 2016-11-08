package eu.fbk.das.rs.challengeGeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.LocalDate;

import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.PointConcept;

/**
 * RecommendationSystem challenge generation module: generate all possible
 * challenges using provided {@link RecommendationSystemConfig}
 */
public class RecommendationSystemChallengeGeneration {

	private static final Logger logger = LogManager
			.getLogger(RecommendationSystem.class);

	// define a variable that the player should improve its mode
	private double improvementValue = 0;
	// define a temporary variable to save the improvement
	private double tmpValueimprovment = 0;
	// configuration
	private RecommendationSystemConfig configuration;

	/**
	 * Create a new recommandation system challenge generator
	 * 
	 * @param configuration
	 * @throws IllegalArgumentException
	 *             if configuration is null
	 */
	public RecommendationSystemChallengeGeneration(
			RecommendationSystemConfig configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException(
					"Recommandation system configuration must be not null");
		}
		this.configuration = configuration;
		logger.debug("RecommendationSystemChallengeGeneration init complete");
	}

	public Map<String, List<ChallengeDataDTO>> generate(List<Content> input) {
		Map<String, List<ChallengeDataDTO>> output = new HashMap<String, List<ChallengeDataDTO>>();
		HashMap<String, HashMap<String, Double>> modeValues = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, Double> playerScore = new HashMap<>();
		for (Content content : input) {
			// filter users
			if (configuration.isUserfiltering()) {
				if (configuration.getPlayerIds()
						.contains(content.getPlayerId())) {
					modeValues = buildModeValues(modeValues, playerScore,
							content);
				}
			} else {
				modeValues = buildModeValues(modeValues, playerScore, content);
			}
			playerScore.clear();
		}

		LocalDate now = new LocalDate();

		int challengeNum = 0;
		for (String mode : modeValues.keySet()) {
			for (String playerId : modeValues.get(mode).keySet()) {
				if (output.get(playerId) == null) {
					output.put(playerId, new ArrayList<ChallengeDataDTO>());
				}
				Double modeCounter = modeValues.get(mode).get(playerId);
				if (modeCounter > 0) {

					// generate different types of challenges by percentage
					for (int i = 0; i < configuration.getPercentage().length; i++) {
						// calculating the improvement of last week activity
						tmpValueimprovment = configuration.getPercentage()[i]
								* modeCounter;
						if (mode.endsWith("_Trips")) {
							improvementValue = tmpValueimprovment + modeCounter;
							improvementValue = Math.round(improvementValue);
						} else {
							improvementValue = tmpValueimprovment + modeCounter;
						}
						challengeNum++;
						ChallengeDataDTO cdd = new ChallengeDataDTO();
						cdd.setModelName("percentageIncrement");
						cdd.setInstanceName(configuration
								.getChallengeNamePrefix()
								+ mode
								+ "_"
								+ configuration.getPercentage()[i]
								+ "_"
								+ UUID.randomUUID());
						cdd.setStart(now.dayOfMonth().addToCopy(1).toDate());
						cdd.setEnd(now.dayOfMonth().addToCopy(7).toDate());
						Map<String, Object> data = new HashMap<String, Object>();
						data.put("target", improvementValue);
						data.put("bonusPointType", "green leaves");
						data.put("bonusScore", 100d);
						data.put("baseline", modeCounter);
						data.put("counterName", mode);
						data.put("periodName", "weekly");
						data.put("percentage", configuration.getPercentage()[i]);
						cdd.setData(data);
						output.get(playerId).add(cdd);
					}
				} else {
					if (configuration.isDefaultMode(mode)) {
						challengeNum++;
						// build a try once
						ChallengeDataDTO cdd = new ChallengeDataDTO();
						cdd.setModelName("absoluteIncrement");
						cdd.setInstanceName(configuration
								.getChallengeNamePrefix()
								+ mode
								+ "_try_"
								+ UUID.randomUUID());
						cdd.setStart(now.dayOfMonth().addToCopy(1).toDate());
						cdd.setEnd(now.dayOfMonth().addToCopy(7).toDate());
						Map<String, Object> data = new HashMap<String, Object>();
						data.put("target", 1);
						data.put("bonusPointType", "green leaves");
						data.put("bonusScore", 100d);
						data.put("counterName", mode);
						data.put("periodName", "weekly");
						cdd.setData(data);
						output.get(playerId).add(cdd);
					}
				}
			}
		}
		logger.debug("generated challenges : " + challengeNum);
		return output;
	}

	private HashMap<String, HashMap<String, Double>> buildModeValues(
			HashMap<String, HashMap<String, Double>> modeValues,
			HashMap<String, Double> playerScore, Content content) {
		// retrieving the players' last week data "weekly"
		for (int i = 0; i < configuration.getDefaultMode().length; i++) {

			String mode = configuration.getDefaultMode()[i];
			for (PointConcept pc : content.getState().getPointConcept()) {
				if (pc.getName().equals(mode)) {
					Double score = pc.getPeriodCurrentScore("weekly");
					playerScore.put(content.getPlayerId(), score);
				}
			}
			if (modeValues.get(mode) == null) {
				modeValues.put(mode, new HashMap<String, Double>());
			}
			modeValues.get(mode).putAll(playerScore);
		}
		return modeValues;
	}

}
