package eu.fbk.das.rs.sortfilter;


import java.util.Comparator;

import eu.fbk.das.model.ChallengeExpandedDTO;

public class DifficultyPrizeComparator implements Comparator<ChallengeExpandedDTO> {

    @Override
    public int compare(ChallengeExpandedDTO o1, ChallengeExpandedDTO o2) {
        double difficulty1 = o1.getDataFL("difficulty");
        double difficulty2 =  o2.getDataFL("difficulty");
        double bonusScore1 = o1.getDataFL("bonusScore");
        double bonusScore2 = o2.getDataFL("bonusScore");
        Double wi1 = (Double) o1.getData("wi");
        Double wi2 = (Double) o2.getData("wi");

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