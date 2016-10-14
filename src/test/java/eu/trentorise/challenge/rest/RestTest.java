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
import java.util.Arrays;
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
					+ getScore(content, "green leaves") + ";"
					+ getCustomData(content, false)// +
													// getChalengesStatus(content)
					+ "\n");

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
				result += pc.getPeriodPreviousScore("weekly") + ";";
			} else {
				result += pc.getScore() + ";";
			}
		}
		return result;
	}

	private Double getScore(Content content, String points) {
		for (PointConcept pc : content.getState().getPointConcept()) {
			if (pc.getName().equalsIgnoreCase(points)) {
				return pc.getScore();
			}
		}
		return null;
	}

	private Double getScore(Content content, String points, Long moment) {
		if (content.getPlayerId().equals("23897")) {
			System.out.println();

		}
		for (PointConcept pc : content.getState().getPointConcept()) {
			if (pc.getName().equalsIgnoreCase(points)) {
				return pc.getPeriodScore("weekly", moment);
			}
		}
		return null;
	}

	@Test
	public void challengeStatus() throws FileNotFoundException, IOException {
		// a small utility to get a list of all users with a given challenge in
		// a period and its status
		List<Content> result = facade.readGameState(get(GAMEID));
		assertTrue(!result.isEmpty());

		String[] customNames = get(RELEVANT_CUSTOM_DATA).split(",");
		assertTrue(customNames != null);

		List<String> listNames = Arrays.asList(customNames);
		StringBuffer toWrite = new StringBuffer();

		toWrite.append("PLAYER_ID;CHALLENGE_TYPE;CHALLENGE_END;SUCCESS;\n");
		for (Content content : result) {
			// if (getScore(content, "green leaves week 5") > 0) {
			List<ChallengeTuple> cts = getChallengeWithEndDate(content);
			if (!cts.isEmpty()) {
				for (ChallengeTuple ct : cts) {
					if (ct.getEndDate().contains("18/07/2016 00:00:01")) {
						toWrite.append(content.getPlayerId() + ";"
								+ ct.getType() + ";" + ct.getEndDate() + ";"
								+ getSuccess(ct, content) + ";\n");
					}
				}
			}
			// }
		}
		IOUtils.write(toWrite.toString(), new FileOutputStream(
				"challengeReportstatus.csv"));

		assertTrue(!toWrite.toString().isEmpty());
	}

	private boolean getSuccess(ChallengeTuple ct, Content content) {
		for (String key : content.getCustomData().getAdditionalProperties()
				.keySet()) {
			if (key.equals(ct.getUuid() + "_success")) {
				return (boolean) content.getCustomData()
						.getAdditionalProperties().get(key);
			}
		}
		return false;
	}

	private List<ChallengeTuple> getChallengeWithEndDate(Content content) {
		List<ChallengeTuple> result = new ArrayList<RestTest.ChallengeTuple>();
		for (String key : content.getCustomData().getAdditionalProperties()
				.keySet()) {
			if (key.endsWith("_endChTs")) {
				ChallengeTuple ct = new ChallengeTuple();
				ct.setUuid(StringUtils.removeEnd(key, "_endChTs"));
				ct.setEndDate(sdf.format(content.getCustomData()
						.getAdditionalProperties().get(key)));
				ct.setType((String) content.getCustomData()
						.getAdditionalProperties().get(ct.getUuid() + "_type"));
				result.add(ct);
			}
		}
		return result;
	}

	private class ChallengeTuple {

		private String uuid;

		private String type;

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public String getEndDate() {
			return endDate;
		}

		public void setEndDate(String endDate) {
			this.endDate = endDate;
		}

		private String endDate;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

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
			if (getScore(user, "green leaves") > 0) {
				for (ChallengeConcept cc : user.getState()
						.getChallengeConcept()) {
					toWrite.append(user.getPlayerId() + ";");
					toWrite.append(cc.getName() + ";");
					toWrite.append(cc.getModelName() + ";");
					toWrite.append(cc.getFields().get(Constants.TARGET) + ";");
					toWrite.append(cc.getFields().get(Constants.BONUS_SCORE)
							+ ";");
					toWrite.append(cc.getFields().get(
							Constants.BONUS_POINT_TYPE)
							+ ";");
					toWrite.append(CalendarUtil.format((Long) cc.getStart())
							+ ";");
					toWrite.append(CalendarUtil.format((Long) cc.getEnd())
							+ ";");
					toWrite.append(cc.getCompleted() + ";");
					toWrite.append(CalendarUtil.format(cc.getDateCompleted())
							+ ";");
					toWrite.append(cc.getFields().get(Constants.BASELINE) + ";");
					toWrite.append(cc.getFields().get(Constants.PERIOD_NAME)
							+ ";");
					toWrite.append(cc.getFields().get(Constants.COUNTER_NAME)
							+ ";");
					toWrite.append(getScore(
							user,
							(String) cc.getFields().get(Constants.COUNTER_NAME),
							cc.getStart())
							+ ";\n");

				}
			}
		}

		String writable = toWrite.toString();
		writable = StringUtils.replace(writable, "null", "");

		IOUtils.write(writable, new FileOutputStream(
				"challengeReportDetails.csv"));
		logger.info("challengeReportDetails.csv written");
		assertTrue(!writable.isEmpty());
	}

}
