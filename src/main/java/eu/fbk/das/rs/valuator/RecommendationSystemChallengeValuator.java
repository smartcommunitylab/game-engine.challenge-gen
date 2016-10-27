package eu.fbk.das.rs.valuator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.math.Quantiles;

import eu.fbk.das.rs.challengeGeneration.RecommendationSystemConfig;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.PointConcept;
import eu.trentorise.game.challenges.rest.PointConcept.PeriodInstanceImpl;
import eu.trentorise.game.challenges.rest.PointConcept.PeriodInternal;

public class RecommendationSystemChallengeValuator {
	// defining a table for points/prize
	private int points[][] = { { 100, 106, 111, 122, 150 }, { 133, 139, 144, 155, 183 }, { 166, 172, 177, 189, 217 },
			{ 197, 205, 211, 222, 250 } };
	private final Integer tryOnceprize = 183;

	public Map<String, List<ChallengeDataDTO>> valuate(Map<String, List<ChallengeDataDTO>> combinations,
			List<Content> input) {

		for (int i = 0; i < RecommendationSystemConfig.defaultMode.length; i++) {

			String mode = RecommendationSystemConfig.defaultMode[i];

			// String mode = RecommendationSystemConfig.defaultMode[i];
			List<Double> activePlayersvalues = new ArrayList<Double>();
			for (Content content : input) {
				for (PointConcept pc : content.getState().getPointConcept()) {
					if (pc.getName().equals(mode)) {
						for (String period : pc.getPeriods().keySet()) {
							PeriodInternal periodInstance = pc.getPeriods().get(period);
							for (PeriodInstanceImpl p : periodInstance.getInstances()) {
								if (p.getScore() > 0) {
									activePlayersvalues.add(p.getScore());
								}

							}

						}

					}

				}

			}

			Collections.sort(activePlayersvalues);
			// ObjectMapper mapper = new ObjectMapper();
			// FileOutputStream oout;
			// try {
			// oout = new FileOutputStream(new
			// File("/Users/rezakhoshkangini/Documents/data.json"));
			// IOUtils.write(mapper.writeValueAsString(activePlayersvalues),
			// oout);
			// oout.flush();
			// } catch (JsonProcessingException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

			// finding the percentiles of mode walk "weekly" from start to
			// now
			Map<Integer, Double> quartiles = Quantiles.scale(10).indexes(4, 7, 9).compute(activePlayersvalues);

			// System.out.println(mode);
			// System.out.println(activePlayersvalues);

			for (String playerId : combinations.keySet()) {
				for (ChallengeDataDTO challenge : combinations.get(playerId)) {
					if (isSameOf((String) challenge.getData().get("counterName"), mode)) {
						if (challenge.getModelName() == "percentageIncrement") {
							Double baseline = (Double) challenge.getData().get("baseline");
							Double target = (Double) challenge.getData().get("target");
							Integer zone = DifficultyCalculator.computeZone(quartiles, baseline);
							Integer difficulty = DifficultyCalculator.computeDifficulty(quartiles, zone, baseline,
									target);
							System.out.println("Challenge baseline=" + baseline + ", target=" + target + ", zone= "
									+ zone + ", difficulty=" + difficulty);
							challenge.getData().put("difficulty", difficulty);

							Integer prize = calculatePrize(difficulty,
									Math.round(Math.abs(baseline - target) * 100.0 / baseline) / 100.0);
							challenge.getData().put("bonusScore", prize);
						} else if (challenge.getModelName() == "absoluteIncrement") {
							challenge.getData().put("difficulty", DifficultyCalculator.MEDIUM);
							challenge.getData().put("bonusScore", tryOnceprize);
						}
					}
				}
			}
		}

		// Add number of trips

		// Add number of trips

		return combinations;

	}

	private boolean isSameOf(String v, String mode) {
		if (v.equals(mode)) {
			return true;
		}
		if (v.contains("_") && mode.contains("_")) {
			String[] s = v.split("_");
			String[] m = mode.split("_");
			return s[0].equals(m[0]);
		}
		return false;
	}

	private Integer calculatePrize(Integer difficulty, double percent) {
		int y = 0;
		if (percent == 0.1) {
			y = 0;
		} else if (percent == 0.2) {
			y = 1;
		} else if (percent == 0.3) {
			y = 2;
		} else if (percent == 0.5) {
			y = 3;
		} else if (percent == 1) {
			y = 4;
		}
		Integer prize = points[difficulty - 1][y];

		return prize;
	}

}
