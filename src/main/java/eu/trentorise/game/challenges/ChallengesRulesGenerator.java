package eu.trentorise.game.challenges;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.model.ChallengeDataInternalDto;
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

	private ChallengeInstanceFactory factory;
	// private Map<String, Map<String, Object>> playerIdCustomData;
	private StringBuffer reportBuffer;

	private final String reportHeader = "PLAYER;CHALLENGE_NAME;CHALLENGE_TYPE;TRANSPORT_MODE;BASELINE_VALUE;TARGET_VALUE;PRIZE;POINT_TYPE;CH_ID\n";
	private FileOutputStream fout;
	private Map<String, Integer> challengeMap;
	private FileOutputStream oout;

	public ChallengesRulesGenerator(ChallengeInstanceFactory factory,
			String reportName, String outputName) throws IOException {
		// this.playerIdCustomData = new HashMap<String, Map<String, Object>>();
		this.factory = factory;
		// prepare report output
		fout = new FileOutputStream(reportName);
		oout = new FileOutputStream(outputName);
		// write header
		IOUtils.write(reportHeader, fout);
		// init challenge map
		challengeMap = new HashMap<String, Integer>();
		logger.debug("ChallengesRulesGenerator - created");
	}

	/**
	 * Generate rules starting from a challenge specification for a set of given
	 * users
	 * 
	 * @param challengeSpec
	 * @param users
	 * @return list of challenges ready to be uploaded
	 * @throws UndefinedChallengeException
	 * @throws IOException
	 */
	public List<ChallengeDataInternalDto> generateRules(
			ChallengeRuleRow challengeSpec, List<Content> users,
			Date startDate, Date endDate) throws UndefinedChallengeException,
			IOException {
		logger.debug("ChallengesRulesGenerator - started");
		List<ChallengeDataInternalDto> result = new ArrayList<ChallengeDataInternalDto>();
		Map<String, Object> params = new HashMap<String, Object>();
		reportBuffer = new StringBuffer();
		// playerIdCustomData.clear();
		// get right challenge
		for (Content user : users) {
			// create a challenge for user only under a specific limit
			if (getChallenges(user.getPlayerId()) < challengeLimitNumber) {
				params.put(Constants.TARGET, challengeSpec.getTarget());
				params.put(Constants.BONUS_POINT_TYPE,
						challengeSpec.getPointType());
				params.put(Constants.BONUS_SCORE, challengeSpec.getBonus());
				params.put(Constants.START_DATE, startDate);
				params.put(Constants.END_DATE, endDate);

				ChallengeDataDTO cdd = factory.createChallenge(
						challengeSpec.getModelName(), params);
				ChallengeDataInternalDto cdit = new ChallengeDataInternalDto();
				cdit.setPlayerId(user.getPlayerId());
				cdit.setGameId(user.getGameId());
				cdit.setDto(cdd);
				result.add(cdit);

				reportBuffer.append(user.getPlayerId() + ";"
						+ challengeSpec.getName() + ";"
						+ challengeSpec.getModelName() + ";"
						+ challengeSpec.getGoalType() + ";"
						+ challengeSpec.getBaselineVar() + ";"
						+ challengeSpec.getTarget() + ";"
						+ challengeSpec.getBonus() + ";"
						+ challengeSpec.getPointType() + ";"
						+ cdd.getInstanceName() + "\n");
				// save custom data for user for later use
				// playerIdCustomData.put(user.getPlayerId(), cdd.getData());

				// increase challenge number for user
				increaseChallenge(user.getPlayerId());
			}
		}
		// write report to file
		IOUtils.write(reportBuffer.toString(), fout);

		// write json file
		ObjectMapper mapper = new ObjectMapper();
		try {
			IOUtils.write(mapper.writeValueAsString(result), oout);
		} catch (IOException e) {
			logger.error("Error in writing result " + e.getMessage());
		}
		// close stream
		if (oout != null) {
			try {
				fout.close();
			} catch (IOException e) {
				logger.error("Error in closing output file " + e.getMessage());
				return null;
			}
		}
		logger.debug("ChallengesRulesGenerator - completed - generated challenges "
				+ result.size());
		return result;
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

}
