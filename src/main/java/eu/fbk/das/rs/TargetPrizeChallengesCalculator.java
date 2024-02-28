package eu.fbk.das.rs;

import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.booster;
import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.getWeeklyContentMode;
import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.roundTarget;
import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.week_n;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import eu.fbk.das.GamificationEngineRestFacade;
import eu.fbk.das.rs.challenges.Challenge;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.calculator.DifficultyCalculator;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.utils.Pair;
import it.smartcommunitylab.model.GameStatistics;
import it.smartcommunitylab.model.PlayerStateDTO;


public class TargetPrizeChallengesCalculator {

    private static final Logger logger = Logger.getLogger(TargetPrizeChallengesCalculator.class);
    private GamificationEngineRestFacade facade;
    private DateTime execDate;
    private DateTime lastMonday;
    private GameStatistics gs;
    private DifficultyCalculator dc;
    private String gameId;
    private RecommendationSystem rs;
    private double modifier = 0.9;
    private HashMap<String, Integer> modeMax =  new HashMap<>();
    public static final String TEAM = "team";
    public static final String CURRENTPLAYERS_CUSTOMEDATA = "currentPlayers";
     
    // TODO remove
    public void prepare(RecommendationSystem rs, String gameId, DateTime execDate, HashMap<String, Integer> modeMaxMap) {
        this.execDate = execDate;
        facade = rs.facade;
        this.rs = rs;
        this.gameId = gameId;
        this.modeMax = modeMaxMap;
    }


    public Map<String, Double> targetPrizeChallengesCompute(String pId_1, String pId_2, String counter, String type) {

        prepare();

        Map<Integer, Double> quantiles = getQuantiles2(gameId, counter);

        Map<String, Double> res = new HashMap<>();

        Pair<Double, Double> res1 = getForecast("player1", pId_1, res, counter);
        double player1_tgt = res1.getFirst();
        double player1_bas = res1.getSecond();

        Pair<Double, Double> res2 = getForecast("player2", pId_2, res, counter);
        double player2_tgt = res2.getFirst();
        double player2_bas = res2.getSecond();


        double target;
        if (type.equals("groupCompetitiveTime")) {
            target = roundTarget(counter,((player1_tgt + player2_tgt) / 2.0) * modifier);

            target = checkMaxTargetCompetitive(counter, target);

            res.put("target", target);
                    res.put("player1_prz", evaluate(target, player1_bas, counter, quantiles));
            res.put("player2_prz",  evaluate(target, player2_bas, counter, quantiles));
        }
        else if (type.equals("groupCooperative")) {
            target = roundTarget(counter, (player1_tgt + player2_tgt) * modifier);

            target = checkMaxTargetCooperative(counter, target);

            double player1_prz = evaluate(player1_tgt, player1_bas, counter, quantiles);
            double player2_prz = evaluate(player2_tgt, player2_bas, counter, quantiles);
            double prz = Math.max(player1_prz, player2_prz);

            res.put("target", target);
            res.put("player1_prz", prz);
            res.put("player2_prz", prz);
        }  else if (type.equals("groupCompetitivePerformance")) {
            p("WRONG TYPE");
        } else
            p("UNKOWN TYPE");

        return res;
    }

