package eu.fbk.das.rs;

import eu.fbk.das.rs.challengeGeneration.RecommendationSystem;
import eu.fbk.das.rs.challengeGeneration.RecommendationSystemChallengeGeneration;
import eu.fbk.das.rs.challengeGeneration.RecommendationSystemConfig;
import eu.fbk.das.rs.challengeGeneration.RecommendationSystemStatistics;
import eu.fbk.das.rs.sortfilter.RecommendationSystemChallengeFilteringAndSorting;
import eu.fbk.das.rs.valuator.RecommendationSystemChallengeValuator;
import eu.trentorise.challenge.BaseTest;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static eu.fbk.das.rs.ArrayUtils.pos;
import static eu.fbk.das.rs.Utils.p;
import static eu.fbk.das.rs.Utils.pf;

public class RecommendationSystemTest extends BaseTest {

    private GamificationEngineRestFacade facade;
    private RecommendationSystemConfig cfg;
    private RecommendationSystemChallengeValuator rscv;
    private RecommendationSystemChallengeGeneration rscg;
    private RecommendationSystemChallengeFilteringAndSorting rscf;

    private LocalDate now;

    @Before
    public void setup() {
        facade = new GamificationEngineRestFacade(HOST,
                USERNAME, PASSWORD);
        cfg = new RecommendationSystemConfig();
        now = new LocalDate();

        rscv = new RecommendationSystemChallengeValuator(cfg);

        rscg = new RecommendationSystemChallengeGeneration(cfg);

        rscf = new RecommendationSystemChallengeFilteringAndSorting(cfg);
    }

    @Test
    public void testForecast() {

        String pId = "24164"; // cfg.get("PLAYER_ID");

        DateTime date = Utils.stringToDate(cfg.get("DATE"));

        RecommendationSystem rs = new RecommendationSystem();
        rs.setFacade(facade);
        rs.prepare(cfg, date);

        forecast(rs, 10.0, 9.0);
        forecast(rs, 10.0, 11.0);

        forecast(rs, 10.0, 5.0);
        forecast(rs, 10.0, 15.0);
    }

    private void forecast(RecommendationSystem rs, double current, double last) {
        pf("currentValue: %.2f, lastValue: %.2f\n", current, last);
        Double forecastValue = rs.forecastMode(current, last);
        pf("forecastValue: %.2f\n\n", forecastValue);
    }

    @Test
    public void testRecommendationForecast() {
        // preparazione
        String pId = "24164"; // cfg.get("PLAYER_ID");

        DateTime date = Utils.stringToDate(cfg.get("DATE"));

        RecommendationSystem rs = new RecommendationSystem();
        rs.setFacade(facade);
        rs.prepare(cfg, date);

        Content state = facade.getPlayerState(cfg.get("GAME_ID"), pId);

        rs.recommendForecast(state, date);
    }

    @Test
    public void testRecommendation() {

        // preparazione
        String player_id = "24164"; // cfg.get("PLAYER_ID");
        // Set<String> allPlayerIds = new HashSet<>();
        // allPlayerIds.add(player_id);

        DateTime date = Utils.stringToDate(cfg.get("DATE"));

        RecommendationSystem rs = new RecommendationSystem();
        rs.setFacade(facade);
        rs.prepare(cfg, date);

        List<ChallengeDataDTO> cnt = rs.recommend(player_id, date);

        for (ChallengeDataDTO cd : cnt) {
            p(cd);
        }
    }

    @Test
    // After change of player state adding player level, check of compatibility
    public void testGetPlayerState() {

        facade = new GamificationEngineRestFacade(cfg.get("HOST"),
                cfg.get("USERNAME"), cfg.get("PASSWORD"));
        Content c = facade.getPlayerState("5b7a885149c95d50c5f9d442", "8");

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

        DateTime date = Utils.stringToDate(cfg.get("DATE"));

        RecommendationSystemStatistics statistics = new RecommendationSystemStatistics(cfg);
        statistics.checkAndUpdateStats(facade, date, cfg.getDefaultMode());

        rscv.prepare(statistics);
        rscf.prepare(statistics);

        Content cnt = facade.getPlayerState(cfg.get("GAME_ID"), player_id);

        // generazione della challenges
        List<ChallengeDataDTO> l_cha = rscg.generate(cnt, "Walk_Km", date);

        p("\n #### GENERATED #### \n");
        for (ChallengeDataDTO cha : l_cha) {
            p(cha);
        }

        // rivalutazione della challenges
        for (ChallengeDataDTO cha : l_cha) {
            rscv.valuate(cha);
        }

        p("\n #### VALUATED #### \n");

        for (ChallengeDataDTO cha : l_cha) {
            p(cha);
        }

        // filtraggio delle challenges
        List<ChallengeDataDTO> filtered_cha = rscf.filter(l_cha, cnt, date);

        p("\n #### FILTERED #### \n");

        for (ChallengeDataDTO cha : filtered_cha) {
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
