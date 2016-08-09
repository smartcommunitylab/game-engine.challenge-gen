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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.ExecutionDataDTO;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.InsertedRuleDto;
import eu.trentorise.game.challenges.rest.PointConcept;
import eu.trentorise.game.challenges.util.ConverterUtil;
import eu.trentorise.game.challenges.util.JourneyData;

public class RestTest {

	private GamificationEngineRestFacade facade;
	private GamificationEngineRestFacade insertFacade;
	private SimpleDateFormat sdf = new SimpleDateFormat(
			"dd/MM/YYYY HH:mm:ss, zzz ZZ");

	private final String[] COLUMNS = new String[] { "success", "startChTs",
			"point_type", "type", "endChTs", "counter", "target", "bonus",
			"mode", "recommendations_sent_during_challenges",
			"Km_traveled_during_challenge" };

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
		@SuppressWarnings("unchecked")
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

		String[] customNames = get(RELEVANT_CUSTOM_DATA).split(",");
		assertTrue(customNames != null);

		List<String> listNames = Arrays.asList(customNames);
		StringBuffer toWrite = new StringBuffer();

		toWrite.append("PLAYER_ID;SCORE_GREEN_LEAVES;"
				+ StringUtils.join(customNames, ";")
				+ ";CHALLENGES_SUCCESS;CHALLENGES_TOTAL\n");
		for (Content content : result) {
			toWrite.append(content.getPlayerId() + ";"
					+ getScore(content, "green leaves") + ";"
					+ getCustomData(content, listNames)
					+ getChalengesStatus(content) + "\n");

		}
		IOUtils.write(toWrite.toString(),
				new FileOutputStream("gameStatus.csv"));

		assertTrue(!toWrite.toString().isEmpty());
	}

	private String getCustomData(Content content, List<String> listNames) {
		String result = "";
		Iterator<String> iter = listNames.iterator();
		while (iter.hasNext()) {
			String name = iter.next();
			result += content.getCustomData().getAdditionalProperties()
					.get(name);
			if (listNames.iterator().hasNext()) {
				result += ";";
			}
		}
		return result;
	}

	private String getChalengesStatus(Content content) {
		int s = 0;
		int t = 0;
		if (content.getCustomData() != null
				&& content.getCustomData().getAdditionalProperties() != null) {
			for (String k : content.getCustomData().getAdditionalProperties()
					.keySet()) {
				Object v = content.getCustomData().getAdditionalProperties()
						.get(k);
				if (v != null) {
					if (((String) k).endsWith("_success")) {
						boolean c = Boolean.valueOf(v.toString());
						t++;
						if (c) {
							s++;
						}
					}
				}
			}
		}
		return s + ";" + t;
	}

	private Double getScore(Content content, String points) {
		for (PointConcept pc : content.getState().getPointConcept()) {
			if (pc.getName().equalsIgnoreCase(points)) {
				return pc.getScore();
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
		toWrite.append("PLAYER_ID;CHALLENGE_UUID;"
				+ StringUtils.join(COLUMNS, ";") + "\n");
		List<WeekChallenge> challenges = new ArrayList<WeekChallenge>();
		for (Content content : result) {
			if (content.getCustomData() != null
					&& content.getCustomData().getAdditionalProperties() != null
					&& !content.getCustomData().getAdditionalProperties()
							.isEmpty()) {
				Map<String, Object> data = content.getCustomData()
						.getAdditionalProperties();
				Set<String> keys = data.keySet();
				for (String key : keys) {
					if (key.startsWith("ch_")) {
						// try to fix error on data
						if (key.endsWith("point_type_baseline")) {
							if (StringUtils.countMatches(key, "_") == 3) {
								key = StringUtils.replace(key,
										"point_type_baseline",
										"_point_type_baseline");
							}
						}
						StringTokenizer stk = new StringTokenizer(key, "_");
						String k = "";
						try {
							k = stk.nextToken();
							k = stk.nextToken();
							WeekChallenge c = null;
							if (!containChallengeWithId(challenges, k)) {
								c = new WeekChallenge();
								if (k.endsWith("point")) {
									k = StringUtils.removeEnd(k, "point");
								}

								c.setId(k);
								c.setData(new HashMap<String, String>());
								c.getPlayerId().add(content.getPlayerId());
								challenges.add(c);
							} else {
								c = findChallegeWithId(challenges, k);
							}
							if (!c.getData().containsKey(key)) {
								c.getData().put(key,
										String.valueOf(data.get(key)));
								if (!c.getPlayerId().contains(
										content.getPlayerId())) {
									c.getPlayerId().add(content.getPlayerId());
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		for (WeekChallenge wc : challenges) {
			toWrite.append(wc.getPlayerId().get(0) + ";" + wc.getId() + ";"
					+ getCustomDataFromColumn(wc, COLUMNS) + ";\n");
		}

		IOUtils.write(toWrite.toString(), new FileOutputStream(
				"challengeReportDetails.csv"));

		assertTrue(!toWrite.toString().isEmpty());
	}

	private WeekChallenge findChallegeWithId(List<WeekChallenge> challenges,
			String k) {
		for (WeekChallenge wc : challenges) {
			if (wc.getId().equals(k)) {
				return wc;
			}
		}
		return null;
	}

	private boolean containChallengeWithId(List<WeekChallenge> challenges,
			String k) {
		for (WeekChallenge ch : challenges) {
			if (ch.getId().equals(k)) {
				return true;
			}
		}
		return false;
	}

	private String getCustomDataFromColumn(WeekChallenge wc, String[] cls) {
		Map<String, Boolean> used = new HashMap<String, Boolean>();
		StringBuffer sb = new StringBuffer();
		for (String c : cls) {

			if (!keySetContains(wc.getData().keySet(), c)) {
				sb.append(";");
			} else {
				for (String key : wc.getData().keySet()) {
					if (key.endsWith(c)
							&& (used.get(key) == null || used.get(key) != null
									&& !used.get(key))) {
						if (c.equals("type") && key.endsWith("point_type")) {
							sb.append(";");
							break;
						}
						sb.append(wc.getData().get(key)).append(";");
						used.put(key, true);
						break;
					}
				}
			}
		}
		return sb.toString();
	}

	private boolean keySetContains(Set<String> keySet, String c) {
		for (String k : keySet) {
			if (k.endsWith(c)) {
				return true;
			}
		}
		return false;
	}

	private class WeekChallenge {

		private String id;
		private Map<String, String> data = new HashMap<String, String>();
		private List<String> playerId = new ArrayList<String>();

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Map<String, String> getData() {
			return data;
		}

		public void setData(Map<String, String> data) {
			this.data = data;
		}

		public List<String> getPlayerId() {
			return playerId;
		}

		public void setPlayerId(List<String> playerId) {
			this.playerId = playerId;
		}

	}

}