	public Map<String, Double> targetPrizeHSCChallengesCompute(String pId_1, String pId_2, String counter, String type,
			Challenge chg) {
		prepare();
		Map<String, Double> res = new HashMap<>();
		Pair<Double, Double> res1 = getForecast("player1", pId_1, res, counter);
		double player1_tgt = res1.getFirst();
		Pair<Double, Double> res2 = getForecast("player2", pId_2, res, counter);
		double player2_tgt = res2.getFirst();
		double target;
		if (("groupCompetitiveTime").equals(type)) {
			target = roundTarget(counter, ((player1_tgt + player2_tgt) / 2.0) * modifier);
			// TEAM
			if (chg.getPlayerSet().contains(TEAM)) {
				PlayerStateDTO ts1 = facade.getPlayerState(gameId, pId_1);
				PlayerStateDTO ts2 = facade.getPlayerState(gameId, pId_2);
				if (ts1.getCustomData().containsKey(CURRENTPLAYERS_CUSTOMEDATA)
						&& ts2.getCustomData().containsKey(CURRENTPLAYERS_CUSTOMEDATA)) {
					Integer activePlayersT1 = (Integer) ts1.getCustomData().get(CURRENTPLAYERS_CUSTOMEDATA);
					Integer activePlayersT2 = (Integer) ts2.getCustomData().get(CURRENTPLAYERS_CUSTOMEDATA);
					Integer media = (activePlayersT1 + activePlayersT2) / 2;
					target = checkMaxTargetCompetitive(counter, target, media);
				} else {
					logger.error("Skipping team - missing attribute maxMembers");
				}
			} else {
				target = checkMaxTargetCompetitive(counter, target);
			}
			res.put("target", target);
			res.put("player1_prz", chg.getReward().getValue());
			res.put("player2_prz", chg.getReward().getValue());
		} else if ("groupCooperative".equals(type)) {
			target = roundTarget(counter, (player1_tgt + player2_tgt) * modifier);
			// team or single player
			if (chg.getPlayerSet().contains(TEAM)) {
				PlayerStateDTO ts1 = facade.getPlayerState(gameId, pId_1);
				PlayerStateDTO ts2 = facade.getPlayerState(gameId, pId_2);
				if (ts1.getCustomData().containsKey(CURRENTPLAYERS_CUSTOMEDATA)
						&& ts2.getCustomData().containsKey(CURRENTPLAYERS_CUSTOMEDATA)) {
					Integer activePlayersT1 = (Integer) ts1.getCustomData().get(CURRENTPLAYERS_CUSTOMEDATA);
					Integer activePlayersT2 = (Integer) ts2.getCustomData().get(CURRENTPLAYERS_CUSTOMEDATA);
					Integer somma = activePlayersT1 + activePlayersT2;
					target = checkMaxTargetCooperative(counter, target, somma);
				} else {
					logger.error("Skipping team - missing attribute maxMembers");
				}
			} else {
				target = checkMaxTargetCooperative(counter, target);
			}
			double prz = chg.getReward().getValue();
			res.put("target", target);
			res.put("player1_prz", prz);
			res.put("player2_prz", prz);
		} else if (type.equals("groupCompetitivePerformance")) {
			p("WRONG TYPE");
		} else
			p("UNKOWN TYPE");
		return res;
	}
    
    
	private double checkMaxTargetCompetitive(String mode, double v) {
		if (mode.equals(ChallengesConfig.WALK_KM) && v >= modeMax.get(ChallengesConfig.WALK_KM))
			return Math.min(this.modeMax.get(mode), v);
		if (mode.equals(ChallengesConfig.BIKE_KM) && v >= modeMax.get(ChallengesConfig.BIKE_KM))
			return Math.min(this.modeMax.get(mode), v);
		if (mode.equals(ChallengesConfig.TRAIN_TRIPS) && v >= modeMax.get(ChallengesConfig.TRAIN_TRIPS))
			return Math.min(this.modeMax.get(mode), v);
		if (mode.equals(ChallengesConfig.BUS_TRIPS) && v >= modeMax.get(ChallengesConfig.BUS_TRIPS))
			return Math.min(this.modeMax.get(mode), v);
		if (mode.equals(ChallengesConfig.GREEN_LEAVES) && v >= modeMax.get(ChallengesConfig.GREEN_LEAVES))
			return Math.min(this.modeMax.get(mode), v);

		return v;
	}

	private double checkMaxTargetCompetitive(String mode, double v, Integer activeMembers) {
		if (mode.equals(ChallengesConfig.WALK_KM) && v >= (modeMax.get(ChallengesConfig.WALK_KM) * activeMembers))
			return Math.min(this.modeMax.get(mode) * activeMembers, v);
		if (mode.equals(ChallengesConfig.BIKE_KM) && v >= (modeMax.get(ChallengesConfig.BIKE_KM) * activeMembers))
			return Math.min(this.modeMax.get(mode) * activeMembers, v);
		if (mode.equals(ChallengesConfig.TRAIN_TRIPS)
				&& v >= (modeMax.get(ChallengesConfig.TRAIN_TRIPS) * activeMembers))
			return Math.min(this.modeMax.get(mode) * activeMembers, v);
		if (mode.equals(ChallengesConfig.BUS_TRIPS) && v >= (modeMax.get(ChallengesConfig.BUS_TRIPS) * activeMembers))
			return Math.min(this.modeMax.get(mode) * activeMembers, v);
		if (mode.equals(ChallengesConfig.GREEN_LEAVES)
				&& v >= (modeMax.get(ChallengesConfig.GREEN_LEAVES) * activeMembers))
			return Math.min(this.modeMax.get(mode) * activeMembers, v);
		
		return v;
	}

