package eu.fbk.das.api;

import static eu.fbk.das.utils.Utils.p;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Set;

import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import org.junit.Test;

import eu.fbk.das.api.exec.RecommenderSystemGroup;
import eu.fbk.das.api.exec.RecommenderSystemTantum;
import eu.fbk.das.api.exec.RecommenderSystemWeekly;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.GameConcept;

public class ApiSimpleTest extends ChallengesBaseTest {


    public ApiSimpleTest() {
        prod = false;
    }

    @Test
    public void baseTest() {
        Set<String> ps = facade.getGamePlayers(conf.get("GAMEID"));
        String analyze = "";
        for (String pId: ps) {
            p(pId);
            analyze = pId;
        }

        PlayerStateDTO player = facade.getPlayerState(conf.get("GAMEID"), analyze);
        Set<GameConcept> scores =  player.getState().get("PointConcept");
        for (GameConcept gc : scores) {
            p(gc);
        }
    }

    @Test
    public void weekSingle() {
        RecommenderSystemWeekly rsw = new RecommenderSystemWeekly();
      //   rsw.exec(conf, "1069");
        rsw.go(conf, "28467", null, null);
    }

    @Test
    public void weekGroup() {
        RecommenderSystemGroup rsw = new RecommenderSystemGroup();

        String challengeType = "groupCooperative";
        // String challengeType = "groupCompetitiveTime";
        // String challengeType = "groupCompetitivePerformance"

        rsw.exec(conf, "all", challengeType, null);
    }

    @Test
    public void testSingle() {
        RecommenderSystemTantum rsw = new RecommenderSystemTantum();

        rsw.exec(conf, "survey", new HashMap<>(), "1069");
    }

    @Test
    public void testDates() {
        RecommenderSystemGroup rse = new RecommenderSystemGroup();
        rse.prepare();
        HashMap<String, Object> config = rse.config;
        GroupExpandedDTO gcd = new GroupExpandedDTO();

        gcd.setDates(config.get("start"), config.get("duration"));
    }
}
