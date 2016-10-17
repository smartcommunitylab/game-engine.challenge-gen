package eu.fbk.das.rs;

import static eu.trentorise.challenge.PropertiesUtil.CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.GAMEID;
import static eu.trentorise.challenge.PropertiesUtil.HOST;
import static eu.trentorise.challenge.PropertiesUtil.PASSWORD;
import static eu.trentorise.challenge.PropertiesUtil.USERNAME;
import static eu.trentorise.challenge.PropertiesUtil.get;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import eu.fbk.das.rs.challengeGeneration.RecommendationSystemChallengeGeneration;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;

public class RecommendationSystemChallengeGenerationTest {

	private GamificationEngineRestFacade facade;

	@Before
	public void setup() {
		facade = new GamificationEngineRestFacade(get(HOST) + get(CONTEXT),
				get(USERNAME), get(PASSWORD));
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
	 * Generate all possible combinations of challenge for every player in the game
	 */
	public void challengeGeneration() {
		assertTrue(facade != null);
		List<Content> gameData = facade.readGameState(get(GAMEID));
		// create all challenges combinations
		RecommendationSystemChallengeGeneration rs = new RecommendationSystemChallengeGeneration();
		Map<String, List<ChallengeDataDTO>> challengeCombinations = rs
				.generate(gameData);

		assertTrue(challengeCombinations != null);
	}

	@Test
	/**
	 * Evaluate challenges adding difficulty and computing the prize
	 */
	public void challengeValuator() {
		List<Content> gameData = facade.readGameState(get(GAMEID));
		RecommendationSystemChallengeGeneration rs = new RecommendationSystemChallengeGeneration();
		Map<String, List<ChallengeDataDTO>> challengeCombinations = rs
				.generate(gameData);
		// evaluate all challenges
		RecommendationSystemChallengeValuator valuator = new RecommendationSystemChallengeValuator();

		Map<String, List<ChallengeDataDTO>> evaluatedChallenges = valuator
				.valuate(challengeCombinations, gameData);

		assertTrue(evaluatedChallenges != null);
	}

	@Test
	/**
	 * Sort and filter challenges using difficulty and prize
	 */
	public void challengeSortAndFiltering() {
		List<Content> gameData = facade.readGameState(get(GAMEID));
		RecommendationSystemChallengeGeneration rs = new RecommendationSystemChallengeGeneration();
		Map<String, List<ChallengeDataDTO>> challengeCombinations = rs
				.generate(gameData);
		RecommendationSystemChallengeValuator valuator = new RecommendationSystemChallengeValuator();
		Map<String, List<ChallengeDataDTO>> evaluatedChallenges = valuator
				.valuate(challengeCombinations, gameData);

		RecommendationSystemChallengeFilteringAndSorting filtering = new RecommendationSystemChallengeFilteringAndSorting();
		List<ChallengeDataDTO> filteredChallenges = filtering
				.filterAndSort(evaluatedChallenges);

		assertTrue(filteredChallenges != null);
	}
}
