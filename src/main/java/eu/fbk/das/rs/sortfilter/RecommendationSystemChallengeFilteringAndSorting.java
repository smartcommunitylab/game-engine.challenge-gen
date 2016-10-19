package eu.fbk.das.rs.sortfilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.trentorise.game.challenges.model.ChallengeDataDTO;

public class RecommendationSystemChallengeFilteringAndSorting {

	private HashMap<String, Integer> modeWeights;

	public RecommendationSystemChallengeFilteringAndSorting() {
		this.modeWeights = new HashMap<String, Integer>();
		// just for test
		modeWeights.put("Walk", 10);
	}

	public Map<String, List<ChallengeDataDTO>> filterAndSort(Map<String, List<ChallengeDataDTO>> evaluatedChallenges,
			List<LeaderboardPosition> leaderboard) {
		Map<String, List<ChallengeDataDTO>> result = new HashMap<String, List<ChallengeDataDTO>>();
		Double wi = 0.0;
		for (String playerId : evaluatedChallenges.keySet()) {
			// creating two list for the challenges that can improve the player
			// in the leader board and not improving
			List<ChallengeDataDTO> improvingLeaderboard = new ArrayList<ChallengeDataDTO>();
			List<ChallengeDataDTO> notImprovingLeaderboard = new ArrayList<ChallengeDataDTO>();

			for (ChallengeDataDTO challenge : evaluatedChallenges.get(playerId)) {
				Double baseline = (Double) challenge.getData().get("baseline");
				Double target = 0.0;
				if (challenge.getData().get("target") instanceof Integer) {
					target = new Double((Integer) challenge.getData().get("target"));
				} else {
					target = (Double) challenge.getData().get("target");
				}
				Integer weight = getWeight((String) challenge.getData().get("counterName"));
				Double percentageImprovment = 0.0;
				if (baseline != null) {
					percentageImprovment = Math.round(Math.abs(baseline - target) * 100.0 / baseline) / 100.0;
				} else {
					percentageImprovment = 1.0;
				}
				Integer prize = (Integer) challenge.getData().get("bonusScore");
				// System.out.println();
				// calculating the WI for each mode based on weight of Mode and
				// improvement percentage
				wi = percentageImprovment * weight;
				challenge.getData().put("wi", wi);
				// finding the position of the player in the leader board
				LeaderboardPosition position = findPosition(leaderboard, playerId);
				if (position.getIndex() == 0) {
					// all the challenges;
					improvingLeaderboard.add(challenge);
				} else {
					LeaderboardPosition pos = findScoreMoreThanMe(leaderboard, position.getIndex(),
							position.getScore());
					if (prize + position.getScore() > pos.getScore()) {
						// I like this challenge, because improve my position
						// into the leader board
						improvingLeaderboard.add(challenge);
					} else {
						notImprovingLeaderboard.add(challenge);
					}
				}
			}
			// make some initialization for result data structure
			if (result.get(playerId) == null) {
				result.put(playerId, new ArrayList<ChallengeDataDTO>());
			}
			// sorting both lists
			Collections.sort(improvingLeaderboard, new DifficultyWiComparator());
			// TODO : we have to test the sorting
			Collections.sort(notImprovingLeaderboard, new DifficultyPrizeComparator());

			if (notImprovingLeaderboard.size() == 1) {
				System.out.println();
			}
			// add for first the improving leaderboard challenges
			if (!improvingLeaderboard.isEmpty()) {
				result.get(playerId).addAll(improvingLeaderboard);
			}
			if (!notImprovingLeaderboard.isEmpty()) {
				result.get(playerId).addAll(notImprovingLeaderboard);
			}
		}
		// TODO; add the filter
		return result;
	}

	private LeaderboardPosition findScoreMoreThanMe(List<LeaderboardPosition> leaderboard, Integer index,
			Integer score) {
		for (int i = index; i >= 0; i--) {
			if (leaderboard.get(i).getScore() > score) {
				return leaderboard.get(i);
			}
		}
		// in a game with no score, we'll get a null pointer exception
		return null;
	}

	private LeaderboardPosition findPosition(List<LeaderboardPosition> leaderboard, String playerId) {
		Integer currentIndex = 0;
		for (LeaderboardPosition pos : leaderboard) {
			if (playerId == pos.getPlayerId()) {
				pos.setIndex(currentIndex);
				return pos;
			} else {
				currentIndex++;
			}
		}
		return null;
	}

	private Integer getWeight(String counterName) {
		for (String mode : modeWeights.keySet()) {
			if (counterName.startsWith(mode)) {
				// in our example this means we are into walk challenge
				return modeWeights.get(mode);
			}

		}
		return 1;
	}

}
