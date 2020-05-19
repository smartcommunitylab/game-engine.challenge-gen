package eu.fbk.das.rs.sortfilter;


import it.smartcommunitylab.model.ChallengeAssignmentDTO;

import java.util.Comparator;

public class DifficultyPrizeComparator implements Comparator<ChallengeAssignmentDTO> {

    @Override
    public int compare(ChallengeAssignmentDTO o1, ChallengeAssignmentDTO o2) {
        int difficulty1 = (int) o1.getData().get("difficulty");
        int difficulty2 = (int) o2.getData().get("difficulty");
        int bonusScore1 = (int) o1.getData().get("bonusScore");
        int bonusScore2 = (int) o2.getData().get("bonusScore");
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