package eu.fbk.das.rs.challenges.evaluation.analyzer;

import eu.fbk.das.rs.challenges.evaluation.ChallengeAnalyzer;
import it.smartcommunitylab.model.PlayerStateDTO;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.getWeeklyContentMode;
import static eu.fbk.das.rs.utils.Utils.pf;

public class ChallengeAnalyzerChoice extends ChallengeAnalyzer {

    private Map<Integer, int[]> cache;

    int[] l;

    public static void main(String[] args) {
        ChallengeAnalyzerChoice cdg = new ChallengeAnalyzerChoice();

        cdg.analyzeAll();
    }

    protected void prepare() {
        super.prepare();

        cache = new TreeMap<>();

        weekStart = 20;
    }

    protected void experiment(String file) {

        for (String pId : challenges.keySet()) {

            PlayerStateDTO player = rs.facade.getPlayerState(rs.gameId, pId);

            List<ChallengeRecord> chas = challenges.get(pId);

            Integer w = getChallengeWeek(new DateTime(chas.get(0).start));

            // does not consider if he was not active during that week
            if (getWeeklyContentMode(player, "green leaves", new DateTime(chas.get(0).start)) <= 0)
                continue;

            if (!cache.containsKey(w)) {
                l = new int[2];
                cache.put(w, l);
            }

            l = cache.get(w);
            l[0] ++;

            // check if the user chose any challenges
            double targetChosen = getPlayerChosen(chas);
            if (targetChosen <= 0)
                l[1]++;

            }

        }


    protected void output() {

        for (Integer week: cache.keySet()) {
            l = cache.get(week);
            pf ("%d,%.2f, (%d / %d) \n", week, l[1] * 1.0 / l[0], l[1], l[0]);
        }
    }

}
