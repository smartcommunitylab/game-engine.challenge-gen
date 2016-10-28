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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.fbk.das.rs.challengeGeneration.RecommendationSystemChallengeGeneration;
import eu.fbk.das.rs.challengeGeneration.RecommendationSystemConfig;
import eu.fbk.das.rs.sortfilter.LeaderboardPosition;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.valuator.DifficultyCalculator;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.PointConcept;

public class RecommendationSystemChallengeGenerationTest {

	private static final boolean test = false;
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
		List<Content> listofContent = new ArrayList<Content>();
		for (Content c : gameData) {
			if (RecommendationSystemConfig.getPlayerIds().contains(c.getPlayerId())) {
				listofContent.add(c);
			}
		}

		RecommendationSystemChallengeGeneration rs = new RecommendationSystemChallengeGeneration();
		Map<String, List<ChallengeDataDTO>> challengeCombinations = rs.generate(listofContent);
		RecommendationSystemChallengeValuator valuator = new RecommendationSystemChallengeValuator();
		Map<String, List<ChallengeDataDTO>> evaluatedChallenges = valuator.valuate(challengeCombinations,
				listofContent);

		// build a leaderboard, for now is the current, to be parameterized for
		// weekly or general leaderboard
		List<LeaderboardPosition> leaderboard = buildLeaderBoard(listofContent);
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

		// just for test for user 23897 (Alberto)
		// Iterator<Content> iter = gameData.iterator();
		// while (iter.hasNext()) {
		// Content p = iter.next();
		// if (p.getPlayerId().equals("23897")) {
		// System.out.println();
		// }
		// }

		// filtering

		// remove duplicates

		List<ChallengeDataDTO> challengeIdToRemove = new ArrayList<ChallengeDataDTO>();
		for (String key : filteredChallenges.keySet()) { // upload and assign
															// challenge
			Iterator<ChallengeDataDTO> iter = filteredChallenges.get(key).iterator();
			while (iter.hasNext()) {
				ChallengeDataDTO dto = iter.next();
				Iterator<ChallengeDataDTO> innerIter = filteredChallenges.get(key).iterator();
				int count = 0;
				System.out.println("current counter: " + dto.getData().get("counterName"));
				if (dto.getData().get("counterName").equals("Walk_Trips")) {
					System.out.println();
				}
				while (innerIter.hasNext()) {
					ChallengeDataDTO idto = innerIter.next();

					if (dto.getModelName().equals(idto.getModelName())
							&& dto.getData().get("counterName").equals(idto.getData().get("counterName"))) {
						double t = 0;
						double ti = 0;
						if (dto.getData().get("target") instanceof Double) {
							t = (Double) dto.getData().get("target");
						} else {
							t = (Integer) dto.getData().get("target");
						}
						if (idto.getData().get("target") instanceof Double) {
							ti = (Double) idto.getData().get("target");
						} else {
							ti = (Integer) idto.getData().get("target");
						}
						if (t == ti) {
							count++;
						}
					}
					if (count > 1) {
						System.out.println();

						challengeIdToRemove.add(idto);
						count = 1;
					}
				}

			}
			filteredChallenges.get(key).removeAll(challengeIdToRemove);
			challengeIdToRemove.clear();
		}

		// remove duplicates ^

		// select K-top challenges

		// List<ChallengeDataDTO> KTopChallenge = new
		// ArrayList<ChallengeDataDTO>();

		// for (String key : filteredChallenges.keySet()) {

		// }

		// select K-top challenges

		// printing out stuff, just for debug

		ObjectMapper mapper = new ObjectMapper();
		FileOutputStream oout;

		try {
			oout = new FileOutputStream(new File("generatedChallenges.json"));
			IOUtils.write(mapper.writeValueAsString(filteredChallenges), oout);

			oout.flush();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Converting to CSV file
		String log = "";
		String msg = "";

		// StringWriter OutputCsv=new StringWriter
		StringBuffer buffer = new StringBuffer();
		buffer.append("PLAYER_ID;" + "CHALLENGE_TYPE_NAME;" + "CHALLENGE_NAME;"
				+ "MODE;MODE_WEIGHT;DIFFICULTY;WI;BONUS_SCORE;BASELINE;TARGET;TOP_TEN;\n");

		// get only top 10 challenges for user
		Map<String, Integer> count = new HashMap<String, Integer>();

		for (String key : filteredChallenges.keySet()) {
			// upload and assign challenge
			if (count.get(key) == null) {
				count.put(key, 0);
			}
			for (ChallengeDataDTO dto : filteredChallenges.get(key)) {

				System.out.println("Inserted challenge with Id " + dto.getInstanceName());

				if (count.get(key) < 10) {
					count.put(key, count.get(key) + 1);
					buffer = buildBuffer(buffer, key, dto, true);

				} else {
					buffer = buildBuffer(buffer, key, dto, false);

				}
			}
			// buffer = buildBuffer(buffer, key,
			// filteredChallenges.get(key).get(filteredChallenges.get(key).size()
			// - 2));
			// buffer = buildBuffer(buffer, key,
			// filteredChallenges.get(key).get(filteredChallenges.get(key).size()
			// - 1));

		}

		try {
			FileOutputStream out = new FileOutputStream("reportGeneratedChallenges.csv");
			IOUtils.write(buffer.toString(), out);
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			System.err.println("Error in writing the report");
		}
		System.out.println(msg);

		// Converting to CSV file
	}

	private StringBuffer buildBuffer(StringBuffer buffer, String key, ChallengeDataDTO dto, boolean flag) {
		buffer.append(key + ";");
		buffer.append(dto.getModelName() + ";");
		buffer.append(dto.getInstanceName() + ";");
		buffer.append(dto.getData().get("counterName") + ";");
		buffer.append(RecommendationSystemConfig.getWeight(getMode((String) dto.getData().get("counterName"))) + ";");
		buffer.append(dto.getData().get("difficulty") + ";");
		buffer.append(dto.getData().get("wi") + ";");
		buffer.append(dto.getData().get("bonusScore") + ";");
		buffer.append(dto.getData().get("baseline") + ";");
		buffer.append(dto.getData().get("target") + ";");
		buffer.append(flag + ";\n");

		return buffer;
	}

	private String getMode(String mode) {
		if (mode == null) {
			return "";
		}
		String[] t = mode.split("_");
		if (t == null) {
			return "";
		}
		return t[0];
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
		Double baseline = 1.2;
		Double target = 60.43;
		Integer difficulty = DifficultyCalculator.computeDifficulty(quartiles, baseline, target);
		System.out.println("Difficulty: " + difficulty);
		assertTrue(difficulty == DifficultyCalculator.MEDIUM);

		// .... we can check more cases..
	}

}
