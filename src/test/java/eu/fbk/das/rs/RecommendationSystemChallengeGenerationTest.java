package eu.fbk.das.rs;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemChallengeGeneration;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.calculator.DifficultyCalculator;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;


import eu.fbk.das.GamificationEngineRestFacade;
import it.smartcommunitylab.model.PlayerStateDTO;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class RecommendationSystemChallengeGenerationTest extends ChallengesBaseTest {

    private GamificationEngineRestFacade facade;
    private RecommendationSystemConfig configuration;
    private DateTime now;
    private RecommendationSystemChallengeGeneration rcg;

    @Before
    public void setup() {
        now = new DateTime();
        rs = new RecommendationSystem();
        rcg = new RecommendationSystemChallengeGeneration(rs);
    }


    private boolean checkChallenge(ChallengeExpandedDTO cdd) {
        if (StringUtils.isBlank(cdd.getModelName())) {
            return false;
        }
        if (StringUtils.isBlank(cdd.getInstanceName())) {
            return false;
        }
        if (cdd.getStart() == null) {
            return false;
        }
        if (cdd.getEnd() == null) {
            return false;
        }
        if (StringUtils.isEmpty((String) cdd.getData("bonusPointType"))) {
            return false;
        }
        if (cdd.getData("bonusScore") == null) {
            return false;
        }
        if (cdd.getModelName().equals("percentageIncrement")
                && cdd.getData("baseline") == null) {
            return false;
        }
        if (StringUtils.isEmpty((String) cdd.getData("counterName"))) {
            return false;
        }
        if (StringUtils.isEmpty((String) cdd.getData("periodName"))) {
            return false;
        }
        if (StringUtils.isEmpty((String) cdd.getData("challengeName"))) {
            return false;
        }
        return true;
    }

    @Test
    /**
     * Evaluate challenges adding difficulty and computing the prize

    public void challengeValuator() {
        Map<String, Player> m_users = facade.readGameState(GAMEID);
        List<Player> gameData = new ArrayList<Player>();
        for (String pId: m_users.keySet())
            gameData.add(m_users.get(pId));

        Map<String, List<ChallengeExpandedDTO>> challengeCombinations = rcg
                .generateAll(gameData);
        // evaluate all challenges
        RecommendationSystemChallengeValuator valuator = new RecommendationSystemChallengeValuator(
        );

        // Map<String, List<ChallengeExpandedDTO>> evaluatedChallenges = valuator.valuate(challengeCombinations, gameData);
        Map<String, List<ChallengeExpandedDTO>> evaluatedChallenges = null;
        for (String playerId : evaluatedChallenges.keySet()) {
            // generate at least two challenge
            for (ChallengeExpandedDTO challenge : evaluatedChallenges.get(playerId)) {
                assertTrue(checkValuatedChallenge(challenge));
            }
        }
    }


    private boolean checkValuatedChallenge(ChallengeExpandedDTO cdd) {
        Map<String, Object> data = cdd.getData();
        if (!(cdd.getData("bonusScore") instanceof Double)) {
            return false;
        }
        Double bonus = (Double) cdd.getData("bonusScore");
        if (bonus <= 0.0) {
            return false;
        }
        if (cdd.getData("difficulty") == null) {
            return false;
        }
        if (!(cdd.getData("difficulty") instanceof Integer)) {
            return false;
        }
        Integer difficulty = (Integer) cdd.getData("difficulty");
        if (difficulty == DifficultyCalculator.EASY
                || difficulty == DifficultyCalculator.MEDIUM
                || difficulty == DifficultyCalculator.HARD
                || difficulty == DifficultyCalculator.VERY_HARD) {
            return true;
        }
        return false;
    }
         */

    public void configurationFilteringTest() {
        // at least two users for filtering
        ChallengesConfig rc = new ChallengesConfig();
        assertTrue(!rc.isUserfiltering()
                || (rc.isUserfiltering() && rc.getPlayerIds().size() > 2));
    }

    @Test
    public void generatorContentEmptyTest() throws IOException {

        List<PlayerStateDTO> input = new ArrayList<PlayerStateDTO>();
        Map<String, List<ChallengeExpandedDTO>> result = rcg.generateAll(input);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testConfigIsModeNull() throws IOException {
        assertTrue(!ChallengesConfig.isDefaultMode(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigFilteringIdsNull() {
        @SuppressWarnings("unused")
        ChallengesConfig config = new ChallengesConfig();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDateNotNull() {
        List<PlayerStateDTO> gameData = new ArrayList<PlayerStateDTO>();
        rcg.generateAll(gameData);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDateStartEndMustBeDifferent() {
        List<PlayerStateDTO> gameData = new ArrayList<PlayerStateDTO>();
        rcg.generateAll(gameData);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDateStartMustBeBeforeEnd() {
        List<PlayerStateDTO> gameData = new ArrayList<PlayerStateDTO>();
        rcg.generateAll(gameData);
    }

    @Test
    public void testUseFilteringAndFilterIds() {
        assertTrue(ChallengesConfig.isUserfiltering()
                && !ChallengesConfig.getPlayerIds().isEmpty());
    }



}
