package eu.fbk.das.rs.challengeGeneration;

import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.PointConcept;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.util.*;

import static eu.fbk.das.rs.Utils.*;


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
    // cfg
    private RecommendationSystemConfig cfg;

    /**
     * Create a new recommandation system challenge generator
     *
     * @param configuration
     * @throws IllegalArgumentException if cfg is null
     */
    public RecommendationSystemChallengeGeneration(RecommendationSystemConfig configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException(
                    "Recommandation system cfg must be not null");
        }
        this.cfg = configuration;
        dbg(logger, "RecommendationSystemChallengeGeneration init complete");
    }

    public List<ChallengeDataDTO> generate(Content cnt, String mode, DateTime execDate) {

        List<ChallengeDataDTO> output = new ArrayList<>();

        Double modeCounter = getContentMode(cnt, mode, execDate);
        modeCounter  = round(modeCounter, 1);

        double lastCounter = -1;

        if (modeCounter >= 1) {

            // generate different types of challenges by percentage
            for (int i = 0; i < cfg.getPercentage().length; i++) {
                // calculating the improvement of last week activity
                tmpValueimprovment = cfg.getPercentage()[i]
                        * modeCounter;

                improvementValue = tmpValueimprovment + modeCounter;

                if (equal(mode, cfg.GREEN_LEAVES)) {
                    improvementValue = Math.ceil(improvementValue);
                } else if (mode.endsWith("_Trips")) {
                    improvementValue = Math.ceil(improvementValue);
                } else {
                    improvementValue = round(improvementValue, 1);
                }

                if (improvementValue == lastCounter)
                    continue;

                if (Math.abs(improvementValue - modeCounter) < 0.01)
                    continue;

                lastCounter = improvementValue;

                ChallengeDataDTO cdd = prepareChallange(mode, execDate);

                cdd.setModelName("percentageIncrement");
                cdd.setData("target", improvementValue);
                cdd.setData("percentage", cfg.getPercentage()[i]);
                cdd.setData("baseline", modeCounter);

                output.add(cdd);
            }
        } else {

            if (equal(mode, cfg.GREEN_LEAVES))
                return output;

            // if (cfg.isDefaultMode(mode)) {

            // build a try once
            ChallengeDataDTO cdd = prepareChallange(mode, execDate);

            cdd.setModelName("absoluteIncrement");
            cdd.setData("target", 1);


            output.add(cdd);
            // }
        }

        return output;
    }

    public ChallengeDataDTO prepareChallange(String mode, DateTime execDate) {
        // Set next monday as start, and next sunday as end
        int week_day = execDate.getDayOfWeek();
        int d = (7 - week_day) + 1;

        DateTime startDate = execDate.plusDays(d);
        startDate = startDate.minusDays(2);
        DateTime endDate = startDate.plusDays(6);

        return prepareChallange(mode, startDate, endDate);
    }

    private ChallengeDataDTO prepareChallange(String mode, DateTime startDate, DateTime endDate) {

        ChallengeDataDTO cdd = new ChallengeDataDTO();
        cdd.setInstanceName(f("%s_%s_%s", cfg.getChallengeNamePrefix(),
                mode, UUID.randomUUID()));

        cdd.setStart(startDate.toDate());
        cdd.setEnd(endDate.toDate());

        cdd.setData("bonusPointType", "green leaves");
        cdd.setData("bonusScore", 100d);
        cdd.setData("counterName", mode);
        cdd.setData("periodName", "weekly");

        return cdd;
    }

    public Map<String, List<ChallengeDataDTO>> generate(List<Content> input,
                                                        DateTime start, DateTime end) {
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
            if (cfg.isUserfiltering()) {
                if (cfg.getPlayerIds()
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
                    for (int i = 0; i < cfg.getPercentage().length; i++) {
                        // calculating the improvement of last week activity
                        tmpValueimprovment = cfg.getPercentage()[i]
                                * modeCounter;
                        if (mode.endsWith("_Trips")) {
                            improvementValue = tmpValueimprovment + modeCounter;
                            improvementValue = Math.round(improvementValue);
                        } else {
                            improvementValue = tmpValueimprovment + modeCounter;
                        }
                        playersNum++;

                        ChallengeDataDTO cdd = prepareChallange(mode, start, end);

                        if (equal(mode, cfg.GREEN_LEAVES))
                            improvementValue = Math.floor(improvementValue);

                        cdd.setModelName("percentageIncrement");
                        cdd.setData("baseline", modeCounter);
                        cdd.setData("target", improvementValue);
                        cdd.setData("percentage", cfg.getPercentage()[i]);

                        output.get(playerId).add(cdd);
                    }
                } else {

                    if (equal(mode, cfg.GREEN_LEAVES))
                        continue;

                    if (cfg.isDefaultMode(mode)) {
                        playersNum++;
                        // build a try once
                        ChallengeDataDTO cdd = prepareChallange(mode, start, end);

                        cdd.setModelName("absoluteIncrement");
                        cdd.setData("target", 1);

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
        for (int i = 0; i < cfg.getDefaultMode().length; i++) {

            String mode = cfg.getDefaultMode()[i];
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


    protected Double getContentMode(Content cnt, String mode, DateTime execDate) {
        for (PointConcept pc : cnt.getState().getPointConcept()) {

            String m = pc.getName();
            if (!m.equals(mode))
                continue;

            return pc.getPeriodScore("weekly", execDate);
        }

        return 0.0;
    }

}