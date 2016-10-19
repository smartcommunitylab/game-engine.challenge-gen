package eu.fbk.das.rs.sortfilter;

import java.util.Comparator;

import eu.trentorise.game.challenges.model.ChallengeDataDTO;

public class DifficultyPrizeComparator implements Comparator<ChallengeDataDTO> {

	@Override
	public int compare(ChallengeDataDTO o1, ChallengeDataDTO o2) {
		// sorting the challenges based on less difficulty and WI
		int difficulty1 = (int) o1.getData().get("difficulty");
		int difficulty2 = (int) o2.getData().get("difficulty");
		// TODO we are here, we have to add comparison also on WI
		int bonusScore1 = (int) o1.getData().get("bonusScore");
		int bonusScore2 = (int) o2.getData().get("bonusScore");
		if (difficulty1 == difficulty2 && bonusScore1 == bonusScore2) {
			return 0;
		} else if (difficulty1 == difficulty2 && bonusScore1 > bonusScore2) {
			return -1;
		} else if (difficulty1 == difficulty2 && bonusScore1 < bonusScore2) {
			return 1;
		} else if (difficulty1 > difficulty2) {
			return 1;
		} else if (difficulty1 < difficulty2) {
			return -1;
		}
		return 0;
	}

}