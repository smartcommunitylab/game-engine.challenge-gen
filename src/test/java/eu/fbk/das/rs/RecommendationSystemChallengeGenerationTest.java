package eu.fbk.das.rs;

import static eu.trentorise.challenge.PropertiesUtil.CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.GAMEID;
import static eu.trentorise.challenge.PropertiesUtil.HOST;
import static eu.trentorise.challenge.PropertiesUtil.PASSWORD;
import static eu.trentorise.challenge.PropertiesUtil.USERNAME;
import static eu.trentorise.challenge.PropertiesUtil.get;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.fbk.das.rs.challengeGeneration.RecommendationSystemChallengeGeneration;
import eu.fbk.das.rs.sortfilter.LeaderboardPosition;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.valuator.DifficultyCalculator;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.PointConcept;

public class RecommendationSystemChallengeGenerationTest {

	private GamificationEngineRestFacade facade;

	@Before
	public void setup() {
		facade = new GamificationEngineRestFacade(get(HOST) + get(CONTEXT), get(USERNAME), get(PASSWORD));
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
		RecommendationSystemChallengeGeneration rs = new RecommendationSystemChallengeGeneration();
		Map<String, List<ChallengeDataDTO>> challengeCombinations = rs.generate(gameData);

		assertTrue(challengeCombinations != null);
	}

	@Test
	/**
	 * Evaluate challenges adding difficulty and computing the prize
	 */
	public void challengeValuator() {
		List<Content> gameData = facade.readGameState(get(GAMEID));
		RecommendationSystemChallengeGeneration rs = new RecommendationSystemChallengeGeneration();
		Map<String, List<ChallengeDataDTO>> challengeCombinations = rs.generate(gameData);
		// evaluate all challenges
		RecommendationSystemChallengeValuator valuator = new RecommendationSystemChallengeValuator();

		Map<String, List<ChallengeDataDTO>> evaluatedChallenges = valuator.valuate(challengeCombinations, gameData);

		assertTrue(evaluatedChallenges != null);
	}

	@Test
	/**
	 * Sort and filter challenges using difficulty and prize
	 */
	public void challengeSortAndFiltering() {
		List<Content> gameData = facade.readGameState(get(GAMEID));
		RecommendationSystemChallengeGeneration rs = new RecommendationSystemChallengeGeneration();
		Map<String, List<ChallengeDataDTO>> challengeCombinations = rs.generate(gameData);
		RecommendationSystemChallengeValuator valuator = new RecommendationSystemChallengeValuator();
		Map<String, List<ChallengeDataDTO>> evaluatedChallenges = valuator.valuate(challengeCombinations, gameData);

		// build a leaderboard, for now is the current, to be parameterized for
		// weekly or general leaderboard
		List<LeaderboardPosition> leaderboard = buildLeaderBoard(gameData);
		Collections.sort(leaderboard);
		int index = 0;
		for (LeaderboardPosition pos : leaderboard) {
			pos.setIndex(index);
			index++;
		}

		RecommendationSystemChallengeFilteringAndSorting filtering = new RecommendationSystemChallengeFilteringAndSorting();
		Map<String, List<ChallengeDataDTO>> filteredChallenges = filtering.filterAndSort(evaluatedChallenges,
				leaderboard);

		assertTrue(filteredChallenges != null);

		ObjectMapper mapper = new ObjectMapper();
		FileOutputStream oout;
		try {
			oout = new FileOutputStream(new File("/Users/rezakhoshkangini/Documents/generatedChallengesRs.json"));
			IOUtils.write(mapper.writeValueAsString(filteredChallenges), oout);
			oout.flush();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private List<LeaderboardPosition> buildLeaderBoard(List<Content> gameData) {
		List<LeaderboardPosition> result = new ArrayList<LeaderboardPosition>();
		for (Content content : gameData) {
			for (PointConcept pc : content.getState().getPointConcept()) {
				if (pc.getName().equals("green leaves")) {
					Integer score = (int) Math.round(pc.getPeriodCurrentScore("weekly"));
					result.add(new LeaderboardPosition(score, content.getPlayerId()));
				}
			}
		}
		return result;
	}

	@Test
	public void testDifficulty() {
		Map<Integer, Double> quartiles = new HashMap<Integer, Double>();
		quartiles.put(4, 3.99);
		quartiles.put(7, 12.516551);
		quartiles.put(9, 30.51);
		Integer zone = 1;
		Double baseline = 1.2;
		Double target = 2.43;
		Integer difficulty = DifficultyCalculator.computeDifficulty(quartiles, zone, baseline, target);

		assert (difficulty == DifficultyCalculator.EASY);

		// .... we can check more cases..
	}

}
