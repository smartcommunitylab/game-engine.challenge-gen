package eu.fbk.das.rs;

import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemChallengeGeneration;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.challenges.calculator.DifficultyCalculator;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Player;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
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
        facade = new GamificationEngineRestFacade(HOST + CONTEXT,
                USERNAME, PASSWORD);
        configuration = new RecommendationSystemConfig();
        now = new DateTime();

        rcg = new RecommendationSystemChallengeGeneration(configuration, null);
    }

    @Test
    /**
     * Read game status from gamification engine
     */
    public void readGameTest() {
        assertTrue(facade != null);
        // read data from gamification engine
        Map<String, Player> gameData = facade.readGameState(GAMEID);

        assertTrue(!gameData.isEmpty());
    }

    @Test
    /**
     * Generate all possible combinations of challenge for every player in the
     * game
     */
    public void challengeGeneration() {
        assertTrue(facade != null);
        Map<String, Player> m_users = facade.readGameState(GAMEID);
        List<Player> gameData = new ArrayList<Player>();
        for (String pId: m_users.keySet())
            gameData.add(m_users.get(pId));

        // create all challenges combinations
        Map<String, List<ChallengeDataDTO>> challengeCombinations = rcg
                .generateAll(gameData);

        for (String playerId : challengeCombinations.keySet()) {
            // generate at least two challenge for player
            assertTrue(challengeCombinations.get(playerId).size() > 2);
            for (ChallengeDataDTO challenge : challengeCombinations
                    .get(playerId)) {
                assertTrue(checkChallenge(challenge));
            }
        }

    }

    private boolean checkChallenge(ChallengeDataDTO cdd) {
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
        if (MapUtils.isEmpty(cdd.getData())) {
            return false;
        }
        Map<String, Object> data = cdd.getData();
        if (data.get("target") == null) {
            return false;
        }
        if (StringUtils.isEmpty((String) data.get("bonusPointType"))) {
            return false;
        }
        if (data.get("bonusScore") == null) {
            return false;
        }
        if (cdd.getModelName().equals("percentageIncrement")
                && data.get("baseline") == null) {
            return false;
        }
        if (StringUtils.isEmpty((String) data.get("counterName"))) {
            return false;
        }
        if (StringUtils.isEmpty((String) data.get("periodName"))) {
            return false;
        }
        if (StringUtils.isEmpty((String) data.get("challengeName"))) {
            return false;
        }
        return true;
    }

    @Test
    /**
     * Evaluate challenges adding difficulty and computing the prize
     */
    public void challengeValuator() {
        Map<String, Player> m_users = facade.readGameState(GAMEID);
        List<Player> gameData = new ArrayList<Player>();
        for (String pId: m_users.keySet())
            gameData.add(m_users.get(pId));

        Map<String, List<ChallengeDataDTO>> challengeCombinations = rcg
                .generateAll(gameData);
        // evaluate all challenges
        RecommendationSystemChallengeValuator valuator = new RecommendationSystemChallengeValuator(
                configuration);

        // Map<String, List<ChallengeDataDTO>> evaluatedChallenges = valuator.valuate(challengeCombinations, gameData);
        Map<String, List<ChallengeDataDTO>> evaluatedChallenges = null;
        for (String playerId : evaluatedChallenges.keySet()) {
            // generate at least two challenge
            for (ChallengeDataDTO challenge : evaluatedChallenges.get(playerId)) {
                assertTrue(checkValuatedChallenge(challenge));
            }
        }
    }

    private boolean checkValuatedChallenge(ChallengeDataDTO cdd) {
        Map<String, Object> data = cdd.getData();
        if (!(data.get("bonusScore") instanceof Double)) {
            return false;
        }
        Double bonus = (Double) data.get("bonusScore");
        if (bonus <= 0.0) {
            return false;
        }
        if (data.get("difficulty") == null) {
            return false;
        }
        if (!(data.get("difficulty") instanceof Integer)) {
            return false;
        }
        Integer difficulty = (Integer) data.get("difficulty");
        if (difficulty == DifficultyCalculator.EASY
                || difficulty == DifficultyCalculator.MEDIUM
                || difficulty == DifficultyCalculator.HARD
                || difficulty == DifficultyCalculator.VERY_HARD) {
            return true;
        }
        return false;
    }

    @Test
    public void configurationFilteringTest() {
        // at least two users for filtering
        ChallengesConfig rc = new ChallengesConfig();
        assertTrue(!rc.isUserfiltering()
                || (rc.isUserfiltering() && rc.getPlayerIds().size() > 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void recommendationSystemWriteNullChallenges() throws IOException {
        RecommendationSystem rs = new RecommendationSystem(configuration);
        rs.writeToFile(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generatorNullConfigTest() throws IOException {
        @SuppressWarnings("unused")
        RecommendationSystemChallengeGeneration rg = new RecommendationSystemChallengeGeneration(
                null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void valuatorNullConfigTest() throws IOException {
        @SuppressWarnings("unused")
        RecommendationSystemChallengeValuator rv = new RecommendationSystemChallengeValuator(
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void valuatorSortConfigTest() throws IOException {
        @SuppressWarnings("unused")
        RecommendationSystemChallengeFilteringAndSorting rs = new RecommendationSystemChallengeFilteringAndSorting(
                null);
    }

    @Test
    public void generatorContentEmptyTest() throws IOException {

        List<Player> input = new ArrayList<Player>();
        Map<String, List<ChallengeDataDTO>> result = rcg.generateAll(input);

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
        List<Player> gameData = new ArrayList<Player>();

        rcg.generateAll(gameData);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDateStartEndMustBeDifferent() {
        List<Player> gameData = new ArrayList<Player>();

        rcg.generateAll(gameData);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDateStartMustBeBeforeEnd() {
        List<Player> gameData = new ArrayList<Player>();

        rcg.generateAll(gameData);
    }

    @Test
    public void testUseFilteringAndFilterIds() {
        assertTrue(ChallengesConfig.isUserfiltering()
                && !ChallengesConfig.getPlayerIds().isEmpty());
    }



}
