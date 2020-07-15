package eu.fbk.das.rs.challenges;

import eu.fbk.das.GamificationEngineRestFacade;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemChallengeGeneration;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.ChallengeConcept;
import it.smartcommunitylab.model.ext.GameConcept;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static eu.fbk.das.rs.utils.Utils.p;

public class ChallengesBaseTest {


    protected GamificationEngineRestFacade facade;
    protected RecommendationSystem rs;
    protected RecommendationSystemConfig cfg;
    protected RecommendationSystemChallengeValuator rscv;
    protected RecommendationSystemChallengeGeneration rscg;
    protected RecommendationSystemChallengeFilteringAndSorting rscf;

    private LocalDate now;

    //protected String HOST = "https://dev.smartcommunitylab.it/gamification";
    protected String HOST = "https://tn.smartcommunitylab.it/gamification2/";
    protected String CONTEXT = "gengine/";
    protected String USERNAME = "long-rovereto";
    protected String PASSWORD = "long_RoVg@me";
    protected String GAMEID = "5d9353a3f0856342b2dded7f";
    protected String INSERT_CONTEXT = "todo";
    protected String SAVE_ITINERARY = "todo";
    protected String RELEVANT_CUSTOM_DATA = "todo";

    protected GamificationEngineRestFacade facadeLocal;


    @Before
    public void setup() {
        facadeLocal = new GamificationEngineRestFacade(HOST,
                USERNAME, PASSWORD);

        cfg = new RecommendationSystemConfig();

        now = new LocalDate();

        facade = new GamificationEngineRestFacade(cfg.get("HOST"),
                cfg.get("USERNAME"), cfg.get("PASSWORD"));

        rs = new RecommendationSystem(cfg);

        rscv = rs.rscv;

        rscg = rs.rscg;

        rscf = rs.rscf;
    }

    @Test
    public void test() {
        PlayerStateDTO res = facade.getPlayerState(cfg.get("gameId"), "25706");
        Set<GameConcept> scores =  res.getState().get("ChallengeConcept");
        for (GameConcept gc : scores) {
            ChallengeConcept cha = (ChallengeConcept) gc;
            p(cha.getName());

            p(cha.getStateDate());
        }
    }


}