	private double checkMaxTargetCooperative(String mode, double v) {
		if (mode.equals(ChallengesConfig.WALK_KM) && v >= modeMax.get(ChallengesConfig.WALK_KM))
			return Math.min(this.modeMax.get(mode) * 2, v);
		if (mode.equals(ChallengesConfig.BIKE_KM) && v >= modeMax.get(ChallengesConfig.BIKE_KM))
			return Math.min(this.modeMax.get(mode) * 2, v);
		if (mode.equals(ChallengesConfig.TRAIN_TRIPS) && v >= modeMax.get(ChallengesConfig.TRAIN_TRIPS))
			return Math.min(this.modeMax.get(mode) * 2, v);
		if (mode.equals(ChallengesConfig.BUS_TRIPS) && v >= modeMax.get(ChallengesConfig.BUS_TRIPS))
			return Math.min(this.modeMax.get(mode) * 2, v);
		if (mode.equals(ChallengesConfig.GREEN_LEAVES) && v >= modeMax.get(ChallengesConfig.GREEN_LEAVES))
			return Math.min(this.modeMax.get(mode) * 2, v);

		return v;
	}
	
	private double checkMaxTargetCooperative(String mode, double v, double activeMembers) {
		if (mode.equals(ChallengesConfig.WALK_KM) && v >= (modeMax.get(ChallengesConfig.WALK_KM) * activeMembers))
			return Math.min(this.modeMax.get(mode) * activeMembers * 2, v);
		if (mode.equals(ChallengesConfig.BIKE_KM) && v >= (modeMax.get(ChallengesConfig.BIKE_KM) * activeMembers))
			return Math.min(this.modeMax.get(mode) * activeMembers * 2, v);
		if (mode.equals(ChallengesConfig.TRAIN_TRIPS)
				&& v >= (modeMax.get(ChallengesConfig.TRAIN_TRIPS) * activeMembers))
			return Math.min(this.modeMax.get(mode) * activeMembers * 2, v);
		if (mode.equals(ChallengesConfig.BUS_TRIPS) && v >= (modeMax.get(ChallengesConfig.BUS_TRIPS) * activeMembers))
			return Math.min(this.modeMax.get(mode) * activeMembers * 2, v);
		if (mode.equals(ChallengesConfig.GREEN_LEAVES)
				&& v >= (modeMax.get(ChallengesConfig.GREEN_LEAVES) * activeMembers))
			return Math.min(this.modeMax.get(mode) * activeMembers * 2, v);
		
		return v;
	}
	
   private Pair<Double, Double> getForecast(String nm, String pId, Map<String, Double> res,  String counter) {

        PlayerStateDTO state = facade.getPlayerState(gameId, pId);

        Pair<Double, Double> forecast = forecastMode(state, counter);

        double tgt = forecast.getFirst();
        double bas = forecast.getSecond();

        tgt = checkMinTarget(counter, tgt);

        tgt = roundTarget(counter, tgt);

        // res.put(nm + "_id", Integer.valueOf(pId).doubleValue());

        res.put(nm + "_tgt", tgt);
        res.put(nm + "_bas", bas);

        return new Pair<>(tgt, bas);
    }

	private Double checkMinTarget(String counter, Double v) {
		if (counter.equals(ChallengesConfig.WALK_KM) || counter.equals(ChallengesConfig.BIKE_KM)
				|| counter.equals(ChallengesConfig.TRAIN_TRIPS) || counter.equals(ChallengesConfig.BUS_TRIPS)
				|| counter.equals(ChallengesConfig.GREEN_LEAVES)) {
			return Math.max(this.modeMax.get(counter), v);
		}
		logger.warn(String.format(
				"Unsupported value %s calculating min target, valid values: Walk_Km, Bike_Km, green leaves", counter));
		return 0.0;
	}

    private Map<Integer, Double> getQuantiles2(String gameId, String counter) {
        return rs.getStats().getQuantiles(counter);
    }

