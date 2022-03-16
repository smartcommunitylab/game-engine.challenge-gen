package eu.fbk.das.rs;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.ChallengeConcept;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.*;

import static eu.fbk.das.rs.challenges.ChallengeUtil.getPeriodScore;
import static eu.fbk.das.utils.Utils.*;

public class RecommendationSystemEvaluator extends ChallengesBaseTest {

    public RecommendationSystemEvaluator() {
        prod = true;
    }

    Map<String, Integer> res = new HashMap<>();

    Map<Integer, Map<String, List<Double>>> weekResult;

    int startW = 36;

    int endW = 66;

    @Test
    public void executeAll() {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        conf.put("GAMEID", ferrara20_gameid);

        Set<String> players = facade.getGamePlayers(conf.get("GAMEID"));

        analyzeChallengePlayers(players);
    }

    private void analyzeChallengePlayers(Set<String> players) {
        weekResult = new HashMap<>();

        for (String pId: players) {

            PlayerStateDTO player = facade.getPlayerState(conf.get("GAMEID"), pId);
            Map<String, Set<GameConcept>> st = player.getState();

            int startWeek = getStartWeek(player);
            Set<GameConcept> scores =  st.get("ChallengeConcept");
            for (GameConcept gc : scores) {
                ChallengeConcept chal = (ChallengeConcept) gc;
                String nm = chal.getName();

                String mName = chal.getModelName();
                // p(chal);

                int w = daysApart(new DateTime(chal.getStart()), parseDate("29/10/2018"));
                w = w / 7 +1;

                w-= startWeek;

                // don't consider players inactive that week
                if (!scoredGreenLeaves(player, chal.getStart()))
                    continue;


                if (nm.contains("survey") || nm.contains("initial") || nm.contains("recommend"))
                    continue;

                if (mName.contains("group") || mName.contains("repetitive") || mName.contains("absolute"))
                    continue;

                Map<String, Object> fields = chal.getFields();

                // add difficulty challenge
                double diff = (Double) fields.get("target") / (Double) fields.get("baseline");
                addWeekRes(w,"diff", diff);

                // choice
                addWeekRes(w,"choice", chal.isForced() ? 1 : 0);

                // completion
                addWeekRes(w,"compl", chal.isCompleted() ? 1 : 0);

                // analysis
                boolean c = chal.isCompleted();

                incr("tot", c);

                String ww = f("w%d", w);

                incr(ww, c);

                incr(chal.getOrigin(), c);

                incr(ww + chal.getOrigin(), c);

            }
        }

        showResult("diff");
        showResult("choice");
        showResult("compl");


        p("completion");
        val("tot");
        p("for week");
        for (int w = 54; w <= 60; w++ ) {
            val(f("w%d", w));
        }

        String[] orig = new String[]{"player", "rs"};

        p("acceptance");
        for (String s: orig)
            vall("tot", s);
        p("for week");
        for (int w = 54; w <= 60; w++ ) {
            for (String s: orig) {
                String ww = f("w%d", w);
                vall(ww, ww+ s);
            }
        }
    }

    private boolean scoredGreenLeaves(PlayerStateDTO player, Date dt) {
        return scoredGreenLeaves(player, new DateTime(dt));
    }

    private int getStartWeek(PlayerStateDTO player) {


        for (int i = startW; i < endW; i++) {
            DateTime stDt = parseDate("29/10/2018").plusDays(7*i);
            if (!(scoredGreenLeaves(player, stDt)))
                continue;

            return i;
        }

        return 68;
    }

    private void showResult(String k) {
        p(k);
        for (int wk = 0; wk <= endW - startW; wk++) {
            if (!weekResult.containsKey(wk))
                continue;
            List<Double> lr = weekResult.get(wk).get(k);
            double mean = 0;
            for (Double s: lr) {
                mean += s;
            }
            mean /= lr.size();
            pf("%d\t%.2f\n", wk, mean);
        }
    }

    private void addWeekRes(int w, String k, double v) {
        if (!weekResult.containsKey(w))
            weekResult.put(w, new HashMap<>());
        Map<String, List<Double>> weekR = weekResult.get(w);
        if (!weekR.containsKey(k))
            weekR.put(k, new ArrayList<>());
        weekR.get(k).add(v);
    }

    private void val(String s) {
        int t = res.get(s);
        int c = res.get(s + "-compl");
        pf("%s - %.2f (%d) \n", s, c*1.0/t, t);
    }

    private void vall(String s, String d) {
        int t = res.get(s);
        int c = res.get(d);
        pf("%s - %.2f (%d) \n", d, c*1.0/t, c);
    }

    private void incr(String f, boolean add) {
        incr_int(f);
        if (add)
            incr_int(f+ "-compl");
    }

    private void incr_int(String f) {
        if (!res.containsKey(f))
            res.put(f, 0);
            res.put(f, res.get(f) + 1);
    }

    private boolean scoredGreenLeaves(PlayerStateDTO player, Long day) {
        return scoredGreenLeaves(player, new DateTime(day));

    }

    private boolean scoredGreenLeaves(PlayerStateDTO player, DateTime day) {

            Double sc = getWeeklyContentMode(player, "green leaves", day);

            if (sc < 100)
                return false;

        return true;
    }

    public static Double getWeeklyContentMode(PlayerStateDTO cnt, String mode, DateTime execDate) {
        Set<GameConcept> scores =  cnt.getState().get("PointConcept");
        for (GameConcept gc : scores) {
            PointConcept pc = (PointConcept) gc;

            String m = pc.getName();
            if (!m.equals(mode))
                continue;

            return getPeriodScore(pc,"weekly", execDate);
        }

        return 0.0;
    }
}
