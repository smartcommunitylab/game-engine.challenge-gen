package eu.fbk.das.rs.challenges;

import eu.fbk.das.rs.challenges.generation.RecommendationSystemChallengeGeneration;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.trentorise.game.challenges.rest.ChallengeConcept;
import eu.trentorise.game.challenges.rest.Player;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import static eu.fbk.das.rs.utils.Utils.p;

public class ChallengesBaseTest {


    protected GamificationEngineRestFacade facade;
    protected RecommendationSystemConfig cfg;
    protected RecommendationSystemChallengeValuator rscv;
    protected RecommendationSystemChallengeGeneration rscg;
    protected RecommendationSystemChallengeFilteringAndSorting rscf;

    private LocalDate now;

    protected String HOST = "http://localhost:18000/gamification/";
    protected String CONTEXT = "gengine/";
    protected String USERNAME = "long-rovereto";
    protected String PASSWORD = "ciao";
    protected String GAMEID = "59a91478e4b0c9db6800afaf";
    protected String INSERT_CONTEXT = "todo";
    protected String SAVE_ITINERARY = "todo";
    protected String RELEVANT_CUSTOM_DATA = "todo";

    private GamificationEngineRestFacade facadeLocal;


    @Before
    public void setup() {
        facadeLocal = new GamificationEngineRestFacade(HOST,
                USERNAME, PASSWORD);

        cfg = new RecommendationSystemConfig();

        now = new LocalDate();

        facade = new GamificationEngineRestFacade(cfg.get("HOST"),
                cfg.get("USERNAME"), cfg.get("PASSWORD"));

        rscv = new RecommendationSystemChallengeValuator(cfg);

        rscg = new RecommendationSystemChallengeGeneration(cfg, rscv);

        rscf = new RecommendationSystemChallengeFilteringAndSorting(cfg);
    }

    @Test
    public void test() {
        Player res = facade.getPlayerState(cfg.get("GAME_ID"), "25706");
        for (ChallengeConcept cha: res.getState().getChallengeConcept()) {
            p(cha.getName());
            p(cha.getStateDate());
        }
    }


}
