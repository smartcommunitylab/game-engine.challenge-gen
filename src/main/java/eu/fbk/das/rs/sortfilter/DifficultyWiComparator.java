package eu.fbk.das.rs.sortfilter;

import eu.fbk.das.model.ChallengeExpandedDTO;
import it.smartcommunitylab.model.ChallengeAssignmentDTO;

import java.util.Comparator;

public class DifficultyWiComparator implements Comparator<ChallengeExpandedDTO> {

    @Override
    public int compare(ChallengeExpandedDTO o1, ChallengeExpandedDTO o2) {
        // sorting the challenges based on less difficulty and WI
        int difficulty1 = (int) o1.getData("difficulty");
        int difficulty2 = (int) o2.getData("difficulty");
        double wi1 = (double) o1.getData("wi");
        double wi2 = (double) o2.getData("wi");
        if (difficulty1 == difficulty2 && wi1 == wi2) {
            return 0;
        } else if (difficulty1 == difficulty2 && wi1 > wi2) {
            return -1;
        } else if (difficulty1 == difficulty2 && wi1 < wi2) {
            return 1;
        } else if (difficulty1 > difficulty2) {
            return 1;
        } else if (difficulty1 < difficulty2) {
            return -1;
        }

        return 0;
    }

}
