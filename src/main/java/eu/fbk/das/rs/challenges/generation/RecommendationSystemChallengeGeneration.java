package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.rs.challenges.ChallengeUtil;
import eu.fbk.das.rs.utils.Pair;
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
public class RecommendationSystemChallengeGeneration extends ChallengeUtil {

    private static final Logger logger = LogManager.getLogger(RecommendationSystem.class);

    private double lastCounter;

    public RecommendationSystemChallengeGeneration(RecommendationSystem rs) {
        super(rs);
    }

    public List<ChallengeDataDTO> generate(Player state, String mode, DateTime execDate) {
        return generate(state, mode, execDate, "treatment");
    }

    public List<ChallengeDataDTO> generate(Player state, String mode, DateTime execDate, String exp) {

        prepare(execDate);

        List<ChallengeDataDTO> output = new ArrayList<>();

        Double currentValue = rs.getWeeklyContentMode(state, mode, lastMonday);
        currentValue  = round(currentValue, 1);

        lastCounter = -1.0;

        if (currentValue >= 1) {

            // int pos = stats.getPosition(mode, currentValue);

            // if (pos > 4) {
            Pair<Double, Double> res;

            if ("treatment".equals(exp)) {
                res = forecastMode(state, mode);
            } else {
                res = oldChallengeMode(state, mode);
            }
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
            rs.rscv.valuate(cdd);

            cdd.setData("bonusScore", 100);


            output.add(cdd);
            // }
        }

        return output;
    }

