package eu.fbk.das.rs;

import static eu.trentorise.challenge.PropertiesUtil.CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.GAMEID;
import static eu.trentorise.challenge.PropertiesUtil.HOST;
import static eu.trentorise.challenge.PropertiesUtil.PASSWORD;
import static eu.trentorise.challenge.PropertiesUtil.USERNAME;
import static eu.trentorise.challenge.PropertiesUtil.get;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import eu.fbk.das.rs.challengeGeneration.RecommendationSystem;
import eu.fbk.das.rs.challengeGeneration.RecommendationSystemChallengeGeneration;
import eu.fbk.das.rs.challengeGeneration.RecommendationSystemConfig;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.valuator.DifficultyCalculator;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.trentorise.challenge.PropertiesUtil;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;

public class RecommendationSystemChallengeGenerationTest {

	private GamificationEngineRestFacade facade;
	private RecommendationSystemConfig configuration;

	@Before
	public void setup() {
		facade = new GamificationEngineRestFacade(get(HOST) + get(CONTEXT),
				get(USERNAME), get(PASSWORD));
		configuration = new RecommendationSystemConfig(
				get(PropertiesUtil.FILTERING));
	}

	@Test
	/**
	 * Read game status from gamification engine
	 */
	public void readGameTest() {
		assertTrue(facade != null);
		// read data from gamification engine
		List<Content> gameData = facade.readGameState(get(GAMEID));

		assertTrue(!gameData.isEmpty());
	}

	@Test
	/**
	 * Generate all possible combinations of challenge for every player in the
	 * game
	 */
	public void challengeGeneration() {
		assertTrue(facade != null);
		List<Content> gameData = facade.readGameState(get(GAMEID));
		// create all challenges combinations
		RecommendationSystemChallengeGeneration rs = new RecommendationSystemChallengeGeneration(
				configuration);
		Map<String, List<ChallengeDataDTO>> challengeCombinations = rs
				.generate(gameData);

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
		List<Content> gameData = facade.readGameState(get(GAMEID));
		RecommendationSystemChallengeGeneration rs = new RecommendationSystemChallengeGeneration(
				configuration);
		Map<String, List<ChallengeDataDTO>> challengeCombinations = rs
				.generate(gameData);
		// evaluate all challenges
		RecommendationSystemChallengeValuator valuator = new RecommendationSystemChallengeValuator(
				configuration);

		Map<String, List<ChallengeDataDTO>> evaluatedChallenges = valuator
				.valuate(challengeCombinations, gameData);
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
		RecommendationSystemConfig rc = new RecommendationSystemConfig(
				get(PropertiesUtil.FILTERING));
		assertTrue(!rc.isUserfiltering()
				|| (rc.isUserfiltering() && rc.getPlayerIds().size() > 2));
	}

	@Test
	/**
	 * Sort and filter challenges using difficulty and prize
	 */
	public void recommendationSystemTest() throws IOException {
		RecommendationSystem rs = new RecommendationSystem(configuration);
		Map<String, List<ChallengeDataDTO>> result = rs.recommendation(
				get(HOST), get(CONTEXT), get(USERNAME), get(PASSWORD),
				get(GAMEID));
		rs.writeToFile(result);
		assertTrue(!result.isEmpty());
	}

	@Test(expected = IllegalArgumentException.class)
	public void recommendationSystemNullGameData() throws IOException {
		RecommendationSystem rs = new RecommendationSystem(configuration);
		rs.recommendation(null);
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
				null);
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
		RecommendationSystemChallengeGeneration rg = new RecommendationSystemChallengeGeneration(
				configuration);
		List<Content> input = new ArrayList<Content>();
		Map<String, List<ChallengeDataDTO>> result = rg.generate(input);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testConfigIsModeNull() throws IOException {
		assertTrue(!configuration.isDefaultMode(null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConfigFilteringIdsNull() {
		@SuppressWarnings("unused")
		RecommendationSystemConfig config = new RecommendationSystemConfig(null);
	}

}
