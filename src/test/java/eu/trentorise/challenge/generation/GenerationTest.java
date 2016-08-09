package eu.trentorise.challenge.generation;

import static eu.trentorise.challenge.PropertiesUtil.CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.GAMEID;
import static eu.trentorise.challenge.PropertiesUtil.HOST;
import static eu.trentorise.challenge.PropertiesUtil.INSERT_CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.PASSWORD;
import static eu.trentorise.challenge.PropertiesUtil.USERNAME;
import static eu.trentorise.challenge.PropertiesUtil.get;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import eu.trentorise.game.bean.ChallengeDataDTO;
import eu.trentorise.game.challenges.ChallengeInstanceFactory;
import eu.trentorise.game.challenges.ChallengesRulesGenerator;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.ChallengeDataInternalDto;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.util.ChallengeRuleRow;
import eu.trentorise.game.challenges.util.ChallengeRules;
import eu.trentorise.game.challenges.util.ChallengeRulesLoader;
import eu.trentorise.game.challenges.util.Matcher;

public class GenerationTest {

	private static final Logger logger = LogManager
			.getLogger(GenerationTest.class);

	private GamificationEngineRestFacade facade;
	private GamificationEngineRestFacade insertFacade;
	private GamificationEngineRestFacade challengeModelFacade;
	private GamificationEngineRestFacade challengeAssignFacade;

	@Before
	public void setup() {
		facade = new GamificationEngineRestFacade(get(HOST) + get(CONTEXT),
				get(USERNAME), get(PASSWORD));
		insertFacade = new GamificationEngineRestFacade(get(HOST)
				+ get(INSERT_CONTEXT), get(USERNAME), get(PASSWORD));
		challengeModelFacade = new GamificationEngineRestFacade(get(HOST),
				get(USERNAME), get(PASSWORD));
		challengeAssignFacade = new GamificationEngineRestFacade(get(HOST)
				+ "data/game/", get(USERNAME), get(PASSWORD));
	}

