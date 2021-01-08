package eu.fbk.das;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.utils.Utils;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.ChallengeConcept;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;
import org.joda.time.DateTime;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static eu.fbk.das.rs.challenges.ChallengeUtil.getPeriodScore;
import static eu.fbk.das.utils.Utils.p;
import static eu.fbk.das.utils.Utils.pf;

public class RandomTest extends ChallengesBaseTest {

    public RandomTest() {
        prod = false;
    }

    @Test
    public void checkIfEvilCyclist() {
        

    }
}