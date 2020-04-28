package eu.fbk.das.rs.challenges.evaluation;

import eu.fbk.das.rs.challenges.ChallengeUtil;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.utils.PolynomialRegression;
import eu.trentorise.game.challenges.rest.Player;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.getWeeklyContentMode;
import static eu.fbk.das.rs.utils.Utils.*;
import static org.chocosolver.util.tools.ArrayUtils.sort;

public class PerformanceEstimation extends ChallengeUtil {


    private int week = 19;

    private HashMap<String, List<double[]>> record;

    private Map<String, Map<String, List<Double>>> total_error;
    private Map<String, List<Double>> method_error;
    private List<Double> counter_error;
    private PrintWriter writer;

    public static void main(String[] args) throws IOException, InterruptedException {
        PerformanceEstimation cdg = new PerformanceEstimation();
        cdg.execute();
    }

    public PerformanceEstimation() {
        super();
        playerLimit = 50;
    }

    public void prepare(DateTime date) {
        super.prepare(date);

        record = new HashMap<>();
        for (String counter: counters) {
            record.put(counter, new ArrayList<>());
        }

        total_error = new HashMap<>();

        try {
            writer = new PrintWriter(new File("performances.csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void execute() {

        prepare(new DateTime());

        List<String> players = getPlayers();

        for (String pId: players) {
            Player state = rs.facade.getPlayerState(rs.gameId, pId);
            consider(state);
        }

        for (String counter: counters) {
            for (double[] perf : record.get(counter)) {
                testPrediction(counter, perf);
            }
        }

        for (String meth: sort(total_error.keySet())) {
            method_error = total_error.get(meth);

            double tot_error = 0.0;
            int num = 0;
            for (String counter: method_error.keySet()) {
                counter_error = method_error.get(counter);

                    for (double e: counter_error)
                        tot_error += e;

                    num +=  counter_error.size();
            }

            tot_error /= num;
            pf("%.2f: %8s \n", tot_error, meth);
        }
    }

    private void computeMase(String counter, String meth, double[] perf, int w) {

        // MASE computation

        double num_mase = 0.0;
        double den_mase = 0;
        for (int ix = week -1; ix > w; ix--) {
            double tr = perf[ix];
            double pred;

            if (meth.equals("wma"))
                    pred = getWMA(perf, w, ix - 1);
            else if (meth.equals("ma"))
                    pred = getMA(perf, w, ix - 1);
            else if (meth.equals("poly"))
                pred = getPolinomial(perf, w, ix - 1);
            else
                continue;


            double num =  Math.abs(pred - tr);
            double den = Math.abs(tr - perf[ix-1]);

           //  pf("%.2f - %.2f \n", num, den);

            num_mase += num;
            den_mase += den;
        }

        if (den_mase > 0)
            addError(counter, f("%s-%d", meth, w), num_mase / den_mase);

    }

    private void addError(String counter, String meth, double v) {
        if (!total_error.containsKey(meth))
            total_error.put(meth, new HashMap<>());
        method_error = total_error.get(meth);

        if (!method_error.containsKey(counter))
            method_error.put(counter, new ArrayList<>());
        counter_error = method_error.get(counter);

        counter_error.add(v);
    }


    private void testPrediction(String counter, double[] perf) {

        for (String meth: new String[] {"wma", "ma", "poly"})
        for (int w = 2; w < 6; w++) {
            computeMase(counter, meth, perf, w);
        }

        // test all other methods?
    }

    private void consider(Player state) {

        for (String counter: counters) {

            double[] perf = new double[week];
            String[] out = new String[week];

            DateTime date = lastMonday;
            double sum = 0;


            for (int ix = week -1; ix > 0; ix--) {
                // weight * value
                Double c = getWeeklyContentMode(state, counter, date);
                perf[ix] = c;
                date = date.minusDays(7);

                out[ix] = f("%.2f", c);
                sum+= c;
            }

            record.get(counter).add(perf);

            if (sum > 0) {
                writer.println(StringUtils.join(out, ","));
                writer.flush();
            }
        }
    }

    private double getWMA(double[] improv, int v, int start) {

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

    private double getMA(double[] improv, int v, int start) {

        double pred = 0.0;
        for (int ix = 0; ix < v; ix++) {
           pred += improv[start - ix];
        }

        return pred / v;
    }


    private double getPolinomial(double[] perf, int v, int start) {

        double[] x = new double[v];
        double[] y = new double[v];
        for (int ix = 0 ; ix < v; ix++) {
            // weight * value
            int it = start - (v -ix);
            x[ix] = perf[it];
            y[ix] = it;
        }

        PolynomialRegression regression = new PolynomialRegression(x, y, 3);
        double pred = regression.predict(start);
        if (pred > 100 || pred < -100)
            p("ciao");
        return pred;
    }
}