    private ChallengeDataDTO generatePercentage(Double modeCounter, String mode, double improvementValue) {

        modeCounter = checkMax(modeCounter, mode);

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

        rs.rscv.valuate(cdd);

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

    public ChallengeDataDTO prepareChallange(String mode) {
        return prepareChallange(mode, mode);
    }

    public ChallengeDataDTO prepareChallange(String name, String mode) {

        ChallengeDataDTO cdd = new ChallengeDataDTO();
        cdd.setInstanceName(f("%s_%s_%s", prefix,
                name, UUID.randomUUID()));

        cdd.setStart(startDate.toDate());
        cdd.setEnd(endDate.toDate());

        cdd.setData("bonusPointType", "green leaves");
        cdd.setData("bonusScore", 100.0);
        if (mode != null)
            cdd.setData("counterName", mode);
        cdd.setData("periodName", "weekly");

        return cdd;
    }


/*
    private HashMap<String, HashMap<String, Double>> buildModeValues(
            HashMap<String, HashMap<String, Double>> modeValues,
            HashMap<String, Double> playerScore, Player content) {
        // retrieving the players' last week data "weekly"
        for (int i = 0; i < cfg.getPerfomanceCounters().length; i++) {

            String mode = cfg.getPerfomanceCounters()[i];
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
            for (String mode : ChallengesConfig.getPerfomanceCounters()) {
                challanges.addAll(generate(cnt, mode, new DateTime()));
            }

            res.put(cnt.getPlayerId(), challanges);
        }

        return res;
    }

    public List<ChallengeDataDTO> forecast(Player state, String mode, DateTime execDate) {

        prepare(execDate);

        Pair<Double, Double> res = forecastMode(state, mode);
        double target = res.getFirst();
        double baseline = res.getSecond();

        // p(forecastValue);

        List<ChallengeDataDTO> cha = new ArrayList<>();

        target = roundTarget(mode, target);

        ChallengeDataDTO cdd;

        if (target > 0) {
            cdd = generatePercentage(baseline, mode, target);
            cha.add(cdd);
        }

        double incr_targer = roundTarget(mode, Math.round(target * 1.1));
        if (incr_targer == target)
            incr_targer = target + getDelta(mode);
        if (incr_targer > 0) {
            cdd = generatePercentage(baseline, mode, incr_targer);
            cha.add(cdd);
        }

        double decr_targer = roundTarget(mode, Math.round(target * 0.9));
        if (decr_targer == target)
            decr_targer = target - getDelta(mode);
        if (decr_targer > 0) {
            cdd = generatePercentage(baseline, mode, decr_targer);
            cha.add(cdd);
        }

        return cha;
    }

    private double getDelta(String mode) {
        if (mode.equals(ChallengesConfig.GREEN_LEAVES))
            return 10;

        return 1;
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

    public Pair<Double, Double> oldChallengeMode(Player state, String counter) {

        DateTime date = lastMonday;
        Double lastValue = getWeeklyContentMode(state, counter, date);

        double value = lastValue * 1.3;


        return new Pair<Double, Double>(value, lastValue);
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

    public ChallengeDataDTO getRepetitive(String pId) {

        ChallengeDataDTO rep = prepareChallange("green leaves");

        rep.setModelName("repetitiveBehaviour");
        rep.setData("periodName", "daily");

        List<LinkedHashMap<String, Object>> last = getLastRepetitiveChallenges(pId);

        int[] targets = {60, 90, 135, 180, 240, 300, 375};
        int index;

        // check if there was a challenge two weeks ago
        LinkedHashMap<String, Object> cha = getRepetitiveWeek(last, 14);
        if (cha != null) {
            // decide what to do based on result
            LinkedHashMap<String, Object> fields = (LinkedHashMap<String, Object>) cha.get("fields");
            double periodTarget = (Double) fields.get("periodTarget");
            double target = (Double) fields.get("target");
            int tot = (int) Math.ceil(periodTarget * target);
            index = findIndex(targets, tot) + 1;
            if ((boolean) cha.get("completed")) index++; else index--;
            if (index < 0) index = 0;
            if (index > 7) index = 7;

        } else {
            // check if there was a challenge last week
            cha = getRepetitiveWeek(last, 7);
            if (cha != null) {
                index = 1;
            } else {
                index = 0;
            }
        }

        switch (index) {
            case 0:
                rep.setData("periodTarget", 2.0);
                rep.setData("target", 30.0);
                break;
            case 1:
                rep.setData("periodTarget", 2.0);
                rep.setData("target", 45.0);
                break;
            case 2:
                rep.setData("periodTarget", 3.0);
                rep.setData("target", 45.0);
                break;
            case 3:
                rep.setData("periodTarget", 3.0);
                rep.setData("target", 60.0);
                break;
            case 4:
                rep.setData("periodTarget", 4.0);
                rep.setData("target", 60.0);
                break;
            case 5:
                rep.setData("periodTarget", 4.0);
                rep.setData("target", 75.0);
                break;
            case 6:
                rep.setData("periodTarget", 5.0);
                rep.setData("target", 75.0);
                break;
            case 7:
                rep.setData("periodTarget", 5.0);
                rep.setData("target", 100.0);
                break;
        }

        rep.setData("bonusScore", 100.0 + index *30);

        // TODO baseline dei repetitive
        // rep.setData("percentage", improvementValue / modeCounter - 1);
        // rep.setData("baseline", modeCounter);
        // rep.setInfo("improvement", improvementValue / modeCounter);

        // rscv.valuate(cdd);


        rep.setState("assigned");
        rep.setOrigin("rs");
        rep.setInfo("id", 0);

        return rep;

    }

    private LinkedHashMap<String, Object> getRepetitiveWeek(List<LinkedHashMap<String, Object>> last, int days) {

        DateTime m = jumpToMonday(new DateTime().minusDays(days));

        for (LinkedHashMap<String, Object> cha: last) {

            DateTime start = jumpToMonday(new DateTime(cha.get("start")));

            int v = Math.abs(daysApart(m, start));
            if (v > 0)
                continue;

            return cha;
        }

        return null;
    }

    private List<LinkedHashMap<String, Object>> getLastRepetitiveChallenges(String pId) {

        List<LinkedHashMap<String, Object>> out = new ArrayList<>();


        List<LinkedHashMap<String, Object>> currentChallenges = rs.facade.getChallengesPlayer(rs.gameId, pId);
        for (LinkedHashMap<String, Object> cha: currentChallenges) {

            if (!((String) cha.get("modelName")).contains("repetitiveBehaviour"))
                continue;

            out.add(cha);

        }

        return out;
    }

}