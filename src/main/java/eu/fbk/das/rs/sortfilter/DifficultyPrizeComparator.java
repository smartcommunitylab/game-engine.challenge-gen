package eu.fbk.das.rs.sortfilter;

import java.util.Comparator;

import eu.trentorise.game.challenges.model.ChallengeDataDTO;

public class DifficultyPrizeComparator implements Comparator<ChallengeDataDTO> {

	@Override
	public int compare(ChallengeDataDTO o1, ChallengeDataDTO o2) {
		int difficulty1 = (int) o1.getData().get("difficulty");
		int difficulty2 = (int) o2.getData().get("difficulty");
		long bonusScore1 = (long) o1.getData().get("bonusScore");
		long bonusScore2 = (long) o2.getData().get("bonusScore");
		double wi1 = (double) o1.getData().get("wi");
		double wi2 = (double) o2.getData().get("wi");

		if (difficulty1 < difficulty2) {
			return -1;
		}
		if (difficulty1 > difficulty2) {
			return 1;
		}
		if (bonusScore1 < bonusScore2) {
			return 1;
		}
		if (bonusScore1 > bonusScore2) {
			return -1;
		}
		if (wi1 < wi2) {
			return 1;
		}
		if (wi1 > wi2) {
			return -1;
		}
		return 0;

		//
		// if (difficulty1 == difficulty2 && bonusScore1 == bonusScore2 && wi1
		// == wi2) {
		// return 0;
		// } else if (difficulty1 == difficulty2 && bonusScore1 > bonusScore2) {
		// return -1;
		// } else if (difficulty1 == difficulty2 && bonusScore1 < bonusScore2) {
		// return 1;
		// } else if (difficulty1 > difficulty2) {
		// return 1;
		// } else if (difficulty1 < difficulty2) {
		// return -1;
		// }
		// return 0;
	}

}