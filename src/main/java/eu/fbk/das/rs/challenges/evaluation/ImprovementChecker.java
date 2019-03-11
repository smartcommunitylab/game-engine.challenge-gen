
package eu.fbk.das.rs.challenges.evaluation;

import eu.fbk.das.rs.challenges.ChallengeUtil;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.utils.ArrayUtils;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.Player;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.math.DoubleMath.mean;
import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.getWeeklyContentMode;
import static eu.fbk.das.rs.utils.Utils.p;
import static eu.fbk.das.rs.utils.Utils.pf;

// Checks the rate of improvement between each week
public class ImprovementChecker extends ChallengeUtil {

    int week = 18;

    int v = 5;

    private HashMap<String, Map<Integer, List<Double>>> weekImpr;

    private String[] counters;


    public ImprovementChecker(RecommendationSystemConfig cfg, GamificationEngineRestFacade facade) {
        super(cfg);
        setFacade(facade);

        playerLimit = 0;
        minLvl = -1;
    }


    public void execute() {

        prepare(new DateTime());

        List<String> players = getPlayers();
        for (String pId: players) {
            Player state = facade.getPlayerState(gameId, pId);
            consider(state);
        }

        for (String counter: counters) {
            pf ("checking counter: %s \n", counter);
            for (int ix = week - 1; ix >= 0; ix--) {
                evaluate(ix, weekImpr.get(counter).get(ix));
            }
        }
    }

    private void evaluate(int ix, List<Double> improvs) {

        double[] ar = new double[improvs.size()];
        for (int iv = 0; iv < improvs.size(); iv++)
            ar[iv] = improvs.get(iv);

        double mean = StatUtils.mean(ar);
        double median = StatUtils.percentile(ar, 50);
        double std = FastMath.sqrt(StatUtils.variance(ar));
        pf("%d \t %d \t %.2f \t %.2f \n", ix, ar.length, median, std);
    }

    public void prepare(DateTime date) {
        super.prepare(date);

        counters = ChallengesConfig.getPerfomanceCounters();

        weekImpr = new HashMap<String, Map<Integer, List<Double>>>();
        for (String counter: counters) {
            HashMap<Integer, List<Double>> impr = new HashMap<>();
            for (int ix = week -1; ix >= 0; ix--) {
                impr.put(ix, new ArrayList<>());
            }
            weekImpr.put(counter, impr);
        }
    }

    private void consider(Player state) {

        for (String counter: counters) {

            double[] perf = new double[week];

            DateTime date = lastMonday;

            for (int ix = week -1; ix > 0; ix--) {
                // weight * value
                Double c = getWeeklyContentMode(state, counter, date);
                perf[ix] = c;
                date = date.minusDays(7);
            }

            // p(perf);

            double[] improv = new double[week];

            for (int ix = week -1; ix >= 0; ix--) {
                if (ix <= v) {
                    improv[ix] = -1;
                    continue;
                }

                double baseline = getWMABaseline(perf, ix -1);
                if (baseline > 0)
                    improv[ix] = perf[ix] / baseline;
                else
                    improv[ix] = -1;
            }

            // p(improv);

            for (int ix = week -1; ix >= 0; ix--) {
                if (improv[ix] == -1)
                    continue;

                weekImpr.get(counter).get(ix).add(improv[ix]);
            }
        }
    }

    private double getWMABaseline(double[] improv, int start) {

        double den = 0;
        double num = 0;
        for (int ix = 0; ix < v; ix++) {
            // weight * value
            double c = improv[start - ix];
            den += (v -ix) * c;
            num += (v -ix);
        }

        double baseline = den / num;
        return baseline;
    }


}
