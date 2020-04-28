package eu.fbk.das.api;

import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class RecommendrSystemImplTest {

    private RecommenderSystemAPI api;
    private HashMap<String, String> conf;
    private DateTime execDate;
    private HashMap<String, String> challengeValues;
    private HashMap<String, String> reward;

    @Before
    public void prepare() {
        api = new RecommenderSystemImpl();
        conf = new HashMap<String, String>() {{ put("HOST", ""); put("USER", ""); put("PASS", ""); put("GAME_ID", "");}};

        execDate = new DateTime()
                .withHourOfDay(0)
                .withMinuteOfHour(0)
                .withSecondOfMinute(0);

        // Set next monday as start, and next sunday as end
        int week_day = execDate.getDayOfWeek();
        int d = (7 - week_day) + 1;

        DateTime lastMonday = execDate.minusDays(week_day - 1).minusDays(7);

        DateTime startDate = execDate.plusDays(d);
        startDate = startDate.minusDays(2);
        DateTime endDate = startDate.plusDays(7);

        challengeValues = new HashMap<String, String>();
        challengeValues.put("start", String.valueOf(startDate.getMillis() / 1000));
        challengeValues.put("end", String.valueOf(startDate.getMillis() / 1000));

        reward = new HashMap<>();
    }

    @Test
    public void createCoupleChallengeWeeklyTest() {
        Set<String> modeList = new HashSet<String>(Arrays.asList(ChallengesConfig.WALK_KM,ChallengesConfig.BIKE_KM,ChallengesConfig.GREEN_LEAVES));

        String challengeType = "groupCooperative";
        // String challengeType = "groupCompetitiveTime";
        // String challengeType = "groupCompetitivePerformance"

        api.createCoupleChallengeWeekly(conf, modeList, challengeType, challengeValues, "all", reward);
    }
}
