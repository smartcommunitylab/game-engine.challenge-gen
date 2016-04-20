package eu.trentorise.game.challenges;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.trentorise.game.challenges.api.ChallengeFactoryInterface;
import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.Challenge;
import eu.trentorise.game.challenges.model.ChallengeType;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.util.ChallengeRuleRow;
import eu.trentorise.game.challenges.util.ChallengeRulesLoader;

/**
 * Generate rules for challenges
 */
public class ChallengesRulesGenerator {

	private static final String LINE_SEPARATOR = System
			.getProperty("line.separator");
	private static final Logger logger = LogManager
			.getLogger(ChallengeRulesLoader.class);
	private static final int challengeLimitNumber = 2;

	private StringBuffer buffer;
	private ChallengeFactoryInterface factory;
	private Map<String, Map<String, Object>> playerIdCustomData;
	private StringBuffer reportBuffer;

	private final String reportHeader = "PLAYER;CHALLENGE_NAME;CHALLENGE_TYPE;TRANSPORT_MODE;BASELINE_VALUE;TARGET_VALUE;PRIZE;POINT_TYPE;CH_ID\n";
	private FileOutputStream fout;
	private FileOutputStream rout;
	private Map<String, Integer> challengeMap;

	public ChallengesRulesGenerator(ChallengeFactoryInterface factory,
			String reportName) throws IOException {
		this.buffer = new StringBuffer();
		this.playerIdCustomData = new HashMap<String, Map<String, Object>>();
		this.factory = factory;
		// prepare report output
		fout = new FileOutputStream(reportName);
		rout = new FileOutputStream("generatedRules.drl");
		// write header
		IOUtils.write(reportHeader, fout);
		// init challenge map
		challengeMap = new HashMap<String, Integer>();
	}

	/**
	 * Generate rules starting from a challenge specification for a set of given
	 * users
	 * 
	 * @param challengeSpec
	 * @param users
	 * @return
	 * @throws UndefinedChallengeException
	 * @throws IOException
	 */
	public String generateRules(ChallengeRuleRow challengeSpec,
			List<Content> users, String templateDir)
			throws UndefinedChallengeException, IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		buffer = new StringBuffer();
		buffer.append("/** " + challengeSpec.getType() + " "
				+ challengeSpec.getTarget().toString() + " **/\n");
		reportBuffer = new StringBuffer();
		playerIdCustomData.clear();
		// get right challenge
		for (Content user : users) {
			// create a challenge for user only under a specific limit
			if (getChallenges(user.getPlayerId()) < challengeLimitNumber) {
				Challenge c = factory.createChallenge(
						ChallengeType.valueOf(challengeSpec.getType()),
						templateDir);
				params = new HashMap<String, Object>();
				if (challengeSpec.getTarget() instanceof Double) {
					params.put("target", challengeSpec.getTarget());
				}
				if (challengeSpec.getType().equalsIgnoreCase(
						ChallengeType.NEXTBADGE.toString())
						|| challengeSpec.getType().equalsIgnoreCase(
								ChallengeType.BADGECOLLECTION.toString())) {
					params.put("badge_collection", challengeSpec.getGoalType());
				} else {
					params.put("mode", challengeSpec.getGoalType());
				}
				params.put("bonus", challengeSpec.getBonus());
				params.put("point_type", challengeSpec.getPointType());
				params.put(
						"baseline",
						user.getCustomData().getAdditionalProperties()
								.get(challengeSpec.getBaselineVar()));
				c.setTemplateParams(params);
				c.compileChallenge(user.getPlayerId());
				buffer.append(c.getGeneratedRules());

				reportBuffer.append(user.getPlayerId() + ";"
						+ challengeSpec.getName() + ";" + c.toString() + "\n");
				// save custom data for user for later use
				playerIdCustomData.put(user.getPlayerId(), c.getCustomData());

				// increase challenge number for user
				increaseChallenge(user.getPlayerId());
			}
		}
		// rest custom data counters
		playerIdCustomData = resetAllGamesCounter(playerIdCustomData);

		// write report to file
		IOUtils.write(reportBuffer.toString(), fout);

		// remove package declaration after first
		String result = filterPackageDeclaration(buffer.toString());

		// write generate rule to a file
		IOUtils.write(result, rout);

		return result;
	}

	private static Map<String, Map<String, Object>> resetAllGamesCounter(
			Map<String, Map<String, Object>> cs) {
		for (String userId : cs.keySet()) {
			Map<String, Object> customData = cs.get(userId);
			for (int i = 0; i < Constants.COUNTERS.length; i++) {
				customData.put(Constants.COUNTERS[i], null);
			}
			cs.put(userId, customData);
		}
		return cs;
	}

	private int getChallenges(String playerId) {
		if (!challengeMap.containsKey(playerId)) {
			return 0;
		}
		return challengeMap.get(playerId);
	}

	private void increaseChallenge(String playerId) {
		if (!challengeMap.containsKey(playerId)) {
			challengeMap.put(playerId, 1);
		} else {
			challengeMap.put(playerId, challengeMap.get(playerId) + 1);
		}
	}

	public void closeStream() throws IOException {
		if (fout != null) {
			fout.close();
		}
	}

	/**
	 * Filter buffer from package declaration after first one
	 * 
	 * @return filtered string or null if error
	 */
	private String filterPackageDeclaration(String temp) {
		buffer = new StringBuffer();
		boolean remove = false;
		try {
			BufferedReader rdr = new BufferedReader(new StringReader(temp));
			for (String line = rdr.readLine(); line != null; line = rdr
					.readLine()) {
				if (line.startsWith("package") && !remove) {
					remove = true;
					buffer.append(line).append(LINE_SEPARATOR);

				} else if (line.startsWith("package") && remove) {
					// do nothing
				} else {
					buffer.append(line).append(LINE_SEPARATOR);
				}
			}
			rdr.close();
			return buffer.toString();
		} catch (IOException e) {
			logger.error(e);
		}
		return null;
	}

	public Map<String, Map<String, Object>> getPlayerIdCustomData() {
		return playerIdCustomData;
	}

}
