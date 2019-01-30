package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.rs.utils.Pair;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Player;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import java.util.*;

import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.*;
import static eu.fbk.das.rs.utils.Utils.*;


/**
 * RecommendationSystem challenge generation module: generate all possible
 * challenges using provided {@link ChallengesConfig}
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
    private RecommendationSystem rs;

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
        
        this.rs = rs;

        prefix = f(ChallengesConfig.getChallengeNamePrefix(), rs.getChallengeWeek(execDate));
    }

    public List<ChallengeDataDTO> generate(Player state, String mode, DateTime execDate, RecommendationSystem rs) {

        prepare(execDate, rs);

        List<ChallengeDataDTO> output = new ArrayList<>();

        Double currentValue = rs.getWeeklyContentMode(state, mode, lastMonday);
        currentValue  = round(currentValue, 1);

        lastCounter = -1.0;

        if (currentValue >= 1) {

            int pos = stats.getPosition(mode, currentValue);

            // if (pos > 4) {

                Pair<Double, Double> res = forecastMode(state, mode);
                double target = res.getFirst();
                double baseline = res.getSecond();

                target = checkMax(target, mode);

                ChallengeDataDTO cdd = generatePercentage(baseline, mode, target);
                if (cdd != null)
                    output.add(cdd);

            /* } else {

                // generate different types of challenges by percentage
                for (int i = 0; i < ChallengesConfig.getPercentage().length; i++) {
                    // calculating the improvement of last week activity
                    // define a temporary variable to save the improvement
                    double tmpValueimprovment = ChallengesConfig.getPercentage()[i] * currentValue;

                    // define a variable that the player should improve its mode
                    double improvementValue = tmpValueimprovment + currentValue;

                    improvementValue = checkMax(improvementValue, mode);

                    ChallengeDataDTO cdd = generatePercentage(currentValue, mode, improvementValue);
                    if (cdd != null)
                        output.add(cdd);

                    lastCounter = improvementValue;

                }
            }*/
        } else {

            if (equal(mode, ChallengesConfig.GREEN_LEAVES))
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

    private ChallengeDataDTO generatePercentage(Double modeCounter, String mode, double improvementValue) {

        improvementValue = roundTarget(mode, improvementValue);

        if (improvementValue == lastCounter)
            return null;

        if (Math.abs(improvementValue - lastCounter) < 0.01)
            return null;

        lastCounter = improvementValue;

        ChallengeDataDTO cdd = prepareChallange(mode);

        cdd.setModelName("percentageIncrement");
        cdd.setData("target", improvementValue);
        cdd.setData("percentage", improvementValue / modeCounter - 1);
        cdd.setData("baseline", modeCounter);
        cdd.setInfo("improvement", improvementValue / modeCounter);

        rscv.valuate(cdd);

        return cdd;
    }

    private double checkMax(double v, String mode) {
        if (mode.equals(ChallengesConfig.WALK_KM) && v >= 70)
            return 70;
        if (mode.equals(ChallengesConfig.BIKE_KM) && v >= 210)
            return 210;
        if (mode.equals(ChallengesConfig.TRAIN_TRIPS) && v >= 56)
            return 56;
        if (mode.equals(ChallengesConfig.BUS_TRIPS) && v >= 56)
            return 56;

        if (mode.equals(ChallengesConfig.GREEN_LEAVES) && v >= 3000)
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
            HashMap<String, Double> playerScore, Player content) {
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


    public Map<String, List<ChallengeDataDTO>> generateAll(List<Player> data) {

        Map<String, List<ChallengeDataDTO>> res = new HashMap<>();

        for (Player cnt: data) {

            List<ChallengeDataDTO> challanges = new ArrayList<>();
            for (String mode : ChallengesConfig.getDefaultMode()) {
                challanges.addAll(generate(cnt, mode, new DateTime(), new RecommendationSystem()));
            }

            res.put(cnt.getPlayerId(), challanges);
        }

        return res;
    }

    public List<ChallengeDataDTO> forecast(Player state, String mode, DateTime execDate, RecommendationSystem rs) {

        prepare(execDate, rs);

        Pair<Double, Double> res = forecastMode(state, mode);
        double target = res.getFirst();
        double baseline = res.getSecond();

        // p(forecastValue);

        List<ChallengeDataDTO> cha = new ArrayList<>();

        double[] impr = new double[]{-0.1, 0, 0.1};

        lastCounter = -1;

        for (int i = 0; i < 3; i++) {

            target = target + target * impr[i];

            target = checkMax(target, mode);

            ChallengeDataDTO cdd = generatePercentage(baseline, mode, target);
            if (cdd != null)
                cha.add(cdd);

        }



        return cha;

    }


    private Pair<Double, Double> forecastMode(Player state, String counter) {

        // Check date of registration, decide which method to use
        int week_playing = getWeekPlaying(state, counter);

        if (week_playing == 1) {
            Double baseline = getWeeklyContentMode(state, counter, lastMonday);
            return new Pair<Double, Double>(baseline*booster, baseline);
        } else if (week_playing == 2) {
            return forecastModeSimple(state, counter);
        }

        return forecastWMA(Math.min(week_n, week_playing), state, counter);
    }

    // Weighted moving average
    private Pair<Double, Double> forecastWMA(int v, Player state, String counter) {

        DateTime date = lastMonday;

        double den = 0;
        double num = 0;
        for (int ix = 0; ix < v; ix++) {
            // weight * value
            Double c = getWeeklyContentMode(state, counter, date);
            den += (v -ix) * c;
            num += (v -ix);

            date = date.minusDays(7);
        }

        double baseline = den / num;

        double pv = baseline * booster;

        return new Pair<Double, Double>(pv, baseline);
    }

    private int getWeekPlaying(Player state, String counter) {

        DateTime date = lastMonday;
        int i = 0;
        while (i < 100) {
            // weight * value
            Double c = getWeeklyContentMode(state, counter, date);
            if (c.equals(-1.0))
                break;
            i++;
            date = date.minusDays(7);
        }

        return i;
    }

    public Pair<Double, Double> forecastModeSimple(Player state, String counter) {

        DateTime date = lastMonday;
        Double currentValue = getWeeklyContentMode(state, counter, date);
        date = date.minusDays(7);
        Double lastValue = getWeeklyContentMode(state, counter, date);

        double slope = (lastValue - currentValue) / lastValue;
        slope = Math.abs(slope) * 0.8;
        if (slope > 0.3)
            slope = 0.3;

        double value = currentValue * (1 + slope);
        if (value == 0 || Double.isNaN(value))
            value = 1;


        return new Pair<Double, Double>(value, currentValue);
    }



    private Pair<Double, Double> forecastModeOld(Player state, String counter) {

        // CHECK if it has at least 3 weeks of data!
        // TODO

        // Last 3 values
        int v = 3;
        double[][] d = new double[v][];

        DateTime date = lastMonday;

        double wma = 0;
        int wma_d = 0;

        for (int i = 0 ; i < v; i++) {
            int ix = v - (i+1);
            d[ix] = new double[2];
            Double c = getWeeklyContentMode(state, counter, date);
            d[ix][1] = c;
            d[ix][0] = ix + 1;
            date = date.minusDays(7);
            // res.put(f("%s_base_%d", nm, ix), c);

            wma += (v-i) * c;
            wma_d += (v-i);
        }

        wma /= wma_d;

        SimpleRegression simpleRegression = new SimpleRegression(true);
        simpleRegression.addData(d);

        double slope = simpleRegression.getSlope();
        double intercept =  simpleRegression.getIntercept();
        double pv;
        if (slope < 0)
            pv = wma * 1.1;
        else
            pv = intercept + slope * (v+1) * 0.9;

        // pv = checkMinTarget(counter, pv);

        // res.put(f("%s_tgt", nm), pv);

        return new Pair<Double, Double>(pv, wma);
    }

    private Double getWeeklyContentMode(Player state, String counter, DateTime date) {
        return rs.getWeeklyContentMode(state, counter, date);
    }

    // Prepare quantiles given statistic
    public void prepare(RecommendationSystemStatistics stats) {
        this.stats = stats;
    }
}