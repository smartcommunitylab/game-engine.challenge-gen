
package eu.fbk.das.rs.challenges.evaluation;

import eu.fbk.das.rs.challenges.ChallengeUtil;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.trentorise.game.challenges.rest.Player;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.*;

import static com.google.common.math.DoubleMath.mean;
import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.getWeeklyContentMode;
import static eu.fbk.das.rs.challenges.generation.RecommendationSystem.getChallengeWeek;
import static eu.fbk.das.rs.utils.Utils.p;
import static eu.fbk.das.rs.utils.Utils.pf;

// Checks the rate of improvement between each week
public class ImprovementChecker extends ChallengeUtil {

    int week = 20;

    int v = 3;

    private HashMap<String, Map<Integer, List<Double>>> weekImpr;

    public static void main(String[] args) throws IOException, InterruptedException {
        ImprovementChecker cdg = new ImprovementChecker();

        // cdg.analyzeSelected();
        cdg.execute();
    }


    public ImprovementChecker() {
        super();
        playerLimit = 0;
        minLvl = -1;
    }


    public void execute() {

        prepare(new DateTime());

        List<String> players = getPlayers();
        for (String pId: players) {
            Player state = rs.facade.getPlayerState(rs.gameId, pId);
            consider(state);
        }

        for (String counter: counters) {
            pf(",%s-num, %s-median, %s-std", counter, counter, counter);
        }
        pf("\n");

        for (int ix = 0; ix < week; ix++) {
            pf("%d", ix);
            for (String counter: counters) {
                evaluate(ix, weekImpr.get(counter).get(ix));
            }
            pf("\n");
        }
    }

    private void evaluate(int ix, List<Double> improvs) {

        double[] ar = new double[improvs.size()];
        for (int iv = 0; iv < improvs.size(); iv++)
            ar[iv] = improvs.get(iv);
        Arrays.sort(ar);

        double mean = StatUtils.mean(ar);
        double median = StatUtils.percentile(ar, 50);
        double std = FastMath.sqrt(StatUtils.variance(ar));
        pf(", %d, %.2f, %.2f", ar.length, mean, std);
    }

    public void prepare(DateTime date) {
        super.prepare(getChallengeWeek(date));
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

            Map<Integer, List<Double>> improv = weekImpr.get(counter);

            for (int ix = week -1; ix >= 0; ix--) {
                if (ix <= v) {
                    continue;
                }

                double baseline = getWMABaseline(perf, ix -1);
                if (perf[ix] > 0 && baseline > 0)
                    improv.get(ix).add(perf[ix] / baseline);

            }
        }
    }

    private double getWMABaseline(double[] improv, int start) {

        double den = 0;
        double num = 0;
        int zero = 0;
        for (int ix = 0; ix < v; ix++) {
            // weight * value
            double c = improv[start - ix];
            if (c == 0)
                zero++;
            den += (v -ix) * c;
            num += (v -ix);
        }

        if (zero == v -1)
            return 0;

        double baseline = den / num;
        return baseline;
    }


}
