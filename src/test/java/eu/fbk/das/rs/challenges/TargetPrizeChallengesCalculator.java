package eu.fbk.das.rs.challenges;

import eu.fbk.das.rs.challenges.generation.RecommendationSystemChallengeGeneration;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

public class TargetPrizeChallengesCalculator {

    protected String HOST = "https://dev.smartcommunitylab.it/gamification-copia/";
    protected String CONTEXT = "gengine/";
    protected String USERNAME = "long-rovereto";
    protected String PASSWORD = "long_RoVg@me";
    protected String GAMEID = "5b7a885149c95d50c5f9d442";

    private GamificationEngineRestFacade facade;

    private LocalDate now;

    @Before
    public void setup() {

        now = new LocalDate();

        facade = new GamificationEngineRestFacade(HOST, USERNAME, PASSWORD);
    }

    @Test
    public void test() {
        String pId_1 = "";
        String pId_2 = "";
        Object res = targetPrizeChallengesCompute(pId_1, pId_2, "Walk_Km");
    }

    private Object targetPrizeChallengesCompute(String pId_1, String pId_2, String walk_km) {

        Response stats = facade.readGameStatistics(GAMEID);


        return null;
    }

}
