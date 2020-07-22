package eu.fbk.das.rs.challenges.generation;


import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.rs.utils.Utils;
import it.smartcommunitylab.model.PlayerStateDTO;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static eu.fbk.das.rs.utils.ArrayUtils.pos;
import static eu.fbk.das.rs.utils.Utils.p;
import static eu.fbk.das.rs.utils.Utils.pf;

public class RecommendationSystemTest extends ChallengesBaseTest {
/*
    @Test
    public void testForecast() {

        String pId = "24164"; // cfg.get("PLAYER_ID");

        DateTime date = Utils.stringToDate(cfg.get("DATE"));

        RecommendationSystem rs = new RecommendationSystem();
        rs.prepare(facade, date);

        RecommendationSystemChallengeGeneration rscg = new RecommendationSystemChallengeGeneration(cfg, null);

        PlayerStateDTO state = facade.getPlayerState(cfg.get("gameId"), pId);

        forecast(rscg, 10.0, 9.0);
        forecast(rscg, 10.0, 11.0);

        forecast(rscg, 10.0, 5.0);
        forecast(rscg, 10.0, 15.0);
    }


    private void forecast(RecommendationSystemChallengeGeneration rs, double current, double last) {
        pf("currentValue: %.2f, lastValue: %.2f\n", current, last);
        Double forecastValue = rs.forecastModeOld(current, last);
        pf("forecastValue: %.2f\n\n", forecastValue);
    }*/


    @Test
    public void testRecommendationForecast() {
        // preparazione
        String pId = "24164"; // cfg.get("PLAYER_ID");

        String d = conf.get("DATE");
        DateTime date;
        if ("".equals(d))
            date = new DateTime();
        else
            date = Utils.stringToDate(d);

        RecommendationSystem rs = new RecommendationSystem();

        PlayerStateDTO state = facade.getPlayerState(conf.get("GAME_ID"), pId);

        List<ChallengeExpandedDTO> l_cha = rs.assignForecast(state, date);

        List<ChallengeExpandedDTO> l_cha_2 = rs.assignLimit(3, state, date);
    }

    @Test
    public void loadTestChallenge() {
        // preparazione
        String pId = "122"; // cfg.get("PLAYER_ID");

        RecommendationSystem rs = new RecommendationSystem();
        RecommendationSystemChallengeGeneration rscg = new RecommendationSystemChallengeGeneration(rs);

        DateTime start = Utils.parseDateTime("26/10/2018 00:00");
        DateTime end = Utils.parseDateTime("28/10/2018 23:59");

        ChallengeExpandedDTO cdd = rscg.prepareChallangeImpr("Walk_Km");
        cdd.setModelName("absoluteIncrement");
        cdd.setData("target", 7);

        boolean state = facade.assignChallengeToPlayer(cdd, conf.get("GAME_ID"), pId);

        p(state);
    }

    @Test
    public void testRecommendation() {

        // preparazione
        String player_id = "24164"; // cfg.get("PLAYER_ID");
        // Set<String> allPlayerIds = new HashSet<>();
        // allPlayerIds.add(player_id);

        DateTime date = Utils.stringToDate(conf.get("DATE"));

        RecommendationSystem rs = new RecommendationSystem();

        List<ChallengeExpandedDTO> cnt = rs.recommend(player_id, null, null, null);
        for (ChallengeExpandedDTO cd : cnt) {
            p(cd);
        }
    }

    @Test
    // After change of PlayerStateDTO state adding PlayerStateDTO level, check of compatibility
    public void testGetPlayerState() {

        PlayerStateDTO c = facade.getPlayerState("5b7a885149c95d50c5f9d442", "8");
        p(c.getPlayerId());
    }


    @Test
    public void testEverything() {

        // preparazione


        // String player_id = "24164"; // cfg.get("PLAYER_ID");
        // String player_id = "24373";
        String player_id = "24336 ";


        Set<String> allPlayerIds = new HashSet<>();
        allPlayerIds.add(player_id);

        DateTime date = Utils.stringToDate(conf.get("DATE"));

        RecommendationSystem rs = new RecommendationSystem();

        RecommendationSystemStatistics statistics = new RecommendationSystemStatistics(rs);
        statistics.checkAndUpdateStats(date);

        PlayerStateDTO cnt = facade.getPlayerState(conf.get("GAME_ID"), player_id);

        // generazione della challenges
        List<ChallengeExpandedDTO> l_cha = rscg.generate(cnt, "Walk_Km", date);

        p("\n #### GENERATED #### \n");
        for (ChallengeExpandedDTO cha : l_cha) {
            p(cha);
        }

        // rivalutazione della challenges
        for (ChallengeExpandedDTO cha : l_cha) {
            rscv.valuate(cha);
        }

        p("\n #### VALUATED #### \n");

        for (ChallengeExpandedDTO cha : l_cha) {
            p(cha);
        }

        // filtraggio delle challenges
        List<ChallengeExpandedDTO> filtered_cha = rscf.filter(l_cha, cnt, date);

        p("\n #### FILTERED #### \n");

        for (ChallengeExpandedDTO cha : filtered_cha) {
            p(cha);
        }
    }

    @Test
    public void testPos() {
        double[] s = new double[]{1.0, 1.0, 3.0, 4.0, 5.0, 5.0, 6.0};
        for (double v : new double[]{1.0, 1.5, 3, 3.5, 5.0, 7.0}) {
            pf("%.2f \t-> pos %d\n", v, pos(v, s));
        }
    }

}
