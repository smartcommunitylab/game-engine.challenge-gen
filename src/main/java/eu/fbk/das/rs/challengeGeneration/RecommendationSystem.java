package eu.fbk.das.rs.challengeGeneration;

import static eu.trentorise.challenge.PropertiesUtil.CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.GAMEID;
import static eu.trentorise.challenge.PropertiesUtil.HOST;
import static eu.trentorise.challenge.PropertiesUtil.PASSWORD;
import static eu.trentorise.challenge.PropertiesUtil.USERNAME;
import static eu.trentorise.challenge.PropertiesUtil.get;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.fbk.das.rs.sortfilter.LeaderboardPosition;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.PointConcept;
import eu.trentorise.game.challenges.util.ExcelUtil;

/**
 * Recommandation System main class, requires running Gamification Engine in
 * order to run
 */
public class RecommendationSystem {

	private GamificationEngineRestFacade facade;
	private RecommendationSystemConfig configuration;
	private Map<String, List<ChallengeDataDTO>> toWriteChallenge = new HashMap<String, List<ChallengeDataDTO>>();

	public RecommendationSystem() {
		facade = new GamificationEngineRestFacade(get(HOST) + get(CONTEXT),
				get(USERNAME), get(PASSWORD));
		configuration = new RecommendationSystemConfig();
	}

	/**
	 * Generate challenges using {@link RecommendationSystemChallengeGeneration}
	 * then {@link RecommendationSystemChallengeValuator} and
	 * {@link RecommendationSystemChallengeFilteringAndSorting} modules
	 * 
	 * @return a {@link Map} of generated challenges, where key is playerId and
	 *         value is a {@link List} of {@link ChallengeDataDTO}
	 * @throws NullPointerException
	 *             when data from gamification engine is null
	 */
	public Map<String, List<ChallengeDataDTO>> recommendation()
			throws NullPointerException {
		List<Content> gameData = facade.readGameState(get(GAMEID));
		if (gameData == null) {
			throw new NullPointerException(
					"No game data from Gamification Engine");
		}
		List<Content> listofContent = new ArrayList<Content>();
		for (Content c : gameData) {
			if (configuration.isUserfiltering()) {
				if (configuration.getPlayerIds().contains(c.getPlayerId())) {
					listofContent.add(c);
				}
			} else {
				listofContent.add(c);
			}
		}
		RecommendationSystemChallengeGeneration rs = new RecommendationSystemChallengeGeneration(
				configuration);
		Map<String, List<ChallengeDataDTO>> challengeCombinations = rs
				.generate(listofContent);
		RecommendationSystemChallengeValuator valuator = new RecommendationSystemChallengeValuator(
				configuration);
		Map<String, List<ChallengeDataDTO>> evaluatedChallenges = valuator
				.valuate(challengeCombinations, listofContent);

		// build a leaderboard, for now is the current, to be parameterized for
		// weekly or general leaderboard
		List<LeaderboardPosition> leaderboard = buildLeaderBoard(listofContent);
		Collections.sort(leaderboard);
		int index = 0;
		for (LeaderboardPosition pos : leaderboard) {
			pos.setIndex(index);
			index++;
		}

		// filtering
		RecommendationSystemChallengeFilteringAndSorting filtering = new RecommendationSystemChallengeFilteringAndSorting(
				configuration);
		Map<String, List<ChallengeDataDTO>> filteredChallenges = filtering
				.filterAndSort(evaluatedChallenges, leaderboard);

		filteredChallenges = filtering.removeDuplicates(filteredChallenges);

		Map<String, Integer> count = new HashMap<String, Integer>();

		// select challenges and avoid duplicate mode
		for (String key : filteredChallenges.keySet()) {
			// upload and assign challenge
			if (count.get(key) == null) {
				count.put(key, 0);
			}
			if (toWriteChallenge.get(key) == null) {
				toWriteChallenge.put(key, new ArrayList<ChallengeDataDTO>());
			}

			// filter used modes
			List<String> usedModes = new ArrayList<String>();

			for (ChallengeDataDTO dto : filteredChallenges.get(key)) {

				System.out.println("Inserted challenge with Id "
						+ dto.getInstanceName());

				if (configuration.isSelecttoptwo()) {
					if (count.get(key) < 2) {
						String counter = (String) dto.getData().get(
								"counterName");
						if (counter != null && !usedModes.contains(counter)) {
							usedModes.add(counter);
							count.put(key, count.get(key) + 1);
							toWriteChallenge.get(key).add(dto);
						}
					} else {
						break;
					}
				} else {
					toWriteChallenge.get(key).add(dto);
				}
			}
		}
		return toWriteChallenge;
	}

	/**
	 * Build game leaderboard using players green leaves's points
	 * 
	 * @param gameData
	 * @return
	 */
	private List<LeaderboardPosition> buildLeaderBoard(List<Content> gameData) {
		List<LeaderboardPosition> result = new ArrayList<LeaderboardPosition>();
		for (Content content : gameData) {
			for (PointConcept pc : content.getState().getPointConcept()) {
				if (pc.getName().equals("green leaves")) {
					Integer score = (int) Math.round(pc
							.getPeriodCurrentScore("weekly"));
					result.add(new LeaderboardPosition(score, content
							.getPlayerId()));
				}
			}
		}
		return result;
	}

	/**
	 * Write challenges to xlsx (Excel) file and configuration as json file
	 * 
	 * @param toWriteChallenge
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void writeToFile(Map<String, List<ChallengeDataDTO>> toWriteChallenge)
			throws FileNotFoundException, IOException {
		// Get the workbook instance for XLS file
		Workbook workbook = new XSSFWorkbook();

		// Get first sheet from the workbook
		Sheet sheet = workbook.createSheet();
		String[] labels = { "PLAYER_ID", "CHALLENGE_TYPE_NAME",
				"CHALLENGE_NAME", "MODE", "MODE_WEIGHT", "DIFFICULTY", "WI",
				"BONUS_SCORE", "BASELINE", "TARGET", "PERCENTAGE" };

		Row header = sheet.createRow(0);
		int i = 0;
		for (String label : labels) {
			header.createCell(i).setCellValue(label);
			i++;
		}
		int rowIndex = 1;

		for (String key : toWriteChallenge.keySet()) {
			for (ChallengeDataDTO dto : toWriteChallenge.get(key)) {
				Row row = sheet.createRow(rowIndex);
				sheet = ExcelUtil.buildRow(configuration, sheet, row, key, dto);
				rowIndex++;
			}
		}

		workbook.write(new FileOutputStream(new File(
				"reportGeneratedChallenges.xlsx")));
		workbook.close();

		// print configuration
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

		FileOutputStream oout;

		try {
			oout = new FileOutputStream(new File(
					"recommendationSystemConfiguration.json"));
			IOUtils.write(mapper.writeValueAsString(configuration), oout);

			oout.flush();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
