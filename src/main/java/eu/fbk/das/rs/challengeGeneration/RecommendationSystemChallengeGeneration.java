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

	//

	public Map<String, List<ChallengeDataDTO>> generate(List<Content> input) {
		Map<String, List<ChallengeDataDTO>> output = new HashMap<String, List<ChallengeDataDTO>>();
		HashMap<String, HashMap<String, Double>> modeValues = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, Double> playerScore = new HashMap<>();
		for (Content content : input) {
			// filter users
			// if
			// (RecommendationSystemConfig.getPlayerIds().contains(content.getPlayerId()))
			// {

			// retrieving the players' last week data "weekly"
			for (int i = 0; i < RecommendationSystemConfig.defaultMode.length; i++) {

				String mode = RecommendationSystemConfig.defaultMode[i];
				if (mode == "Bus_Km") {
					System.out.println(mode);
				}
				for (PointConcept pc : content.getState().getPointConcept()) {
					if (pc.getName().equals(mode)) {
						Double score = pc.getPeriodCurrentScore("weekly");
						playerScore.put(content.getPlayerId(), score);
						System.out.println(mode + ":" + score);
					}

				}
				if (modeValues.get(mode) == null) {
					modeValues.put(mode, new HashMap<String, Double>());
				}
				modeValues.get(mode).putAll(playerScore);

			}

			playerScore.clear();

			// }
		}

		LocalDate now = new LocalDate();

		for (String mode : modeValues.keySet()) {

			System.out.println("mode=" + "" + mode);
			for (String playerId : modeValues.get(mode).keySet()) {
				if (output.get(playerId) == null) {
					output.put(playerId, new ArrayList<ChallengeDataDTO>());
				}
				Double modeCounter = modeValues.get(mode).get(playerId);
				if (modeCounter > 0) {

					// generate different types of challenges by percentage
					for (int i = 0; i < percentageNumber; i++) {
						// calculating the improvement of last week activity
						tmpValueimprovment = percentage[i] * modeCounter;
						if (mode.endsWith("_Trips")) {

							improvementValue = tmpValueimprovment + modeCounter;
							improvementValue = round(improvementValue);
						} else {
							improvementValue = tmpValueimprovment + modeCounter;
						}

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
						data.put("percentage", percentage[i]);
						cdd.setData(data);
						output.get(playerId).add(cdd);
					}
				} else {
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
					data.put("counterName", getDefaultMode(mode));
					data.put("periodName", "weekly");
					cdd.setData(data);
					output.get(playerId).add(cdd);
				}
			}
		}
		return output;
	}

	// round the improvement trip number
	private double round(double improvementValue2) {
		// TODO Auto-generated method stub
		return Math.round(improvementValue2);
	}

	private String getDefaultMode(String mode) {
		if (!mode.endsWith("_trips")) {
			return mode;
		}
		for (int i = 0; i < RecommendationSystemConfig.defaultMode.length; i++) {
			String m = RecommendationSystemConfig.defaultMode[i];
			if (m == mode) {
				return RecommendationSystemConfig.defaultModetrip[i];
			}
		}
		return null;
	}

}