    private Map<String, Double> getQuantiles(String gameId, String counter) {

        // Da sistemare richiesta per dati della settimana precedente, al momento non presenti
        List<GameStatistics> stats = facade.readGameStatistics(gameId, lastMonday, counter);
        if (stats == null || stats.isEmpty()) {
            pf("Nope \n");
            return null;
        }

        gs = stats.iterator().next();
        return gs.getQuantiles();
    }

    private void prepare() {

            // Set next monday as start, and next sunday as end
            int week_day = execDate.getDayOfWeek();
            int d = (7 - week_day) + 1;

            lastMonday = execDate.minusDays(week_day-1).minusDays(7);

        dc = new DifficultyCalculator();
    }

    private Pair<Double, Double> forecastMode(PlayerStateDTO state, String counter) {

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
    private Pair<Double, Double> forecastWMA(int v, PlayerStateDTO state, String counter) {

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

    private int getWeekPlaying(PlayerStateDTO state, String counter) {

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

    public Pair<Double, Double> forecastModeSimple(PlayerStateDTO state, String counter) {

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

    // old approach
    private Pair<Double, Double> forecastOld(Map<String, Double> res, String nm, PlayerStateDTO state, String counter) {

        // Last 3 values?
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
            res.put(f("%s_base_%d", nm, ix), c);

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

        pv = checkMinTarget(counter, pv);

        res.put(f("%s_tgt", nm), pv);

        return new Pair<Double, Double>(pv, wma);
    }

    /*
    private GameStatistics getGameStatistics(Set<GameStatistics> stats, String mode) {
        for (GameStatistics gs: stats) {
            if (gs.getPointConceptName().equals(mode))
                return gs;
        }

        pf("ERROR COUNTER '%s' NOT FOUND", mode);
        return null;
    } */



        public Double evaluate(Double target, Double baseline, String counter, Map<Integer, Double> quantiles) {

            if (baseline == 0)
                return 100.0;

            Integer difficulty = DifficultyCalculator.computeDifficulty(quantiles,
                    baseline, target);

            double d = (target  / Math.max(1, baseline)) - 1;

            int prize = dc.calculatePrize(difficulty, d, counter);

            double bonus =  Math.ceil(prize * ChallengesConfig.competitiveChallengesBooster / 10.0) * 10;

            return Math.min(bonus, 300);
        }


    public static void pf(String format, Object... args) {
        System.out.printf(format, args);
    }

    public static String f(String format, Object... args) {
        return String.format(format, args);
    }
    
    public Map<String, Double> targetPrizeChallengesConfig(String pId_1, String pId_2, String counter, String type) {

        prepare();

        Map<Integer, Double> quantiles = getQuantiles2(gameId, counter);

        Map<String, Double> res = new HashMap<>();

        Pair<Double, Double> res1 = getForecast("player1", pId_1, res, counter);
        double player1_tgt = res1.getFirst();
        double player1_bas = res1.getSecond();

        Pair<Double, Double> res2 = getForecast("player2", pId_2, res, counter);
        double player2_tgt = res2.getFirst();
        double player2_bas = res2.getSecond();


        double target;
        if (type.equals("groupCompetitiveTime")) {
            target = roundTarget(counter,((player1_tgt + player2_tgt) / 2.0) * modifier);

            target = checkMaxTargetCompetitive(counter, target);

            res.put("target", target);
                    res.put("player1_prz", evaluate(target, player1_bas, counter, quantiles));
            res.put("player2_prz",  evaluate(target, player2_bas, counter, quantiles));
        }
        else if (type.equals("groupCooperative")) {
            target = roundTarget(counter, (player1_tgt + player2_tgt) * modifier);

            target = checkMaxTargetCooperative(counter, target);

            double player1_prz = evaluate(player1_tgt, player1_bas, counter, quantiles);
            double player2_prz = evaluate(player2_tgt, player2_bas, counter, quantiles);
            double prz = Math.max(player1_prz, player2_prz);

            res.put("target", target);
            res.put("player1_prz", prz);
            res.put("player2_prz", prz);
        }  else if (type.equals("groupCompetitivePerformance")) {
            p("WRONG TYPE");
        } else
            p("UNKOWN TYPE");

        return res;
    }

    public static void p(String s) {
        System.out.println(s);
    }
    
}
