package eu.trentorise.challenge.generation;

import static eu.trentorise.challenge.PropertiesUtil.CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.GAMEID;
import static eu.trentorise.challenge.PropertiesUtil.HOST;
import static eu.trentorise.challenge.PropertiesUtil.INSERT_CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.PASSWORD;
import static eu.trentorise.challenge.PropertiesUtil.USERNAME;
import static eu.trentorise.challenge.PropertiesUtil.get;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import eu.trentorise.game.challenges.ChallengeFactory;
import eu.trentorise.game.challenges.ChallengesRulesGenerator;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.InsertedRuleDto;
import eu.trentorise.game.challenges.util.ChallengeRuleRow;
import eu.trentorise.game.challenges.util.ChallengeRules;
import eu.trentorise.game.challenges.util.ChallengeRulesLoader;
import eu.trentorise.game.challenges.util.Matcher;

public class GenerationTest {

	private static final Logger logger = LogManager
			.getLogger(GenerationTest.class);

	private GamificationEngineRestFacade facade;
	private GamificationEngineRestFacade insertFacade;

	@Before
	public void setup() {
		facade = new GamificationEngineRestFacade(get(HOST) + get(CONTEXT),
				get(USERNAME), get(PASSWORD));
		insertFacade = new GamificationEngineRestFacade(get(HOST)
				+ get(INSERT_CONTEXT), get(USERNAME), get(PASSWORD));
	}

	@Test
	public void loadChallengeRuleGenerate() throws NullPointerException,
			IllegalArgumentException, IOException {
		// load
		ChallengeRules result = ChallengeRulesLoader
				.load("BetaTestChallengesWeek3.csv");

		assertTrue(result != null && !result.getChallenges().isEmpty());

		List<Content> users = facade.readGameState(get(GAMEID));

		// generate challenges
		for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
			Matcher matcher = new Matcher(challengeSpec);
			List<Content> r = matcher.match(users);

			System.out.println();
		}
	}

	@Test
	public void loadTestGeneration() throws NullPointerException,
			IllegalArgumentException, IOException, UndefinedChallengeException {
		// load
		ChallengeRules result = ChallengeRulesLoader
				.load("BetaTestChallengesWeek3.csv");

		assertTrue(result != null && !result.getChallenges().isEmpty());

		// get users from gamification engine
		List<Content> users = facade.readGameState(get(GAMEID));

		ChallengesRulesGenerator crg = new ChallengesRulesGenerator(
				new ChallengeFactory(), "generated-rules-report.csv");

		// generate challenges
		for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
			logger.debug("rules generation for challenge: "
					+ challengeSpec.getName());
			Matcher matcher = new Matcher(challengeSpec);
			List<Content> filteredUsers = matcher.match(users);
			logger.debug("found users: " + filteredUsers.size());
			// generate rule
			if (!filteredUsers.isEmpty()) {
				String res = crg.generateRules(challengeSpec, filteredUsers,
						"rules/templates");
				logger.debug("generated rules \n" + res + "\n");
				assertTrue(!res.isEmpty());
			}
		}

		crg.closeStream();
	}

	@Test
	public void generateChallengeRulesAndInsertToGamificationEngine()
			throws NullPointerException, IllegalArgumentException, IOException,
			UndefinedChallengeException {
		// load
		ChallengeRules result = ChallengeRulesLoader
				.load("BetaTestChallenges.csv");

		assertTrue(result != null && !result.getChallenges().isEmpty());

		// get users from gamification engine
		List<Content> users = facade.readGameState(get(GAMEID));

		ChallengesRulesGenerator crg = new ChallengesRulesGenerator(
				new ChallengeFactory(), "generated-rules-report.csv");

		Map<String, Map<String, Object>> playerIdCustomData = new HashMap<String, Map<String, Object>>();
		// generate challenges
		for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
			logger.debug("rules generation for challenge: "
					+ challengeSpec.getName());
			Matcher matcher = new Matcher(challengeSpec);
			List<Content> filteredUsers = matcher.match(users);
			logger.debug("found users: " + filteredUsers.size());
			String res = crg.generateRules(challengeSpec, filteredUsers,
					"rules/templates");
			logger.debug("generated rules \n" + res + "\n");

			assertTrue(!res.isEmpty());

			// update custom data for every user in challenge
			playerIdCustomData = crg.getPlayerIdCustomData();
			for (Content user : filteredUsers) {
				insertFacade.updateChallengeCustomData(get(GAMEID),
						user.getPlayerId(),
						playerIdCustomData.get(user.getPlayerId()));
			}

			// define rule
			InsertedRuleDto rule = new InsertedRuleDto();
			rule.setContent(res);
			rule.setName(challengeSpec.getName());
			// insert rule
			InsertedRuleDto insertedRule = insertFacade.insertGameRule(
					get(GAMEID), rule);
			if (insertedRule != null) {
				logger.debug("Inserted rule ");
				assertTrue(!insertedRule.getId().isEmpty());
			} else {
				logger.error("Error during insertion of rules");
			}

		}

		crg.closeStream();
	}
}
