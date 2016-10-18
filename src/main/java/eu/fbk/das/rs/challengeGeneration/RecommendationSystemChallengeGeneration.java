package eu.fbk.das.rs.challengeGeneration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joda.time.LocalDate;

import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.PointConcept;

public class RecommendationSystemChallengeGeneration {

	// defining different improvement percentage 10%,20%, etc.
	private Double[] percentage = { 0.1, 0.2, 0.3, 0.5, 1.0 };
	// the number of percentages
	private int percentageNumber = 5;
	// define a variable that the player should improve its mode
	private double improvementValue = 0;
	// define a temporary variable to save the improvement
	private double tmpValueimprovment = 0;

	public Map<String, List<ChallengeDataDTO>> generate(List<Content> input) {
		Map<String, List<ChallengeDataDTO>> output = new HashMap<String, List<ChallengeDataDTO>>();
		// we need to put all the configuration in different modes
		String defaultMode = "Walk_Km";
		String defaultModetrip = "Walk_Trips";

		HashMap<String, HashMap<String, Double>> modeValues = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, Double> playerScore = new HashMap<>();
		for (Content content : input) {

			for (PointConcept pc : content.getState().getPointConcept()) {
				if (pc.getName().equals(defaultMode)) {
					Double score = pc.getPeriodPreviousScore("weekly");
					playerScore.put(content.getPlayerId(), score);

				}
			}
			modeValues.put(defaultMode, playerScore);

		}

		LocalDate now = new LocalDate();

		for (String mode : modeValues.keySet()) {
			for (String playerId : modeValues.get(mode).keySet()) {
				List<ChallengeDataDTO> challenges = new ArrayList<ChallengeDataDTO>();
				Double modeCounter = modeValues.get(mode).get(playerId);
				if (modeCounter > 0) {

					// generate different types of challenges by percentage
					for (int i = 0; i < percentageNumber; i++) {
						// calculating the improvement of last week activity
						tmpValueimprovment = percentage[i] * modeCounter;
						// Adding to the last week activity as the improvement
						// activity for next week
						improvementValue = tmpValueimprovment + modeCounter;

						ChallengeDataDTO cdd = new ChallengeDataDTO();
						cdd.setModelName("percentageIncrement");
						cdd.setInstanceName("InstanceName" + UUID.randomUUID());
						cdd.setStart(now.dayOfMonth().addToCopy(1).toDate());
						cdd.setEnd(now.dayOfMonth().addToCopy(7).toDate());
						Map<String, Object> data = new HashMap<String, Object>();
						data.put("target", improvementValue);
						data.put("bonusPointType", "green leaves");
						data.put("bonusScore", 100d);
						data.put("baseline", modeCounter);
						data.put("counterName", mode);
						data.put("periodName", "weekly");
						cdd.setData(data);
						challenges.add(cdd);
					}
				} else {
					if (mode == defaultMode) {

						// build a try once
						ChallengeDataDTO cdd = new ChallengeDataDTO();
						cdd.setModelName("absoluteIncrement");
						cdd.setInstanceName("InstanceName" + UUID.randomUUID());
						cdd.setStart(now.dayOfMonth().addToCopy(1).toDate());
						cdd.setEnd(now.dayOfMonth().addToCopy(7).toDate());
						Map<String, Object> data = new HashMap<String, Object>();
						data.put("target", 1);
						data.put("bonusPointType", "green leaves");
						data.put("bonusScore", 100d);
						data.put("counterName", defaultModetrip);
						data.put("periodName", "weekly");
						cdd.setData(data);
						challenges.add(cdd);
					}

				}
				output.put(playerId, challenges);
			}
		}
		return output;
	}

}
