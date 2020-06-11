package eu.fbk.das.old;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.fbk.das.model.ChallengeExpandedDTO;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.PointConcept;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static eu.fbk.das.rs.utils.Utils.dbg;

/**
 * Generate rules for challenges
 */
public class ChallengesRulesGenerator {

    private static final Logger logger = LogManager.getLogger(ChallengeRulesLoader.class);
    private static final int challengeLimitNumber = 2;

    private ChallengeInstanceFactory factory;
    // private Map<String, Map<String, Object>> playerIdCustomData;
    private StringBuffer reportBuffer;

    private final String reportHeader =
            "PlayerStateDTO;CHALLENGE_NAME;CHALLENGE_TYPE;GOAL_TYPE;BASELINE_VALUE;TARGET_VALUE;PRIZE;POINT_TYPE;CH_ID"
                    + Constants.LINE_SEPARATOR;
    private FileOutputStream fout;
    private Map<String, Integer> challengeMap;
    private FileOutputStream oout;
    private List<ChallengeExpandedDTO> challenges;

    public ChallengesRulesGenerator(ChallengeInstanceFactory factory, String reportName,
                                    String outputName) throws IOException {
        this.factory = factory;
        this.challenges = new ArrayList<ChallengeExpandedDTO>();
        // prepare report output
        fout = new FileOutputStream(reportName);
        oout = new FileOutputStream(outputName);
        // write header
        IOUtils.write(reportHeader, fout);
        // init challenge map
        challengeMap = new HashMap<String, Integer>();
        dbg(logger, "ChallengesRulesGenerator - created");
    }

    /**
     * Generate rules starting from a challenge specification for a set of given users
     *
     * @param challengeSpec
     * @param users
     * @throws UndefinedChallengeException
     * @throws IOException
     */
    public void generateChallenges(ChallengeRuleRow challengeSpec, List<PlayerStateDTO> users,
                                   Date startDate, Date endDate) throws Exception {
        dbg(logger, "ChallengesRulesGenerator - started");
        this.reportBuffer = new StringBuffer();

        Map<String, Object> params = new HashMap<String, Object>();
        Double targetValue = 0d;
        Double baseLineValue = 0d;
        // get right challenge
        for (PlayerStateDTO user : users) {
            // create a challenge for user only under a specific limit
            if (getChallenges(user.getPlayerId()) < challengeLimitNumber) {
                params.put(Constants.NAME, StringUtils.trim(challengeSpec.getName()));
                params.put(Constants.BONUS_POINT_TYPE,
                        StringUtils.trim(challengeSpec.getPointType()));
                params.put(Constants.BONUS_SCORE, challengeSpec.getBonus());
                params.put(Constants.PERIOD_NAME, StringUtils.isBlank(challengeSpec.getPeriodName())
                        ? "weekly" : challengeSpec.getPeriodName()); // to maintain compatibility
                // with old csv with periodName
                // field wasn't explicit and
                // manage this side as with a
                // constant
                params.put(Constants.PERIOD_TARGET, challengeSpec.getPeriodTarget());
                params.put(Constants.GOAL_TYPE, StringUtils.trim(challengeSpec.getGoalType()));
                params.put(Constants.START_DATE, startDate);
                params.put(Constants.END_DATE, endDate);
                if (challengeSpec.getTarget() != null) {
                    if (challengeSpec.getTarget() instanceof String) {
                        // special case for leaderboardPosition challenge
                        if (((String) challengeSpec.getTarget())
                                .contains(Constants.MIN_MAX_SEPARATOR)) {
                            params.put(Constants.TARGET, challengeSpec.getTarget());
                        }
                    } else {
                        params.put(Constants.TARGET, challengeSpec.getTarget());
                        if (challengeSpec.getTarget() instanceof Double) {
                            targetValue = (Double) challengeSpec.getTarget();
                        } else {
                            String v = (String) challengeSpec.getTarget();
                            if (!v.isEmpty()) {
                                targetValue = Double.valueOf(v);
                            }
                        }
                    }
                }
                if (challengeSpec.getBaselineVar() != null
                        && !challengeSpec.getBaselineVar().isEmpty()) {
                    // for percentage challenges, calculate current baseline and
                    // correct target or use the max value
                    if (!challengeSpec.getBaselineVar().endsWith(".max")) {
                        baseLineValue = getPointConceptCurrentValue(user,
                                challengeSpec.getBaselineVar(), "weekly");
                    } else {
                        baseLineValue = getPointConceptCurrentValue(user,
                                challengeSpec.getBaselineVar(), "weekly");
                    }
                    params.put(Constants.BASELINE, baseLineValue);
                    targetValue = baseLineValue * (1.0d + targetValue);
                    targetValue = Double.valueOf(Math.round(targetValue));
                    if (targetValue < 1) {
                        targetValue = 1.0d;
                    }
                    params.put(Constants.TARGET, targetValue);
                }

                ChallengeExpandedDTO cdd =
                        factory.createChallenge(challengeSpec.getModelName(), params, user);
                ChallengeExpandedDTO cdit = new ChallengeExpandedDTO();
                cdit.setInfo("playerId", user.getPlayerId());
                cdit.setInfo("gameId", user.getGameId());
                challenges.add(cdit);

                reportBuffer.append(user.getPlayerId() + ";" + challengeSpec.getName() + ";"
                        + challengeSpec.getModelName() + ";" + challengeSpec.getGoalType() + ";"
                        + (Double.compare(baseLineValue, 0d) == 0 ? "" : baseLineValue) + ";"
                        + targetValue + ";" + challengeSpec.getBonus() + ";"
                        + challengeSpec.getPointType() + ";" + cdd.getInstanceName()
                        + Constants.LINE_SEPARATOR);

                // increase challenge number for user
                increaseChallenge(user.getPlayerId());
            }
        }
        // write report to file
        IOUtils.write(reportBuffer.toString(), fout);
    }

