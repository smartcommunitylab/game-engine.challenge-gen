package eu.trentorise.challenge.rest;

import static eu.trentorise.challenge.PropertiesUtil.CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.GAMEID;
import static eu.trentorise.challenge.PropertiesUtil.HOST;
import static eu.trentorise.challenge.PropertiesUtil.INSERT_CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.PASSWORD;
import static eu.trentorise.challenge.PropertiesUtil.RELEVANT_CUSTOM_DATA;
import static eu.trentorise.challenge.PropertiesUtil.SAVE_ITINERARY;
import static eu.trentorise.challenge.PropertiesUtil.USERNAME;
import static eu.trentorise.challenge.PropertiesUtil.get;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.rest.ChallengeConcept;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.ExecutionDataDTO;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.InsertedRuleDto;
import eu.trentorise.game.challenges.rest.PointConcept;
import eu.trentorise.game.challenges.util.CalendarUtil;
import eu.trentorise.game.challenges.util.ConverterUtil;
import eu.trentorise.game.challenges.util.JourneyData;

public class RestTest {

	private static final Logger logger = LogManager.getLogger(RestTest.class);

	private GamificationEngineRestFacade facade;
	private GamificationEngineRestFacade insertFacade;
	private SimpleDateFormat sdf = new SimpleDateFormat(
			"dd/MM/YYYY HH:mm:ss, zzz ZZ");

	@Before
	public void setup() {
		facade = new GamificationEngineRestFacade(get(HOST) + get(CONTEXT),
				get(USERNAME), get(PASSWORD));
		insertFacade = new GamificationEngineRestFacade(get(HOST)
				+ get(INSERT_CONTEXT), get(USERNAME), get(PASSWORD));
	}

	@Test
	public void gameReadGameStateTest() {
		List<Content> result = facade.readGameState(get(GAMEID));
		assertTrue(!result.isEmpty());
	}

	@Test
	public void gameInsertRuleTest() {
		// define rule
		InsertedRuleDto rule = new InsertedRuleDto();
		rule.setContent("/* */");
		rule.setName("sampleRule");
		// insert rule
		InsertedRuleDto result = insertFacade.insertGameRule(get(GAMEID), rule);
		assertTrue(!result.getId().isEmpty());
		// delete inserted rule
		boolean res = insertFacade.deleteGameRule(get(GAMEID), result.getId());
		assertTrue(res);
	}

	@Test
	public void saveItineraryTest() {
		ExecutionDataDTO input = new ExecutionDataDTO();
		input.setActionId(get(SAVE_ITINERARY));
		input.setPlayerId("1");
		input.setGameId(get(GAMEID));
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("bikeDistance", Double.valueOf(1.0d));
		input.setData(data);
		boolean result = facade.saveItinerary(input);

		assertTrue(result);
	}

	@Test
	public void saveUsersItineraryLoadedFromFile() throws IOException {
		// create input
		String ref = "178-bus.json";

		// read all lines from file
		List<String> lines = IOUtils.readLines(Thread.currentThread()
				.getContextClassLoader().getResourceAsStream(ref));

		for (String line : lines) {
			JourneyData jd = ConverterUtil.extractJourneyData(line);
			ExecutionDataDTO input = new ExecutionDataDTO();
			input.setActionId(get(SAVE_ITINERARY));
			input.setPlayerId(jd.getUserId());
			input.setGameId(get(GAMEID));
			input.setData(jd.getData());
			boolean result = facade.saveItinerary(input);

			assertTrue(result);
		}
	}

	@Test
	public void printGameStatus() throws FileNotFoundException, IOException {
		List<Content> result = facade.readGameState(get(GAMEID));
		assertTrue(!result.isEmpty());

		String customNames = get(RELEVANT_CUSTOM_DATA);
		assertTrue(customNames != null);

		StringBuffer toWrite = new StringBuffer();

		toWrite.append("PLAYER_ID;SCORE_GREEN_LEAVES;" + customNames + "\n");
		for (Content content : result) {
			toWrite.append(content.getPlayerId() + ";"
					+ getScore(content, "green leaves", false, false) + ";" // false, false = current week counter
					+ getCustomData(content, true) + "\n");

		}
		IOUtils.write(toWrite.toString(),
				new FileOutputStream("gameStatus.csv"));

		assertTrue(!toWrite.toString().isEmpty());
	}

