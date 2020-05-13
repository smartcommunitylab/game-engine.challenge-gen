package eu.fbk.das.api;

import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static eu.fbk.das.rs.utils.Utils.p;

public class ApiTest {

    String host = "http://localhost:8010/gamification/";
    String user = "long-rovereto";
    String pass = "test";
    String gameId = "5b7a885149c95d50c5f9d442";

    private GamificationEngineRestFacade facade;

    @Before
    public void prepare() {
        facade = new GamificationEngineRestFacade(host, user, pass);
    }

    @Test
    public void baseTest() {
        Set<String> ps = facade.getGamePlayers(gameId);
        for (String pId: ps)
            p(pId);
    }

    @Test
    public void singleAssign() {

    }
}
