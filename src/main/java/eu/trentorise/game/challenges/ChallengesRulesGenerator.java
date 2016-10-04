package eu.trentorise.game.challenges;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.model.ChallengeDataInternalDto;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.PointConcept;
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

	private final String reportHeader = "PLAYER;CHALLENGE_NAME;CHALLENGE_TYPE;GOAL_TYPE;BASELINE_VALUE;TARGET_VALUE;PRIZE;POINT_TYPE;CH_ID\n";
	private FileOutputStream fout;
	private Map<String, Integer> challengeMap;
	private FileOutputStream oout;
	private List<ChallengeDataInternalDto> challenges;

	public ChallengesRulesGenerator(ChallengeInstanceFactory factory,
			String reportName, String outputName) throws IOException {
		// this.playerIdCustomData = new HashMap<String, Map<String, Object>>();
		this.factory = factory;
		this.challenges = new ArrayList<ChallengeDataInternalDto>();
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
	 * @throws UndefinedChallengeException
	 * @throws IOException
	 */
	public void generateChallenges(ChallengeRuleRow challengeSpec,
			List<Content> users, Date startDate, Date endDate)
			throws UndefinedChallengeException, IOException {
		logger.debug("ChallengesRulesGenerator - started");
		Map<String, Object> params = new HashMap<String, Object>();
		reportBuffer = new StringBuffer();
		// playerIdCustomData.clear();
		Double targetValue = 0d;
		Double baseLineValue = 0d;
		// get right challenge
		for (Content user : users) {
			// create a challenge for user only under a specific limit
			if (getChallenges(user.getPlayerId()) < challengeLimitNumber) {
				params.put(Constants.NAME, challengeSpec.getName());
				params.put(Constants.BONUS_POINT_TYPE,
						challengeSpec.getPointType());
				params.put(Constants.BONUS_SCORE, challengeSpec.getBonus());
				params.put(Constants.PERIOD_NAME, "weekly");
				params.put(Constants.GOAL_TYPE, challengeSpec.getGoalType());
				params.put(Constants.START_DATE, startDate);
				params.put(Constants.END_DATE, endDate);
				params.put(Constants.TARGET, challengeSpec.getTarget());
				if (challengeSpec.getTarget() instanceof Double) {
					targetValue = (Double) challengeSpec.getTarget();
				} else {
					targetValue = Double.valueOf((String) challengeSpec
							.getTarget());
				}
				if (challengeSpec.getBaselineVar() != null
						&& !challengeSpec.getBaselineVar().isEmpty()) {
					// for percentage challenges, calculate current baseline and
					// correct target
					baseLineValue = getPointConceptCurrentValue(user,
							challengeSpec.getBaselineVar(), "weekly");
					params.put(Constants.BASELINE, baseLineValue);
					targetValue = baseLineValue * (1.0d + targetValue);
					targetValue = Double.valueOf(Math.round(targetValue));
					if (targetValue < 1) {
						targetValue = 1.0d;
					}
					params.put(Constants.TARGET, targetValue);
				}

				ChallengeDataDTO cdd = factory.createChallenge(
						challengeSpec.getModelName(), params, user);
				ChallengeDataInternalDto cdit = new ChallengeDataInternalDto();
				cdit.setPlayerId(user.getPlayerId());
				cdit.setGameId(user.getGameId());
				cdit.setDto(cdd);
				challenges.add(cdit);

				reportBuffer.append(user.getPlayerId()
						+ ";"
						+ challengeSpec.getName()
						+ ";"
						+ challengeSpec.getModelName()
						+ ";"
						+ challengeSpec.getGoalType()
						+ ";"
						+ (Double.compare(baseLineValue, 0d) == 0 ? ""
								: baseLineValue) + ";" + targetValue + ";"
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
	}

	private Double getPointConceptCurrentValue(Content user,
			String baselineVar, String periodName) {
		if (StringUtils.isEmpty(baselineVar)) {
			throw new IllegalArgumentException(
					"baselineVar must be a not null and not empty string");
		}
		String names[] = new String[3];
		if (baselineVar.contains(".")) {
			names = baselineVar.split("\\.");
		}

		for (PointConcept pc : user.getState().getPointConcept()) {
			if (baselineVar.contains(".")) {
				if (pc.getName().equalsIgnoreCase(names[0])) {
					if (names[2].equalsIgnoreCase("current")) {
						return pc.getPeriodCurrentScore(names[1]);
					} else if (names[2].equalsIgnoreCase("previous")) {
						return pc.getPeriodPreviousScore(names[1]);
					}
				}

			} else if (pc.getName().equalsIgnoreCase(baselineVar)) {
				return pc.getPeriodCurrentScore(periodName);
			}
		}
		return 0d;
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

	private void closeStream() throws IOException {
		if (fout != null) {
			fout.close();
		}
		// close stream
		if (oout != null) {
			oout.close();
		}
	}

	public void writeChallengesToFile() throws IOException {
		// write json file
		ObjectMapper mapper = new ObjectMapper();
		//
		IOUtils.write(mapper.writeValueAsString(challenges), oout);
		logger.debug("ChallengesRulesGenerator - completed - written challenges "
				+ challenges.size());
		closeStream();
	}

}
