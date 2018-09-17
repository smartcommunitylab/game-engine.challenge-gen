package eu.fbk.das.rs.challengeGeneration;

import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.PointConcept;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static eu.fbk.das.rs.Utils.dbg;
import static eu.fbk.das.rs.Utils.f;

/**
 * RecommendationSystem challenge generation module: generate all possible
 * challenges using provided {@link RecommendationSystemConfig}
 */
public class RecommendationSystemChallengeGeneration {

    private static final Logger logger = LogManager
            .getLogger(RecommendationSystem.class);

    // define a variable that the player should improve its mode
    private double improvementValue = 0;
    // define a temporary variable to save the improvement
    private double tmpValueimprovment = 0;
    // configuration
    private RecommendationSystemConfig configuration;

    /**
     * Create a new recommandation system challenge generator
     *
     * @param configuration
     * @throws IllegalArgumentException if configuration is null
     */
    public RecommendationSystemChallengeGeneration(RecommendationSystemConfig configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException(
                    "Recommandation system configuration must be not null");
        }
        this.configuration = configuration;
        dbg(logger, "RecommendationSystemChallengeGeneration init complete");
    }

    public List<ChallengeDataDTO> generate(Content cnt, String mode, Date execDate) {

        List<ChallengeDataDTO> output = new ArrayList<>();

        Double modeCounter = getContentMode(cnt, mode, execDate);

        if (modeCounter > 0) {

            // generate different types of challenges by percentage
            for (int i = 0; i < configuration.getPercentage().length; i++) {
                // calculating the improvement of last week activity
                tmpValueimprovment = configuration.getPercentage()[i]
                        * modeCounter;
                if (mode.endsWith("_Trips")) {
                    improvementValue = tmpValueimprovment + modeCounter;
                    improvementValue = Math.round(improvementValue);
                } else {
                    improvementValue = tmpValueimprovment + modeCounter;
                }

                ChallengeDataDTO cdd = prepareChallange(mode, execDate, "go");
                cdd.setModelName("percentageIncrement");
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("target", improvementValue);
                data.put("bonusPointType", "green leaves");
                data.put("bonusScore", 100d);
                data.put("baseline", modeCounter);
                data.put("counterName", mode);
                data.put("periodName", "weekly");
                data.put("percentage", configuration.getPercentage()[i]);
                data.put("challengeName",
                        configuration.getChallengeNamePrefix() + mode
                                + "_"
                                + configuration.getPercentage()[i]);
                cdd.setData(data);
                output.add(cdd);
            }
        } else {
            // if (configuration.isDefaultMode(mode)) {

                // build a try once
                ChallengeDataDTO cdd = prepareChallange(mode, execDate, "try");
                cdd.setModelName("absoluteIncrement");
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("target", 1);
                data.put("bonusPointType", "green leaves");
                data.put("bonusScore", 100d);
                data.put("counterName", mode);
                data.put("periodName", "weekly");
                data.put("challengeName",
                        configuration.getChallengeNamePrefix() + mode
                                + "_try");
                cdd.setData(data);
                output.add(cdd);
            // }
        }

        return output;
    }

    public ChallengeDataDTO prepareChallange(String mode, Date execDate, String sep) {
        ChallengeDataDTO cdd = new ChallengeDataDTO();
        cdd.setInstanceName(f("%s_%s_%s_%s", configuration.getChallengeNamePrefix(),
                mode, "sep", UUID.randomUUID()));

        // Set next monday as start, and next sunday as end
        Calendar cal = Calendar.getInstance();
        cal.setTime(execDate);
        int week_day = cal.get(Calendar.DAY_OF_WEEK);
        int d = (7 - week_day) + 1;

        cal.add(Calendar.DATE, d);
        cdd.setStart(cal.getTime());
        cal.add(Calendar.DATE, 7);
        cdd.setEnd(cal.getTime());
        return cdd;
    }

    public Map<String, List<ChallengeDataDTO>> generate(List<Content> input,
                                                        Date start, Date end) {
        if (input == null) {
            throw new IllegalArgumentException("Input must be not null");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Start and end date for challenges must be not null");
        }
        if (start.compareTo(end) == 0) {
            throw new IllegalArgumentException(
                    "Start and end date for challenges must be different");
        }
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException(
                    "Start date for challenges must be before end of challenges");
        }
        Map<String, List<ChallengeDataDTO>> output = new HashMap<String, List<ChallengeDataDTO>>();
        HashMap<String, HashMap<String, Double>> modeValues = new HashMap<String, HashMap<String, Double>>();
        HashMap<String, Double> playerScore = new HashMap<>();
        for (Content content : input) {
            // filter users
            if (configuration.isUserfiltering()) {
                if (configuration.getPlayerIds()
                        .contains(content.getPlayerId())) {
                    modeValues = buildModeValues(modeValues, playerScore,
                            content);
                }
            } else {
                modeValues = buildModeValues(modeValues, playerScore, content);
            }
            playerScore.clear();
        }

        int playersNum = 0;
        for (String mode : modeValues.keySet()) {
            for (String playerId : modeValues.get(mode).keySet()) {
                if (output.get(playerId) == null) {
                    output.put(playerId, new ArrayList<ChallengeDataDTO>());
                }
                Double modeCounter = modeValues.get(mode).get(playerId);
                if (modeCounter >= 0) {

                    // generate different types of challenges by percentage
                    for (int i = 0; i < configuration.getPercentage().length; i++) {
                        // calculating the improvement of last week activity
                        tmpValueimprovment = configuration.getPercentage()[i]
                                * modeCounter;
                        if (mode.endsWith("_Trips")) {
                            improvementValue = tmpValueimprovment + modeCounter;
                            improvementValue = Math.round(improvementValue);
                        } else {
                            improvementValue = tmpValueimprovment + modeCounter;
                        }
                        playersNum++;
                        ChallengeDataDTO cdd = new ChallengeDataDTO();
                        cdd.setModelName("percentageIncrement");
                        cdd.setInstanceName(configuration
                                .getChallengeNamePrefix()
                                + mode
                                + "_"
                                + UUID.randomUUID());
                        cdd.setStart(start);
                        cdd.setEnd(end);
                        Map<String, Object> data = new HashMap<String, Object>();
                        data.put("target", improvementValue);
                        data.put("bonusPointType", "green leaves");
                        data.put("bonusScore", 100d);
                        data.put("baseline", modeCounter);
                        data.put("counterName", mode);
                        data.put("periodName", "weekly");
                        data.put("percentage", configuration.getPercentage()[i]);
                        data.put("challengeName",
                                configuration.getChallengeNamePrefix() + mode
                                        + "_"
                                        + configuration.getPercentage()[i]);
                        cdd.setData(data);
                        output.get(playerId).add(cdd);
                    }
                } else {
                    if (configuration.isDefaultMode(mode)) {
                        playersNum++;
                        // build a try once
                        ChallengeDataDTO cdd = new ChallengeDataDTO();
                        cdd.setModelName("absoluteIncrement");
                        cdd.setInstanceName(configuration
                                .getChallengeNamePrefix()
                                + mode
                                + "_try_"
                                + UUID.randomUUID());
                        cdd.setStart(start);
                        cdd.setEnd(end);
                        Map<String, Object> data = new HashMap<String, Object>();
                        data.put("target", 1);
                        data.put("bonusPointType", "green leaves");
                        data.put("bonusScore", 100d);
                        data.put("counterName", mode);
                        data.put("periodName", "weekly");
                        data.put("challengeName",
                                configuration.getChallengeNamePrefix() + mode
                                        + "_try");
                        cdd.setData(data);
                        output.get(playerId).add(cdd);
                    }
                }
            }
        }
        dbg(logger, "players used from challenge generation : " + playersNum);
        return output;
    }

    private HashMap<String, HashMap<String, Double>> buildModeValues(
            HashMap<String, HashMap<String, Double>> modeValues,
            HashMap<String, Double> playerScore, Content content) {
        // retrieving the players' last week data "weekly"
        for (int i = 0; i < configuration.getDefaultMode().length; i++) {

            String mode = configuration.getDefaultMode()[i];
            for (PointConcept pc : content.getState().getPointConcept()) {
                if (pc.getName().equals(mode)) {
                    Double score = pc.getPeriodCurrentScore("weekly");
                    playerScore.put(content.getPlayerId(), score);
                }
            }
            if (modeValues.get(mode) == null) {
                modeValues.put(mode, new HashMap<String, Double>());
            }
            modeValues.get(mode).putAll(playerScore);
        }
        return modeValues;
    }


    private Double getContentMode(Content cnt, String mode, Date execDate) {
        for (PointConcept pc : cnt.getState().getPointConcept()) {

            String m = pc.getName();
            if (!m.equals(mode))
                continue;

            return pc.getPeriodScore("weekly", execDate.getTime());
        }

        return 0.0;
    }

}