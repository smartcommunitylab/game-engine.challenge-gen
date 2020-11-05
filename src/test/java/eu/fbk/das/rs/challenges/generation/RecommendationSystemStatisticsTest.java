package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.utils.Utils;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Map;

import static eu.fbk.das.utils.Utils.p;
import static eu.fbk.das.utils.Utils.pf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RecommendationSystemStatisticsTest extends ChallengesBaseTest {

    @Test
    public void generate() {
        RecommendationSystem rs = new RecommendationSystem();
        RecommendationSystemStatistics rss = new RecommendationSystemStatistics(rs);

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

    @Test
    public void test_online_statistics() {
        RecommendationSystemStatistics rss = new RecommendationSystemStatistics(rs,true);
        DateTime date = new DateTime(2020,2,10,10,0);
        rss.checkAndUpdateStats(date);
        Map<Integer, Double> greenLeavesQuantiles = rss.getQuantiles("green leaves");
        assertThat(greenLeavesQuantiles.get(0), is(Double.valueOf(6.0)));
        assertThat(greenLeavesQuantiles.get(1), is(Double.valueOf(53.9)));
    }

}