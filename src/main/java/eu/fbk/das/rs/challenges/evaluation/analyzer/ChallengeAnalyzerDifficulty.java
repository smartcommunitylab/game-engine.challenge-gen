package eu.fbk.das.rs.challenges.evaluation.analyzer;

import eu.fbk.das.rs.challenges.evaluation.ChallengeAnalyzer;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static eu.fbk.das.utils.Utils.pf;

public class ChallengeAnalyzerDifficulty extends ChallengeAnalyzer {

    private Map<String, List<Double>> cache;

    public static void main(String[] args) {
        ChallengeAnalyzerDifficulty cdg = new ChallengeAnalyzerDifficulty();

        cdg.analyzeAll();
    }

    public void prepare() {
        super.prepare();

        cache = new LinkedHashMap<>();

        checkIfChosen = false;

        weekStart = 20;
    }

    protected void experiment(String file) {

        List<Double> ls = new ArrayList<>();

        for (String pId : challenges.keySet()) {

            List<ChallengeRecord> chas = challenges.get(pId);

            for (ChallengeRecord cha: chas) {
                if (cha.baseline == 0)
                    continue;
                ls.add(cha.target * 1.0 / cha.baseline);
            }
        }

        cache.put(file, ls);
    }

    protected void output() {

        for (String f : cache.keySet()) {

            List<Double> ls = cache.get(f);

            double[] ar = new double[ls.size()];
            for (int iv = 0; iv < ls.size(); iv++)
                ar[iv] = ls.get(iv);

            double mean = StatUtils.mean(ar);
            double median = StatUtils.percentile(ar, 50);
            double std = FastMath.sqrt(StatUtils.variance(ar));

            pf("\n%s,  %.2f, %.2f,%d", f, median, std, ar.length);
        }
    }

}
