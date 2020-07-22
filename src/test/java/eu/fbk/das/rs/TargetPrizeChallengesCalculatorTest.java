package eu.fbk.das.rs;

import eu.fbk.das.GamificationEngineRestFacade;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.GamificationConfig;
import it.smartcommunitylab.model.PlayerStateDTO;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static eu.fbk.das.rs.challenges.ChallengeUtil.getLevel;
import static eu.fbk.das.rs.utils.Utils.wf;

public class TargetPrizeChallengesCalculatorTest extends ChallengesBaseTest {

    private DateTime now;

    private TargetPrizeChallengesCalculator tpcc;

    private boolean header = true;

    private String[] key = new String[] {"player1", "player2"};
    private String[] key2 = new String[] {"bas", "tgt", "prz"};
    private String gameId;
    private RecommendationSystem rs;

    @Test
    public void test() {

        String pId_1 = "127";
        String pId_2 = "18375";

        Map<String, Double> res_6 = tpcc.targetPrizeChallengesCompute("27300", "1069", "Walk_Km", "groupCooperative");

        Map<String, Double> res_5 = tpcc.targetPrizeChallengesCompute("27300", "27465", "Bike_Km", "groupCompetitiveTime");

        Map<String, Double> res_4 = tpcc.targetPrizeChallengesCompute("11126", "127", "Bike_Km", "groupCompetitiveTime");

        Map<String, Double> res_3 = tpcc.targetPrizeChallengesCompute("19092", "24288", "Walk_Km", "groupCompetitiveTime");

        Map<String, Double> res_1 = tpcc.targetPrizeChallengesCompute(pId_1, pId_2, "Walk_Km", "groupCompetitiveTime");

        Map<String, Double> res_2 = tpcc.targetPrizeChallengesCompute(pId_1, pId_2, "Walk_Km", "groupCooperative");
    }

    @Test
    public void generation() throws IOException {

        prepare();

        // READ ALL THE PLAYERS
        Set<String> players = facade.getGamePlayers(gameId);
        List<String> playersToConsider = new ArrayList<String>(players.size());
        int ix = 0;
        for (String pId: players) {
             if (ix++ > 100)
               break;
            PlayerStateDTO p = facade.getPlayerState(gameId, pId);
            int lvl = getLevel(p);
            if (lvl >= 3)
                playersToConsider.add(pId);
        }

        BufferedWriter w = new BufferedWriter(new FileWriter("challenges-group.csv"));

        for (int i = 0; i < playersToConsider.size(); i++) {
            for (int j = i+1; j < playersToConsider.size(); j++) {
                for (String type: new String[] {"groupCompetitiveTime", "groupCooperative"}) {
                    for (String counter: new String[] {"Bike_Km", "Walk_Km", "green leaves"}) {
                        execute(w, playersToConsider.get(i), playersToConsider.get(j), type, counter);
                    }
                }
            }
        }

        w.close();


    }

    @Before
    public void prepare() {
        conf = new GamificationConfig().extract();

        now = new DateTime();

        facade = new GamificationEngineRestFacade(conf.get("HOST"),
                conf.get("USERNAME"), conf.get("PASSWORD"));

        gameId = conf.get("GAME_ID");

        rs = new RecommendationSystem(conf);

        tpcc = new TargetPrizeChallengesCalculator();
        tpcc.prepare(rs, gameId);

    }

    private void execute(BufferedWriter w, String pId_1, String pId_2, String type, String counter) throws IOException {
        Map<String, Double> res = tpcc.targetPrizeChallengesCompute(pId_1, pId_2, counter, type);
        if (header) {
            wf(w, "p1,p2,type,counter,target");
            for (String k1: key )
                for (String k2: key2)
                    wf(w, ",%s_%s", k1, k2);

            wf(w, "\n");
            header = false;
        }
        wf(w,"%s,%s,%s,%s,%.2f", pId_1, pId_2, type, counter, res.get("target"));
        for (String k1: key )
            for (String k2: key2)
            wf(w,",%.2f", res.get(k1+"_" +k2));

        wf(w, "\n");
        w.flush();
    }




}