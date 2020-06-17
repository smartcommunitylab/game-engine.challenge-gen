package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.api.RecommenderSystemAPI;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.rs.utils.Utils;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Map;

import static eu.fbk.das.rs.utils.Utils.p;
import static eu.fbk.das.rs.utils.Utils.pf;
import static org.junit.Assert.*;

public class RecommendationSystemStatisticsTest extends ChallengesBaseTest {

    @Test
    public void generate() {
        RecommendationSystem rs = new RecommendationSystem();
;        RecommendationSystemStatistics rss = new RecommendationSystemStatistics(rs);

        DateTime today = new DateTime();
        for (int i = 10; i >= 0; i--) {
            DateTime nn = today.minusDays(i);
            rss.execDate = nn;
            pf("date: %s\n", Utils.printDate(nn));
            Map<String, Map<Integer, Double>> qua = rss.updateStats();
            p(qua.get("walk_km"));
        }

        for (int i = 1; i < 5; i++) {
            DateTime nn = today.plusDays(i);
            rss.execDate = nn;
            pf("date: %s\n", Utils.printDate(nn));
            Map<String, Map<Integer, Double>> qua = rss.updateStats();
            p(qua.get("walk_km"));
        }

    }

}