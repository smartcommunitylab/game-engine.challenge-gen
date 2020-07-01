package eu.fbk.das.rs;

import eu.fbk.das.GamificationEngineRestFacade;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.ChallengeConcept;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.*;

import static eu.fbk.das.rs.challenges.ChallengeUtil.getPeriodScore;
import static eu.fbk.das.rs.utils.Utils.*;

public class RecommendationSystemEvaluator extends ChallengesBaseTest {

    Map<String, Integer> res = new HashMap<>();

    Map<Integer, Map<String, List<Double>>> weekResult;

    int startW = 54;

    int endW = 66;

    Set<String> controlPlayers = new HashSet<String>(Arrays.asList(
            "127", "1160", "1616", "2406", "19092", "24060", "24336", "24347", "24465", "24559", "25548", "25683", "26106",
            "27300", "27350", "27499", "27587", "27695", "27927", "27980", "28011", "28063", "28150", "28393", "28408", "28417",
            "28448", "28453", "28467", "28502", "28505", "28509", "28519", "28538", "28546", "28547", "28566", "28582", "28593", "28607"));

    Set<String> treatmentPlayers = new HashSet<String>(Arrays.asList(
            "1069", "11126", "23779", "24349", "24471", "24476", "24599", "24650", "24861", "24896", "25477", "25618", "25744",
            "25791", "25801", "25883", "26613", "26646", "26796", "27232", "27276", "27287", "27319", "27367", "27374", "27387",
            "27443", "27592", "27718", "27852", "27905", "28464", "28473", "28474", "28487", "28504", "28543", "28548", "28588", "28606"));

    String[] modes = new String[]{"Walk_Km", "Bike_Km", "green leaves", "Bus_Km", "Train_Km", "Bus_Trips", "Train_Trips"};

    @Test
    public void executeAll() {

        cfg.put("HOST", "https://tn.smartcommunitylab.it/gamification2/");
        facade = new GamificationEngineRestFacade(cfg.get("HOST"),
                cfg.get("USERNAME"), cfg.get("PASSWORD"));

        Set<String> players = facade.getGamePlayers(cfg.get("GAME_ID"));

        analyzeChallengePlayers(players);

    }

    @Test
    public void executeControl() {

        cfg.put("HOST", "https://tn.smartcommunitylab.it/gamification2/");
        facade = new GamificationEngineRestFacade(cfg.get("HOST"),
                cfg.get("USERNAME"), cfg.get("PASSWORD"));

        analyzeImprovementPlayers(controlPlayers);

        analyzePerformancesPlayers(controlPlayers);

        analyzeChallengePlayers(controlPlayers);

    }

    @Test
    public void executeTreatment() {

        cfg.put("HOST", "https://tn.smartcommunitylab.it/gamification2/");
        facade = new GamificationEngineRestFacade(cfg.get("HOST"),
                cfg.get("USERNAME"), cfg.get("PASSWORD"));

        analyzeImprovementPlayers(treatmentPlayers);

        analyzePerformancesPlayers(treatmentPlayers);

        analyzeChallengePlayers(treatmentPlayers);

    }

    private void analyzeImprovementPlayers(Set<String> players) {
        weekResult = new HashMap<>();

        for (String pId: players) {
            PlayerStateDTO player = facade.getPlayerState(cfg.get("GAME_ID"), pId);
            Integer ix = null;
            Map<String, Double> old = new HashMap<>();
            for (int w = startW; w <= endW; w++ ) {

                DateTime stDt = parseDate("29/10/2018").plusDays(w * 7);

                if (ix != null) ix++;

                if (!(scoredGreenLeaves(player, stDt)))
                    continue;
                else if (ix == null) ix = 0;

                for (String mode: modes) {
                    double newV = getWeeklyContentMode(player, mode, stDt);
                    if (old.containsKey(mode)) {
                        double oldV = old.get(mode);
                        if (oldV == 0) continue;
                        addWeekRes(ix, mode, newV / oldV);
                    }
                    old.put(mode, newV);
                }
            }
        }

        for (String mode: modes)
            showResult(mode);

    }

    private void analyzePerformancesPlayers(Set<String> players) {
        weekResult = new HashMap<>();

        for (String pId: players) {
            PlayerStateDTO player = facade.getPlayerState(cfg.get("GAME_ID"), pId);
            Integer ix = null;
            for (int w = startW; w <= endW; w++ ) {

                DateTime stDt = parseDate("29/10/2018").plusDays(w * 7);

                if (ix != null) ix++;

                if (!(scoredGreenLeaves(player, stDt)))
                    continue;
                else if (ix == null) ix = 0;

                for (String mode: modes)
                    addWeekRes(ix,mode, getWeeklyContentMode(player, mode, stDt));
            }
        }

        for (String mode: modes)
            showResult(mode);

    }

    private void analyzeChallengePlayers(Set<String> players) {
        weekResult = new HashMap<>();

        for (String pId: players) {

            PlayerStateDTO player = facade.getPlayerState(cfg.get("GAME_ID"), pId);
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
