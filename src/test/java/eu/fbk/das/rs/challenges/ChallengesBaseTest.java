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
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Set;

import static eu.fbk.das.rs.utils.Utils.p;

public class ChallengesBaseTest {

    protected boolean prod = false;

    protected GamificationEngineRestFacade facade;
    protected RecommendationSystem rs;
    protected HashMap<String, String> conf;
    protected RecommendationSystemChallengeValuator rscv;
    protected RecommendationSystemChallengeGeneration rscg;
    protected RecommendationSystemChallengeFilteringAndSorting rscf;

    @Before
    public void setup() {
        conf = new GamificationConfig(prod).extract();

        facade = new GamificationEngineRestFacade(conf.get("HOST"),
                conf.get("USERNAME"), conf.get("PASSWORD"));

        rs = new RecommendationSystem(conf);

        rscv = rs.rscv;

        rscg = rs.rscg;

        rscf = rs.rscf;
    }

    @Test
    public void test() {
        PlayerStateDTO res = facade.getPlayerState(conf.get("GAMEID"), "25706");
        Set<GameConcept> scores =  res.getState().get("ChallengeConcept");
        for (GameConcept gc : scores) {
            ChallengeConcept cha = (ChallengeConcept) gc;
            p(cha.getName());

            p(cha.getStateDate());
        }
    }


}
