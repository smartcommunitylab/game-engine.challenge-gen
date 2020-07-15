package eu.fbk.das.api.exec;

import static eu.fbk.das.rs.challenges.generation.RecommendationSystem.getChallengeWeek;

import java.util.HashMap;
import java.util.Map;

import eu.fbk.das.model.ChallengeExpandedDTO;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import eu.fbk.das.api.RecommenderSystemAPI;
import eu.fbk.das.api.RecommenderSystemImpl;

// Chiama recommender system per generazione settimanale
public class RecommenderSystemExec {

    String host = "http://localhost:8010/gamification";
    String user = "long-rovereto";
    String pass = "test";
    String gameId = "5b7a885149c95d50c5f9d442";

    protected RecommenderSystemAPI api;
    protected HashMap<String, String> conf;
    protected DateTime execDate;
    protected HashMap<String, Object> config;
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
        DateTime startDate = execDate.plusDays(d);
        startDate = startDate.minusDays(2);

        DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();

        config = new HashMap<String, Object>();
        config.put("start", fmt.print(startDate));
        config.put("duration", "7d");
        config.put("exec", execDate.toDate());
        config.put("periodName", "weekly");

        config.put("challengeWeek", getChallengeWeek(execDate));

        reward = new HashMap<>();
        reward.put("scoreType", "green leaves");
    }

    public boolean upload(Map<String, String> conf, ChallengeExpandedDTO cha) {
        if (conf == null) conf = this.conf;

        return api.assignSingleChallenge(conf, cha);
    }
}