	@Test
	public void loadChallengeRuleGenerate() throws NullPointerException,
			IllegalArgumentException, IOException {
		// load
		ChallengeRules result = ChallengeRulesLoader.load("w1_challenges.csv");

		assertTrue(result != null && !result.getChallenges().isEmpty());

		List<Content> users = facade.readGameState(get(GAMEID));

		// generate challenges
		for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
			Matcher matcher = new Matcher(challengeSpec);
			List<Content> r = matcher.match(users);
		}
	}

	@Test
	public void loadTestGeneration() throws NullPointerException,
			IllegalArgumentException, IOException, UndefinedChallengeException {
		// load
		ChallengeRules result = ChallengeRulesLoader.load("w1_challenges.csv");

		assertTrue(result != null && !result.getChallenges().isEmpty());

		// get users from gamification engine
		List<Content> users = facade.readGameState(get(GAMEID));

		ChallengesRulesGenerator crg = new ChallengesRulesGenerator(
				new ChallengeInstanceFactory(), "generated-rules-report.csv");

		// generate challenges
		for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
			logger.debug("rules generation for challenge: "
					+ challengeSpec.getName());
			Matcher matcher = new Matcher(challengeSpec);
			List<Content> filteredUsers = matcher.match(users);
			logger.debug("found users: " + filteredUsers.size());
			// generate rule
			if (!filteredUsers.isEmpty()) {
				List<ChallengeDataInternalDto> challenges = crg.generateRules(
						challengeSpec, filteredUsers);
				assertTrue(!challenges.isEmpty());
				logger.debug("generated challenges: " + challenges.size()
						+ "\n");

			}
		}
		crg.closeStream();
	}

	@Test
	public void loadAndUploadTest() throws NullPointerException,
			IllegalArgumentException, IOException, UndefinedChallengeException {
		// load
		ChallengeRules result = ChallengeRulesLoader.load("w1_challenges.csv");

		assertTrue(result != null && !result.getChallenges().isEmpty());

		// get users from gamification engine
		List<Content> users = facade.readGameState(get(GAMEID));

		ChallengesRulesGenerator crg = new ChallengesRulesGenerator(
				new ChallengeInstanceFactory(), "generated-rules-report.csv");

		// generate challenges
		for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
			logger.debug("rules generation for challenge: "
					+ challengeSpec.getName());
			Matcher matcher = new Matcher(challengeSpec);
			List<Content> filteredUsers = matcher.match(users);
			logger.debug("found users: " + filteredUsers.size());
			// generate rule
			if (!filteredUsers.isEmpty()) {
				List<ChallengeDataInternalDto> challenges = crg.generateRules(
						challengeSpec, filteredUsers);
				assertTrue(!challenges.isEmpty());
				logger.debug("generated challenges: " + challenges.size()
						+ "\n");

			}
		}
		crg.closeStream();

		// upload
		String input = "output.json";
		System.out.println("Uploading for file " + input);
		// read input file
		ObjectMapper mapper = new ObjectMapper();
		TypeFactory typeFactory = mapper.getTypeFactory();
		String jsonString = IOUtils.toString(new FileInputStream(input));
		List<ChallengeDataInternalDto> challenges = mapper.readValue(
				jsonString, typeFactory.constructCollectionType(List.class,
						ChallengeDataInternalDto.class));
		System.out.println("Read challenges " + challenges.size());

		int tot = 0;
		StringBuffer buffer = new StringBuffer();
		buffer.append("CHALLENGE_NAME;CHALLENGE_MODEL;CHALLENGE_UUID;PLAYER_ID\n");

		boolean r = false;
		for (ChallengeDataInternalDto ch : challenges) {
			// upload and assign challenge
			tot++;
			r = challengeAssignFacade.assignChallengeToPlayer(ch.getDto(),
					ch.getGameId(), ch.getPlayerId());
			if (!r) {
				System.out.println("Error in uploading challenge instance "
						+ ch.getDto().getInstanceName());
				return;
			} else {
				System.out.println("Inserted challenge with Id "
						+ ch.getDto().getInstanceName());
				buffer.append(ch.getDto().getInstanceName() + ";");
				buffer.append(ch.getDto().getModelName() + ";");
				buffer.append(ch.getDto().getInstanceName() + ";");
				buffer.append(ch.getPlayerId() + ";");
			}
		}
		try {
			IOUtils.write(buffer, new FileOutputStream("report.csv"));
		} catch (IOException e) {
			System.err.println("Error in writing report.csv file");
			return;
		}
		System.out.println("Inserted challenge: " + tot + "\n"
				+ "Challenge upload completed");
		assertTrue(tot != 0);
	}

	// @Test
	// public void uploadTestChallenge() {
	// // save a new challenge model inside gamification engine
	// ChallengeModel cm = new ChallengeModel();
	// cm.setName("ChallengeModel" + UUID.randomUUID());
	// cm.setGameId(get(GAMEID));
	// cm.setId(cm.getName());
	// Set<String> variables = new HashSet<String>();
	// variables.add("variable1");
	// variables.add("variable2");
	// cm.setVariables(variables);
	// // insert model in gamification engine
	// assertTrue(challengeModelFacade.insertChallengeModel(get(GAMEID), cm));
	//
	// // create a new challenge instance and give it to a player
	// ChallengeDataDTO cdd = new ChallengeDataDTO();
	// cdd.setModelName(cm.getName());
	// cdd.setInstanceName("InstanceName" + UUID.randomUUID());
	// cdd.setStart(new Date());
	//
	// assertTrue(challengeAssignFacade.assignChallengeToPlayer(cdd,
	// get(GAMEID), "1"));
	// // TODO: cancellare istanza e modello
	// }

	@Test
	public void createZeoImpactChallengeInstance() {
		// Test related to rule
		// https://github.com/smartcommunitylab/smartcampus.gamification/blob/r2.0.0/game-engine.test/src/test/resources/rules/challengeTest/zeroimpactChallenge.drl

		LocalDate now = new LocalDate();

		ChallengeDataDTO cdd = new ChallengeDataDTO();
		cdd.setModelName("zeroImpact");
		cdd.setInstanceName("InstanceName" + UUID.randomUUID());
		cdd.setStart(now.dayOfMonth().addToCopy(-10).toDate());
		cdd.setEnd(now.dayOfMonth().addToCopy(5).toDate());
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("counter", 0);
		data.put("target", 1d);
		data.put("bonusPointType", "green leaves");
		data.put("bonusScore", 100d);

		cdd.setData(data);
		assertTrue(challengeAssignFacade.assignChallengeToPlayer(cdd,
				get(GAMEID), "1"));

	}

	@Test
	public void createTripNumberChallengeInstance() {
		// Test related to rule
		// https://github.com/smartcommunitylab/smartcampus.gamification/blob/r2.0.0/game-engine.test/src/test/resources/rules/challengeTest/zeroimpactChallenge.drl

		LocalDate now = new LocalDate();

		ChallengeDataDTO cdd = new ChallengeDataDTO();
		cdd.setModelName("tripNumber");
		cdd.setInstanceName("tripNumber_" + UUID.randomUUID());
		cdd.setStart(now.dayOfMonth().addToCopy(-10).toDate());
		cdd.setEnd(now.dayOfMonth().addToCopy(5).toDate());
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("mode", "bikeDistance");
		data.put("target", 2d);
		data.put("bonusPointType", "green leaves");
		data.put("bonusScore", 100d);
		data.put("counter", new Integer(0));

		cdd.setData(data);
		assertTrue(challengeAssignFacade.assignChallengeToPlayer(cdd,
				get(GAMEID), "1"));
	}

	// @Test
	// public void generateChallengeRulesAndInsertToGamificationEngine()
	// throws NullPointerException, IllegalArgumentException, IOException,
	// UndefinedChallengeException {
	// // load
	// ChallengeRules result = ChallengeRulesLoader.load("TestChallenges.csv");
	//
	// assertTrue(result != null && !result.getChallenges().isEmpty());
	//
	// // get users from gamification engine
	// List<Content> users = facade.readGameState(get(GAMEID));
	//
	// ChallengesRulesGenerator crg = new ChallengesRulesGenerator(
	// new ChallengeInstanceFactory(), "generated-rules-report.csv");
	//
	// Map<String, Map<String, Object>> playerIdCustomData = new HashMap<String,
	// Map<String, Object>>();
	// // generate challenges
	// for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
	// logger.debug("rules generation for challenge: "
	// + challengeSpec.getName());
	// Matcher matcher = new Matcher(challengeSpec);
	// List<Content> filteredUsers = matcher.match(users);
	// logger.debug("found users: " + filteredUsers.size());
	// List<ChallengeDataDTO> res = crg.generateRules(challengeSpec,
	// filteredUsers);
	// logger.debug("generated rules \n" + res.size() + "\n");
	//
	// assertTrue(!res.isEmpty());
	//
	// // update custom data for every user in challenge
	// playerIdCustomData = crg.getPlayerIdCustomData();
	// for (Content user : filteredUsers) {
	// insertFacade.updateChallengeCustomData(get(GAMEID),
	// user.getPlayerId(),
	// playerIdCustomData.get(user.getPlayerId()));
	// }
	//
	// // define rule
	// InsertedRuleDto rule = new InsertedRuleDto();
	// rule.setContent(res);
	// rule.setName(challengeSpec.getName());
	// // insert rule
	// InsertedRuleDto insertedRule = insertFacade.insertGameRule(
	// get(GAMEID), rule);
	// if (insertedRule != null) {
	// logger.debug("Inserted rule ");
	// assertTrue(!insertedRule.getId().isEmpty());
	// } else {
	// logger.error("Error during insertion of rules");
	// }
	//
	// }
	//
	// crg.closeStream();
	// }
}