	private String getCustomData(Content content, boolean weekly) {
		String result = "";
		List<PointConcept> concepts = content.getState().getPointConcept();
		Collections.sort(concepts, new Comparator<PointConcept>() {
			@Override
			public int compare(PointConcept o1, PointConcept o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		Iterator<PointConcept> iter = concepts.iterator();
		while (iter.hasNext()) {
			PointConcept pc = iter.next();
			if (weekly) {
				// result += pc.getPeriodPreviousScore("weekly") + ";";
				result += pc.getPeriodCurrentScore("weekly") + ";";
			} else {
				result += pc.getScore() + ";";
			}
		}
		return result;
	}

	private Double getScore(Content content, String points, boolean previous,
			boolean total) {
		for (PointConcept pc : content.getState().getPointConcept()) {
			if (pc.getName().equalsIgnoreCase(points)) {
				if (total) {
					return pc.getScore();
				}
				if (previous) {
					return pc.getPeriodPreviousScore("weekly");
				}
				return pc.getPeriodCurrentScore("weekly");
			}
		}
		return null;
	}

	private Double getScore(Content content, String points, Long moment) {
		for (PointConcept pc : content.getState().getPointConcept()) {
			if (pc.getName().equalsIgnoreCase(points)) {
				return pc.getPeriodScore("weekly", moment);
			}
		}
		return null;
	}

	@Test
	public void challengeReportDetails() throws FileNotFoundException,
			IOException {
		// a small utility to get a list of all users with a given challenge in
		// a period and its status
		List<Content> result = facade.readGameState(get(GAMEID));
		assertTrue(!result.isEmpty());

		StringBuffer toWrite = new StringBuffer();

		// build weeks details
		toWrite.append("PLAYER_ID;CHALLENGE_UUID;MODEL_NAME;TARGET;BONUS_SCORE;BONUS_POINT_TYPE;START;END;COMPLETED;DATE_COMPLETED;BASELINE;PERIOD_NAME;COUNTER_NAME;COUNTER_VALUE"
				+ "\n");

		for (Content user : result) {
			// if (getScore(user, "green leaves", false, true) > 0) {
			for (ChallengeConcept cc : user.getState().getChallengeConcept()) {
				toWrite.append(user.getPlayerId() + ";");
				toWrite.append(cc.getName() + ";");
				toWrite.append(cc.getModelName() + ";");
				toWrite.append(cc.getFields().get(Constants.TARGET) + ";");
				toWrite.append(cc.getFields().get(Constants.BONUS_SCORE) + ";");
				toWrite.append(cc.getFields().get(Constants.BONUS_POINT_TYPE)
						+ ";");
				toWrite.append(CalendarUtil.format((Long) cc.getStart()) + ";");
				toWrite.append(CalendarUtil.format((Long) cc.getEnd()) + ";");
				toWrite.append(cc.getCompleted() + ";");
				toWrite.append(CalendarUtil.format(cc.getDateCompleted()) + ";");
				toWrite.append(cc.getFields().get(Constants.BASELINE) + ";");
				toWrite.append(cc.getFields().get(Constants.PERIOD_NAME) + ";");
				toWrite.append(cc.getFields().get(Constants.COUNTER_NAME) + ";");
				toWrite.append(getScore(user,
						(String) cc.getFields().get(Constants.COUNTER_NAME),
						cc.getStart())
						+ ";\n");
			}
			// }
		}

		String writable = toWrite.toString();
		writable = StringUtils.replace(writable, "null", "");

		IOUtils.write(writable, new FileOutputStream(
				"challengeReportDetails.csv"));
		logger.info("challengeReportDetails.csv written");
		assertTrue(!writable.isEmpty());
	}

	@Test
	public void printPoints() throws FileNotFoundException, IOException {
		// a small utility to get a list of all users with a given challenge in
		// a period and its status
		List<Content> result = facade.readGameState(get(GAMEID));
		assertTrue(!result.isEmpty());

		// week11 playerIds
		String playerIds = "24813,24538,17741,24816,24498,24871,24279,24612,24853,24339,24391,24150,24650,11125,24092,24869,24329,24826,24828,24762,24883,24288,24486,24566,24224,24741,24367,19092,24864,24347,24823,23513,24526,24327,1667,24120,24482,24320,24122,24440";
		List<String> ids = new ArrayList<String>();
		Collections.addAll(ids, playerIds.split(","));

		System.out.println("PLAYER_ID;TOTAL_SCORE");
		for (Content user : result) {
			if (ids.contains(user.getPlayerId())) {
				for (PointConcept pc : user.getState().getPointConcept()) {
					if (pc.getName().equals("green leaves")) {
						System.out.println(user.getPlayerId() + ";"
								+ pc.getScore());
					}
				}
			}
		}
	}

}
