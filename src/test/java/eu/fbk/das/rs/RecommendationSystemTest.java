package eu.fbk.das.rs;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.Player;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.*;

import static eu.fbk.das.rs.utils.Utils.p;

public class RecommendationSystemTest extends ChallengesBaseTest {

    @Test
    public void assignSurveyTest() {

        // prod
        cfg.put("HOST", "https://tn.smartcommunitylab.it/gamification2/");
         facade = new GamificationEngineRestFacade(cfg.get("HOST"),
                 cfg.get("USERNAME"), cfg.get("PASSWORD"));

        cfg.put("GAME_ID", "5d9353a3f0856342b2dded7f");

        rscg.prepare(new DateTime(), new RecommendationSystem(conf.get("HOST"), conf.get("USER"), conf.get("PASS")));
        ChallengeDataDTO cha = rscg.prepareChallange("survey_prediction");


        cha.setStart(new DateTime());
        cha.setModelName("survey");
        cha.setData("surveyType", "evaluation");
        // cha.setData("surveyType", "prediction");

        cha.delData("counterName");
        cha.delData("periodName");

        // Assign to me


        // MIO ID
            // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), "28540");

        boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), "3");

        // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), "4");

        p(success);

    }

    @Test
    public void assignReccomendationAll() {
        String l = "new_recommend";

        cfg.put("HOST", "https://tn.smartcommunitylab.it/gamification2/");
        facade = new GamificationEngineRestFacade(cfg.get("HOST"),
                cfg.get("USERNAME"), cfg.get("PASSWORD"));

        cfg.put("GAME_ID", "5d9353a3f0856342b2dded7f");

        rscg.prepare(new DateTime(), rs);

        assignReccomendation(l, "19092");

        Map<String, Player> res = facade.readGameState(cfg.get("GAME_ID"));
        for (String pId: res.keySet()) {

            assignReccomendation(l, pId);
        }
    }

    private void assignReccomendation(String l, String pId) {
        ChallengeDataDTO cha = rscg.prepareChallange(l, "Recommendations");
        cha.setStart(new DateTime());
        cha.setModelName("absoluteIncrement");
        cha.setData("target", 1.0);
        cha.setData("bonusScore", 100.0);

        boolean state = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), pId);
        p(pId);
        p(state);
    }

    @Test
    public void assignEvaluation() {

        cfg.put("HOST", "https://tn.smartcommunitylab.it/gamification2/");
        facade = new GamificationEngineRestFacade(cfg.get("HOST"),
                cfg.get("USERNAME"), cfg.get("PASSWORD"));

        cfg.put("GAME_ID", "5d9353a3f0856342b2dded7f");

        rscg.prepare(new DateTime(), new RecommendationSystem(conf.get("HOST"), conf.get("USER"), conf.get("PASS")));
        ChallengeDataDTO cha = rscg.prepareChallange("survey_prediction");

        cha.setModelName("survey");
        cha.setData("surveyType", "evaluation");
        // cha.setData("surveyType", "prediction");

        cha.delData("counterName");
        cha.delData("periodName");

        cha.setStart(new DateTime());

        Set<String> playerIds = facade.getGamePlayers(cfg.get("GAME_ID"));

        int w = this.rs.getChallengeWeek(new DateTime());

        for (String pId: playerIds) {
            Map<String, Object> cs = facade.getCustomDataPlayer(cfg.get("GAME_ID"), pId);

            if (cs == null)
                continue;

            String exp = (String) cs.get("exp");

            if (exp == null)
                continue;

            int dw =  w - (Integer) cs.get("exp-start");
            if (dw < 6) continue;

            p(pId);

           // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), pId);
            }
    }

    @Test
    public void assignRepetitiveTest() {

        cfg.put("HOST", "https://dev.smartcommunitylab.it/gamification-v3/");
        facade = new GamificationEngineRestFacade(cfg.get("HOST"),cfg.get("USERNAME"), cfg.get("PASSWORD"));

        rscg.setFacade(facade);

        cfg.put("GAME_ID", "5d9353a3f0856342b2dded7f");

        String pId = "225";
        Player st = facade.getPlayerState(cfg.get("GAME_ID"), pId);

        rscg.prepare(new DateTime().minusDays(7), new RecommendationSystem(conf.get("HOST"), conf.get("USER"), conf.get("PASS")));
        ChallengeDataDTO cha = rscg.getRepetitive(pId);

        // Assign to me
        boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), pId);
        // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), "4");

        p(success);

    }

    @Test
    public void assignRecommendFriend() {

        cfg.put("HOST", "https://dev.smartcommunitylab.it/gamification-v3/");
        facade = new GamificationEngineRestFacade(cfg.get("HOST"),cfg.get("USERNAME"), cfg.get("PASSWORD"));

        RecommendationSystem rs = new RecommendationSystem(conf.get("HOST"), conf.get("USER"), conf.get("PASS"));
        rs.prepare(facade, new DateTime(), cfg.get("HOST"));

        String pId = "225";
        List<ChallengeDataDTO> s = new ArrayList<>();
        rs.assignRecommendFriend(pId, s);
        ChallengeDataDTO cha = s.get(0);

        // Assign to me
        boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), pId);
        // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), "4");

        p(success);

    }
}
