package eu.fbk.das.api;

import eu.fbk.das.GamificationEngineRestFacade;
import eu.fbk.das.api.exec.RecommenderSystemWeekly;
import eu.fbk.das.model.ChallengeExpandedDTO;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.GameConcept;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static eu.fbk.das.rs.utils.Utils.p;

public class ApiTest {

    String host = "http://localhost:8010/gamification";
    String user = "long-rovereto";
    String pass = "test";
    String gameId = "5b7a885149c95d50c5f9d442";

    private GamificationEngineRestFacade facade;
    private HashMap<String, String> conf;

    @Before
    public void prepare() {
        facade = new GamificationEngineRestFacade(host, user, pass);
        conf = new HashMap<String, String>();
        conf.put("host", host);
        conf.put("user", user);
        conf.put("pass", pass);
        conf.put("gameId", gameId);
    }

    @Test
    public void baseTest() {
        Set<String> ps = facade.getGamePlayers(gameId);
        String analyze = "";
        for (String pId: ps) {
            p(pId);
            analyze = pId;
        }

        PlayerStateDTO player = facade.getPlayerState(gameId, analyze);
        Set<GameConcept> scores =  player.getState().get("PointConcept");
        for (GameConcept gc : scores) {
            p(gc);
        }
    }

    @Test
    public void weekCreate() {
        RecommenderSystemWeekly rsw = new RecommenderSystemWeekly();
        rsw.exec(conf, "1069");
    }
}
