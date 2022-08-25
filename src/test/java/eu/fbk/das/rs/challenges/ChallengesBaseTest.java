package eu.fbk.das.rs.challenges;

import eu.fbk.das.GamificationEngineRestFacade;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemChallengeGeneration;
import eu.fbk.das.GamificationConfig;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.ChallengeConcept;
import it.smartcommunitylab.model.ext.GameConcept;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static eu.fbk.das.utils.Utils.p;

public class ChallengesBaseTest {

    protected boolean prod = false;

    protected GamificationEngineRestFacade facade;
    protected RecommendationSystem rs;
    protected Map<String, String> conf;
    protected RecommendationSystemChallengeValuator rscv;
    protected RecommendationSystemChallengeGeneration rscg;
    protected RecommendationSystemChallengeFilteringAndSorting rscf;

    @Before
    public void setup() {
        conf = new GamificationConfig(prod).extract();

        facade = new GamificationEngineRestFacade(conf.get("HOST"),
                conf.get("API_USER"), conf.get("API_PASS"));

        rs = new RecommendationSystem(conf);

        rscv = rs.rscv;

        rscg = rs.rscg;

        rscf = rs.rscf;

        System.err.close();
        System.setErr(System.out);
    }


}
