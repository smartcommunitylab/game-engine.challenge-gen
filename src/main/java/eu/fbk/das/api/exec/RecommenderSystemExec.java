package eu.fbk.das.api.exec;

import static eu.fbk.das.rs.challenges.generation.RecommendationSystem.getChallengeWeek;

import java.util.HashMap;
import java.util.Map;

import eu.fbk.das.GamificationConfig;
import eu.fbk.das.model.ChallengeExpandedDTO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import eu.fbk.das.api.RecommenderSystemAPI;
import eu.fbk.das.api.RecommenderSystemImpl;

// Chiama recommender system per generazione settimanale
public class RecommenderSystemExec {

    protected RecommenderSystemAPI api;
    protected HashMap<String, String> conf;
    protected DateTime execDate;
    public HashMap<String, Object> config;
    protected HashMap<String, String> reward;

    public RecommenderSystemExec() {
        api = new RecommenderSystemImpl();
    }


    public void prepare(Map<String, String> conf) {
        // remove last blackslash from url
        String url = conf.get("HOST").replaceAll("/$", "");
        conf.put("HOST", url);

        if (conf.containsKey("execDate")) {
            execDate = DateTime.parse(conf.get("execDate"));
        }
        else
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
