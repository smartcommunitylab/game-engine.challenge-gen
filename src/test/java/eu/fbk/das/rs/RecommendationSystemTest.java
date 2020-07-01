package eu.fbk.das.rs;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemChallengeGeneration;
import it.smartcommunitylab.model.PlayerStateDTO;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static eu.fbk.das.rs.utils.Utils.p;

public class RecommendationSystemTest extends ChallengesBaseTest {

    @Before
    public void test() {
        // prod
        cfg.put("HOST", "https://tn.smartcommunitylab.it/gamification2/");
        cfg.put("GAME_ID", "5d9353a3f0856342b2dded7f");

        rs = new RecommendationSystem(cfg);

        rscg = new RecommendationSystemChallengeGeneration(rs);
        rscg.prepare(new DateTime());
        facade = rs.facade;
    }

    @Test
    public void assignSurveyTest() {

        ChallengeExpandedDTO cha = rscg.prepareChallange("survey_prediction");
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


    private void assignReccomendation(String l, String pId) {
        ChallengeExpandedDTO cha = rscg.prepareChallange(l, "Recommendations");
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

        ChallengeExpandedDTO cha = rscg.prepareChallange("survey_prediction");

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

        String pId = "225";
        PlayerStateDTO st = facade.getPlayerState(cfg.get("GAME_ID"), pId);

        ChallengeExpandedDTO cha = rscg.getRepetitive(pId);

        // Assign to me
        boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), pId);
        // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), "4");

        p(success);

    }

    @Test
    public void assignRecommendFriend() {

        String pId = "225";
        List<ChallengeExpandedDTO> s = new ArrayList<>();
        rs.assignRecommendFriend(pId, s);
        ChallengeExpandedDTO cha = s.get(0);

        // Assign to me
        boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), pId);
        // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAME_ID"), "4");

        p(success);

    }
}
