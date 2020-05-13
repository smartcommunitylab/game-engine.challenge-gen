package eu.fbk.das.api.exec;

import eu.fbk.das.api.RecommenderSystemAPI;
import eu.fbk.das.api.RecommenderSystemImpl;
import org.joda.time.DateTime;

import java.util.HashMap;

import static eu.fbk.das.rs.challenges.generation.RecommendationSystem.getChallengeWeek;

// Chiama recommender system per generazione settimanale
public class RecommenderSystemExec {

    String host = "http://localhost:8010/gamification/";
    String user = "long-rovereto";
    String pass = "test";
    String gameId = "5b7a885149c95d50c5f9d442";

    protected RecommenderSystemAPI api;
    protected HashMap<String, String> conf;
    protected DateTime execDate;
    protected HashMap<String, Object> challengeValues;
    protected HashMap<String, String> reward;


    public void prepare() {
        api = new RecommenderSystemImpl();
        conf = new HashMap<String, String>() {{ put("host", host); put("user", user); put("pass", pass); put("gameId", gameId);}};

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

        challengeValues = new HashMap<String, Object>();
        challengeValues.put("start", String.valueOf(startDate.getMillis() / 1000));
        challengeValues.put("end", String.valueOf(startDate.getMillis() / 1000));
        challengeValues.put("exec", String.valueOf(execDate.getMillis() / 1000));
        challengeValues.put("periodName", "weekly");

        challengeValues.put("challengeWeek", getChallengeWeek(execDate));

        reward = new HashMap<>();
        reward.put("scoreType", "green leaves");
    }

}
