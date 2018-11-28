package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
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

    private static final Logger logger = LogManager.getLogger(RecommendationSystem.class);

    private final RecommendationSystemChallengeValuator rscv;

    private RecommendationSystemConfig cfg;

    private DateTime endDate;
    private DateTime startDate;
    private DateTime execDate;
    private DateTime lastMonday;
    private RecommendationSystemStatistics stats;
    private String prefix;
    private double lastCounter;

    /**
     * Create a new recommandation system challenge generator
     */
    public RecommendationSystemChallengeGeneration(RecommendationSystemConfig configuration, RecommendationSystemChallengeValuator rscv) {
        if (configuration == null) {
            throw new IllegalArgumentException(
                    "Recommandation system cfg must be not null");
        }
        this.cfg = configuration;
        dbg(logger, "RecommendationSystemChallengeGeneration init complete");

        this.rscv = rscv;
    }

    private void prepare(DateTime execDate, RecommendationSystem rs) {
        // Set next monday as start, and next sunday as end
        int week_day = execDate.getDayOfWeek();
        int d = (7 - week_day) + 1;

        lastMonday = execDate.minusDays(week_day-1).minusDays(7);

        startDate = execDate.plusDays(d);
        startDate = startDate.minusDays(2);
        endDate = startDate.plusDays(7);

        prefix = f(cfg.getChallengeNamePrefix(), rs.getChallengeWeek(execDate));
    }

    public List<ChallengeDataDTO> generate(Content state, String mode, DateTime execDate, RecommendationSystem rs) {

        prepare(execDate, rs);

        List<ChallengeDataDTO> output = new ArrayList<>();

        Double currentValue = rs.getWeeklyContentMode(state, mode, lastMonday);
        currentValue  = round(currentValue, 1);

        lastCounter = -1.0;

        if (currentValue >= 1) {

            int pos = stats.getPosition(mode, currentValue);

            if (pos > 4) {

                Double lastValue = rs.getWeeklyContentMode(state, mode, lastMonday.minusDays(7));

                Double forecastValue = forecastMode(currentValue, lastValue);

                forecastValue = checkMax(forecastValue, mode);

                ChallengeDataDTO cdd = generatePercentage(currentValue, mode, forecastValue, (forecastValue / currentValue) - 1);
                if (cdd != null)
                    output.add(cdd);

            } else {

                // generate different types of challenges by percentage
                for (int i = 0; i < cfg.getPercentage().length; i++) {
                    // calculating the improvement of last week activity
                    // define a temporary variable to save the improvement
                    double tmpValueimprovment = cfg.getPercentage()[i] * currentValue;

                    // define a variable that the player should improve its mode
                    double improvementValue = tmpValueimprovment + currentValue;

                    improvementValue = checkMax(improvementValue, mode);

                    ChallengeDataDTO cdd = generatePercentage(currentValue, mode, improvementValue, cfg.getPercentage()[i]);
                    if (cdd != null)
                        output.add(cdd);

                    lastCounter = improvementValue;

                }
            }
        } else {

            if (equal(mode, RecommendationSystemConfig.GREEN_LEAVES))
                return output;

            // if (cfg.isDefaultMode(mode)) {

            // build a try once
            ChallengeDataDTO cdd = prepareChallange(mode);

            cdd.setModelName("absoluteIncrement");
            cdd.setData("target", 1.0);
            cdd.setInfo("improvement", 1.0);
            rscv.valuate(cdd);

            cdd.setData("bonusScore", 100);


            output.add(cdd);
            // }
        }

        return output;
    }

    private ChallengeDataDTO generatePercentage(Double modeCounter, String mode, double improvementValue, Double impr) {
         if (mode.endsWith("_Trips")) {
            improvementValue = Math.ceil(improvementValue);
        } else {
             if (improvementValue > 1000)
                 improvementValue = Math.ceil(improvementValue / 100) * 100;
             else if (improvementValue > 100)
                 improvementValue = Math.ceil(improvementValue / 10) * 10;
             else
                 improvementValue = Math.ceil(improvementValue);
        }

        if (improvementValue == lastCounter)
            return null;

        if (Math.abs(improvementValue - lastCounter) < 0.01)
            return null;

        lastCounter = improvementValue;

        ChallengeDataDTO cdd = prepareChallange(mode);

        cdd.setModelName("percentageIncrement");
        cdd.setData("target", improvementValue);
        cdd.setData("percentage", impr);
        cdd.setData("baseline", modeCounter);
        cdd.setInfo("improvement", improvementValue / modeCounter);

        rscv.valuate(cdd);

        return cdd;
    }

    private double checkMax(double v, String mode) {
        if (mode.equals(RecommendationSystemConfig.WALK_KM) && v >= 70)
            return 70;
        if (mode.equals(RecommendationSystemConfig.BIKE_KM) && v >= 210)
            return 210;
        if (mode.equals(RecommendationSystemConfig.TRAIN_TRIPS) && v >= 56)
            return 56;
        if (mode.equals(RecommendationSystemConfig.BUS_TRIPS) && v >= 56)
            return 56;

        if (mode.equals(RecommendationSystemConfig.GREEN_LEAVES) && v >= 3000)
            return 3000;

        return v;

    }

    ChallengeDataDTO prepareChallange(String mode) {

        ChallengeDataDTO cdd = new ChallengeDataDTO();
        cdd.setInstanceName(f("%s_%s_%s", prefix,
                mode, UUID.randomUUID()));

        cdd.setStart(startDate.toDate());
        cdd.setEnd(endDate.toDate());

        cdd.setData("bonusPointType", "green leaves");
        cdd.setData("bonusScore", 100);
        cdd.setData("counterName", mode);
        cdd.setData("periodName", "weekly");

        return cdd;
    }


/*
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
    } */


    public Map<String, List<ChallengeDataDTO>> generateAll(List<Content> data) {

        Map<String, List<ChallengeDataDTO>> res = new HashMap<>();

        for (Content cnt: data) {

            List<ChallengeDataDTO> challanges = new ArrayList<>();
            for (String mode : cfg.getDefaultMode()) {
                challanges.addAll(generate(cnt, mode, new DateTime(), new RecommendationSystem()));
            }

            res.put(cnt.getPlayerId(), challanges);
        }

        return res;
    }

    public List<ChallengeDataDTO> forecast(Content state, String mode, DateTime execDate, RecommendationSystem rs) {

        prepare(execDate, rs);

        Double currentValue = rs.getWeeklyContentMode(state, mode, lastMonday);
        Double lastValue = rs.getWeeklyContentMode(state, mode, lastMonday.minusDays(7));

        Double forecastValue = forecastMode(currentValue, lastValue);

        p(forecastValue);

        List<ChallengeDataDTO> cha = new ArrayList<>();

        double[] impr = new double[]{-0.1, 0, 0.1};

        lastCounter = -1;

        for (int i = 0; i < 3; i++) {

            Double tgt = forecastValue + forecastValue * impr[i];

            tgt = checkMax(tgt, mode);

            ChallengeDataDTO cdd = generatePercentage(currentValue, mode, tgt, (tgt / currentValue) - 1);
            if (cdd != null)
                cha.add(cdd);

        }



        return cha;

    }

    public Double forecastMode(Double currentValue, Double lastValue) {
        double slope = (lastValue - currentValue) / lastValue;
        slope = Math.abs(slope) * 0.8;
        if (slope > 0.3)
            slope = 0.3;

        return (currentValue * (1 + slope));
    }


    // Prepare quantiles given statistic
    public void prepare(RecommendationSystemStatistics stats) {
        this.stats = stats;
    }
}