    private Double getPointConceptCurrentValue(PlayerStateDTO user, String baselineVar,
                                               String periodName) {
        if (StringUtils.isEmpty(baselineVar)) {
            throw new IllegalArgumentException(
                    "baselineVar must be a not null and not empty string");
        }
        String names[] = new String[3];
        if (baselineVar.contains(".")) {
            names = baselineVar.split("\\.");
        }

        for (GameConcept gc: user.getState().get("PointConcept")) {
            PointConcept pc = (PointConcept) gc;
            if (baselineVar.contains(".")) {
                if (pc.getName().equalsIgnoreCase(names[0])) {
                    if (names[2].equalsIgnoreCase("current")) {
                        return pc.getPeriodCurrentScore(names[1]);
                    } else if (names[2].equalsIgnoreCase("previous")) {
                        return pc.getPeriodPreviousScore(names[1]);
                    } else if (names[2].equalsIgnoreCase("max")) {
                        return PointConceptUtil.getScoreMax(user, names[0], names[1]);
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
        dbg(logger,
                "ChallengesRulesGenerator - completed - written challenges " + challenges.size());
        closeStream();
    }

    /**
     * Before challenge generation, add a set of challenges and use them before actual challenge
     * generation
     *
     * @param rsChallenges
     * @param gameId
     * @throws IOException
     */
    public void setChallenges(Map<String, List<ChallengeExpandedDTO>> rsChallenges, String gameId)
            throws IOException {
        if (reportBuffer == null) {
            reportBuffer = new StringBuffer();
        }
        // update generated challenges list
        for (String playerId : rsChallenges.keySet()) {
            if (rsChallenges.get(playerId) != null) {
                for (ChallengeExpandedDTO challenge : rsChallenges.get(playerId)) {
                    ChallengeExpandedDTO cdit = new ChallengeExpandedDTO();
                    cdit.setInfo("playerId", playerId);
                    cdit.setInfo("gameId", gameId);
                    increaseChallenge(playerId);
                    // buffer
                    reportBuffer.append(playerId + ";" + challenge.getData("challengeName")
                            + ";" + challenge.getModelName() + ";" + "goalType" + ";" + 
                            challenge.getData("baseline") 
                            + ";" + (challenge.getData("target")) + ";"
                            + challenge.getData("bonusScore") + ";"
                            + challenge.getData("bonusPointType") + ";"
                            + challenge.getInstanceName() + Constants.LINE_SEPARATOR);
                    removeUnusedData(cdit);
                    challenges.add(cdit);
                }
            }
        }
        // write to the file
        IOUtils.write(reportBuffer.toString(), fout);
    }

    private void removeUnusedData(ChallengeExpandedDTO cdit) {
        // during recommandation system challenge generation we save some data
        // into challenge data structure, we need to remove it (ie. for now
        // challengeName
        if (cdit != null && cdit.getData() != null) {
            if (cdit.hasData("challengeName")) {
                cdit.delData("challengeName");
            }
            if (cdit.hasData("percentage")) {
                cdit.delData("percentage");
            }
        }
    }
}
