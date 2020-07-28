package eu.fbk.das.rs;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.ChallengeConcept;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static eu.fbk.das.rs.challenges.ChallengeUtil.getPeriodScore;
import static eu.fbk.das.utils.Utils.*;

public class GroupAssignedChallengesCheck extends ChallengesBaseTest {

    @Test
    public void check() {

        PlayerStateDTO player = facade.getPlayerState(conf.get("GAMEID"), "28593");

        Set<GameConcept> scores =  player.getState().get("ChallengeConcept");
        for (GameConcept gc : scores) {
            ChallengeConcept chal = (ChallengeConcept) gc;

            String nm = chal.getModelName();

            int w = daysApart(new DateTime(chal.getStart()), parseDate("29/10/2018"));
            w = w / 7 +1;

            p(w);
            if (w < 59) continue;

            p(chal);
            p(nm);

        }


    }

    @Test
    public void execute() {

        Map<String, Integer> cont = new HashMap<>();

        for (String pId: facade.getGamePlayers(conf.get("GAMEID"))) {
            PlayerStateDTO player = facade.getPlayerState(conf.get("GAMEID"), pId);
            Set<GameConcept> scores =  player.getState().get("ChallengeConcept");
            for (GameConcept gc : scores) {
                ChallengeConcept chal = (ChallengeConcept) gc;

                String nm = chal.getModelName();

                if (!nm.contains("group"))
                    continue;

                int w = daysApart(new DateTime(chal.getStart()), parseDate("29/10/2018"));
                w = w / 7 +1;

                //if (w == 61)
                //     continue;

                if (chal.getState().equals("ASSIGNED"))
                    continue;

                if (!scoredGreenLeaves(player, chal.getStart()))
                    continue;

                incr(cont, f("%s-%d-%s", chal.getOrigin(), w, "tot"), true);

                incr(cont, f("%s-%d-%s", chal.getOrigin(), w, "compl"), chal.getState().equals("COMPLETED"));

                p(chal.getState());


            }
        }

        p("Human readable");

        for (String wk: new String[] {"54", "55", "56", "57", "58", "59", "60", "61"})
            for (String or: new String[] {"player", "gca"}) {
                int t = cont.get(f("%s-%s-tot", or, wk));
                int c = cont.get(f("%s-%s-compl", or, wk));
                pf("%s - %s - %.2f (%d) \n", wk, or, c*1.0/t, t);
            }

        p(cont.keySet());

        p("Dataset");

        for (String wk: new String[] {"54", "55", "56", "57", "58", "59", "60", "61", "62", "63"}) {
            pf("%s", wk);
            for (String or : new String[]{"player", "gca"}) {
                int t = cont.get(f("%s-%s-tot", or, wk));
                int c = cont.get(f("%s-%s-compl", or, wk));
                pf("\t%.2f\t%d", c * 1.0 / t, t);
            }
            p("");

        }

        p(cont.keySet());

    }

    private boolean scoredGreenLeaves(PlayerStateDTO player, Date day) {

        Set<GameConcept> scores =  player.getState().get("PointConcept");
        for (GameConcept gc : scores) {
            PointConcept pc = (PointConcept) gc;

            if (!pc.getName().equals("green leaves"))
                continue;

            Double sc = getPeriodScore(pc,"weekly", new DateTime(day));

            if (sc < 100)
                return false;
        }

        return true;
    }

    private void incr(Map<String, Integer> cont, String f, boolean add) {
        if (!cont.containsKey(f))
            cont.put(f, 0);
        if (add)
        cont.put(f, cont.get(f) + 1);
    }

}
