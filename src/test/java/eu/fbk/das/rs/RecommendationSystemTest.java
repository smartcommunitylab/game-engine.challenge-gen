package eu.fbk.das.rs;

import static eu.fbk.das.rs.challenges.generation.RecommendationSystem.getChallengeWeek;
import static eu.fbk.das.utils.Utils.p;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemChallengeGeneration;
import it.smartcommunitylab.model.PlayerStateDTO;

public class RecommendationSystemTest extends ChallengesBaseTest {

    @Before
    public void test() {
        rs = new RecommendationSystem(conf);

        rscg = new RecommendationSystemChallengeGeneration(rs);
        rscg.prepare();
        facade = rs.facade;
    }

    @Test
    public void assignSurveyTest() {

        ChallengeExpandedDTO cha = rscg.prepareChallangeImpr("survey_prediction");
        cha.setStart(new DateTime().toDate());
        cha.setModelName("survey");
        cha.setData("surveyType", "evaluation");
        // cha.setData("surveyType", "prediction");

        cha.delData("counterName");
        cha.delData("periodName");

        // Assign to me


        // MIO ID
            // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAMEID"), "28540");

        boolean success = facade.assignChallengeToPlayer(cha, conf.get("GAMEID"), "3");

        // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAMEID"), "4");

        p(success);

       //  assignSingleChallenge("5d9353a3f0856342b2dded7f", "1024", "03/05/2022", "05/06/2022", "absoluteIncrement", "WalkKm", 2.0, 100.0);
    }


    private void assignSingleChallenge( String gameId, String pId, String start, String end, String model, String counter, Double target, Double score) throws ParseException {

        ChallengeExpandedDTO cha = rscg.prepareChallangeImpr(counter);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        cha.setStart(sdf.parse(start));
        cha.setEnd(sdf.parse(end));

        cha.setModelName(model);
        cha.setData("target", target);
        cha.setData("bonusScore", score);

        boolean state = facade.assignChallengeToPlayer(cha, gameId, pId);
        p(pId);
        p(state);
    }

    @Test
    public void assignEvaluation() {

        ChallengeExpandedDTO cha = rscg.prepareChallangeImpr("survey_prediction");

        cha.setModelName("survey");
        cha.setData("surveyType", "evaluation");
        // cha.setData("surveyType", "prediction");

        cha.delData("counterName");
        cha.delData("periodName");

        cha.setStart(new DateTime().toDate());

        Set<String> playerIds = facade.getGamePlayers(conf.get("GAMEID"));

        int w = 1;

        for (String pId: playerIds) {
            Map<String, Object> cs = facade.getCustomDataPlayer(conf.get("GAMEID"), pId);

            if (cs == null)
                continue;

            String exp = (String) cs.get("exp");

            if (exp == null)
                continue;

            int dw =  w - (Integer) cs.get("exp-start");
            if (dw < 6) continue;

            p(pId);

           // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAMEID"), pId);
            }
    }

    @Test
    public void assignRepetitiveTest() {

        String pId = "225";
        PlayerStateDTO st = facade.getPlayerState(conf.get("GAMEID"), pId);

        ChallengeExpandedDTO cha = rscg.getRepetitive(pId);

        // Assign to me
        boolean success = facade.assignChallengeToPlayer(cha, conf.get("GAMEID"), pId);
        // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAMEID"), "4");

        p(success);

    }

    @Test
    public void assignRecommendFriend() {

        String pId = "225";
        List<ChallengeExpandedDTO> s = new ArrayList<>();
        rs.assignRecommendFriend(pId, s);
        ChallengeExpandedDTO cha = s.get(0);

        // Assign to me
        boolean success = facade.assignChallengeToPlayer(cha, conf.get("GAMEID"), pId);
        // boolean success = facade.assignChallengeToPlayer(cha, cfg.get("GAMEID"), "4");

        p(success);

    }
}
