package eu.trentorise.game.managers;

import eu.fbk.das.GamificationEngineRestFacade;
import eu.fbk.das.api.exec.RecommenderSystemGroup;
import eu.fbk.das.api.exec.RecommenderSystemWeekly;
import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.rs.GroupChallengesAssigner;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static eu.fbk.das.utils.Utils.p;

public class ScenarioTest extends ChallengesBaseTest {

    public ScenarioTest() {
        prod = false;
    }

    @Test
    // TEST SINGLE GENERATION
    public void scenario2() throws Exception {
     //   Set<String> players = facade.getGamePlayers(gameId);
     //   p(players);


        // u_5eae76b8-2828-4932-a821-a16a4811a9c7 - chin8
        // u_75ff09f4b52a4f9291bb8c9412f7f101 - antbucc,
        // u_84d11a2769e1479cb43c23da416e29e7 - TaK,
        // u_affaea06d6014e7cb58f411817a5a82b - nzee,
        // u_c82b2114e2a745f7b0b7e70bc22ec0da - annamelie,
        // u_d995fe2ae909486399d89861aef2f450 - mau,
        // u_e15f28792c574f318ae808790974e43b - rk,
        // u_f89ebf548d8c48bcb367a73e0c18fbfa - ilnori

        Set<String> modelTypes = new HashSet<>();
        modelTypes.add(ChallengesConfig.BIKE_KM);
        modelTypes.add(ChallengesConfig.WALK_KM);
        modelTypes.add(ChallengesConfig.GREEN_LEAVES);
       // modelTypes.add(ChallengesConfig.BUS_KM);
        // modelTypes.add(ChallengesConfig.TRAIN_KM);

        RecommenderSystemWeekly rsw = new RecommenderSystemWeekly();
        List<ChallengeExpandedDTO> res = rsw.go(conf, "all", modelTypes, null);

        Map<String, List<ChallengeExpandedDTO>> cache = new HashMap<>();

        for (ChallengeExpandedDTO cha: res) {
            p(cha);
        }

    }

    @Test
    // TEST GROUP GENERATION
    public void scenario3() throws Exception {
        Set<String> players = facade.getGamePlayers(gameId);

        // String type = "groupCompetitiveTime";
        String type = "groupCooperative";
        //  String type = "groupCompetitivePerformance";

        Set<String> modeList = new HashSet<String>(Arrays.asList(ChallengesConfig.WALK_KM,ChallengesConfig.BIKE_KM,ChallengesConfig.GREEN_LEAVES));

        Map<String, Object> config = new HashMap<>();

        GroupChallengesAssigner gca = new GroupChallengesAssigner(rs);
        List<GroupExpandedDTO> chas = gca.execute(players, modeList, type, config);
        for (GroupExpandedDTO cha: chas)
            p(cha);
    }


    @Test
    // test per capire come mai "Un utente con due sfide di coppia. ID utente 29590"
    public void checkCoopGeneration() {
        // String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        // Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
        // RecommenderSystemAPI api = new RecommenderSystemImpl();
        conf.put("GAMEID", "640ef0a3d82bd2057035f94e");
        conf.put("execDate", "2020-11-06");
        RecommenderSystemGroup rsw = new RecommenderSystemGroup();
        String challengeType = "groupCooperative";
        rsw.go(conf, "all", challengeType, null);
    }

    @Test
    // TEST with weeklyStrategy
    public void testHSC() throws Exception {

        Set<String> modelTypes = new HashSet<>();
        modelTypes.add(ChallengesConfig.BIKE_KM);
        modelTypes.add(ChallengesConfig.WALK_KM);

        Map<String, String> weeklyStrategy = new HashMap<>();
        weeklyStrategy.put("0", "absoluteMbility");

        RecommenderSystemWeekly rsw = new RecommenderSystemWeekly();
        List<ChallengeExpandedDTO> res = rsw.go(conf, "all", modelTypes, weeklyStrategy);

        for (ChallengeExpandedDTO cha: res) {
            p(cha);
        }
    }

}
