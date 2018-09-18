package eu.fbk.das.rs.sortfilter;

import eu.trentorise.game.challenges.model.ChallengeDataDTO;

import java.util.Comparator;

public class DifficultyPrizeComparator implements Comparator<ChallengeDataDTO> {

    @Override
    public int compare(ChallengeDataDTO o1, ChallengeDataDTO o2) {
        int difficulty1 = (int) o1.getData().get("difficulty");
        int difficulty2 = (int) o2.getData().get("difficulty");
        Double bonusScore1 = (Double) o1.getData().get("bonusScore");
        Double bonusScore2 = (Double) o2.getData().get("bonusScore");
        Double wi1 = (Double) o1.getData().get("wi");
        Double wi2 = (Double) o2.getData().get("wi");

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
    }

}