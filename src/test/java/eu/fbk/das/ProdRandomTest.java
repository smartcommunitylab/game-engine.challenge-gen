package eu.fbk.das;

import eu.fbk.das.api.RecommenderSystemImpl;
import eu.fbk.das.api.exec.RecommenderSystemGroup;
import eu.fbk.das.api.exec.RecommenderSystemTantum;
import eu.fbk.das.api.exec.RecommenderSystemWeekly;
import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.RecommendationSystemTest;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.utils.Utils;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.ChallengeConcept;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;
import org.joda.time.DateTime;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static eu.fbk.das.rs.challenges.ChallengeUtil.getPeriodScore;
import static eu.fbk.das.utils.Utils.*;

public class ProdRandomTest extends ChallengesBaseTest {

    public ProdRandomTest() {
        prod = true;
    }

    @Test
    public void checkAssignment() {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        p(ferrara20_gameid);
        // Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
        // RecommenderSystemAPI api = new RecommenderSystemImpl();
        conf.put("GAMEID", ferrara20_gameid);
        conf.put("execDate", "2022-03-08");

        // List<String> idList = Arrays.asList("28540", "19092", "30453", "27300", "4055", "Raman", "4");

        List<String> idList = Arrays.asList("33324", "31548", "29473");

        for (String playerId: idList) {
            String typePoi = "airbreak";

            ChallengeExpandedDTO cha = new ChallengeExpandedDTO();
            cha.setModelName("visitPointInterest");
            cha.setInstanceName(String.format("visitPointInterest_%s_%s", playerId, typePoi));

            cha.setData("periodName", "weekly");

            cha.setData("bonusScore", 100.0);
            cha.setData("bonusPointType", "green leaves");

            cha.setData("target", 2);
            cha.setData("typePoi", typePoi);

            cha.setStart(convert(LocalDate.now().minusDays(3)));
            cha.setEnd(convert(LocalDate.now().plusDays(3)));

            RecommendationSystem rs = new RecommendationSystem();
            rs.facade.assignChallengeToPlayer(cha, ferrara20_gameid, playerId);

            pf("Assegnata sfida %s\n", cha.getInstanceName());
        }
    }

    public Date convert(LocalDate dateToConvert) {
        return java.sql.Date.valueOf(dateToConvert);
    }

    @Test
    public void check_ferrara() {

        String gameId = "5edf5f7d4149dd117cc7f17d";

        Integer count = 0;

        String pId = "29473";

        PlayerStateDTO pl = facade.getPlayerState(gameId, pId);

        Map<ChallengeConcept, Date> cache = new HashMap<>();

        Set<GameConcept> scores = pl.getState().get("ChallengeConcept");
        if (scores != null) {

            for (GameConcept gc : scores) {
                ChallengeConcept cha = (ChallengeConcept) gc;
                String nm = cha.getName();
                if (!nm.contains("visitPointInterest"))
                    continue;
                p(nm);
                p(cha);
            }
        }
    }


    @Test
    public void checkWeeksTests() {

        String gameId = "5edf5f7d4149dd117cc7f17d";

        Integer count = 0;

        Set<String> pIds = facade.getGamePlayers(gameId);
        for (String pId : pIds) {
            PlayerStateDTO pl = facade.getPlayerState(gameId, pId);

            Map<ChallengeConcept, Date> cache = new HashMap<>();

            Set<GameConcept> scores =  pl.getState().get("ChallengeConcept");
            if (scores == null) continue;

            for (GameConcept gc : scores) {
                ChallengeConcept cha = (ChallengeConcept) gc;
                String nm = cha.getName();
                if (!nm.startsWith("w7"))
                    continue;

                if (nm.startsWith("w79")) {
                    count += 1;
                    p(cha);
                    p(nm);
                    p(pId);
                }
            }
        }

        p(count);
    }

    @Test
    public void randTests() {

        Set<String> pIds = facade.getGamePlayers(conf.get("GAMEID"));

        p(String.join(", ", pIds));

        // for (String pId : new String[]{"24650"}) {
            for (String pId : pIds) {

            int count = 0;

            PlayerStateDTO pl = facade.getPlayerState(conf.get("GAMEID"), pId);

            Map<ChallengeConcept, Date> cache = new HashMap<>();

            Set<GameConcept> scores =  pl.getState().get("ChallengeConcept");
                for (GameConcept gc : scores) {
                    ChallengeConcept cha = (ChallengeConcept) gc;
                cache.put(cha, cha.getStart());
            }

            cache = Utils.sortByValues(cache);

            for ( ChallengeConcept cha : cache.keySet()) {
                Date start = cache.get(cha);
                pf("%s %s %s %s\n", new DateTime(start), cha.getName(), cha.getModelName(), cha.getFields().get("counterName"));
                if (cha.getModelName().equals("groupCompetitivePerformance")) {
                    count++;
                    p(cha.getFields());
                    p(cha.getFields().get("challengePointConceptName"));

                    if (count >= 2)
                        p("ciaoa");
                }


            }

            p(pl.getPlayerId());
            p("");
        }
    }


    int map(long x, long in_min, long in_max, long out_min, long out_max)
    {
        return (int) ((x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min);
    }

    public void dir(int dir) {

        int min_speed = 100;
        int max_speed = 200;

        int speed_l;
        int speed_r;


        if (dir < 45) {
            speed_l = map(dir, 0, 45, max_speed, min_speed);
            speed_r = max_speed;
        } else if (dir < 90) {
            speed_l = map(dir - 45, 0, 45, -min_speed, -max_speed);
            speed_r = max_speed;
        } else if (dir < 135) {
            speed_l = -max_speed;
            speed_r = map(dir - 90, 0, 45, max_speed, min_speed);
        } else if (dir < 180) {
            speed_l = -max_speed;
            speed_r = map(dir - 135, 0, 45, -min_speed, -max_speed);
        } else if (dir < 225) {
            speed_l = map(dir - 180, 0, 45, -max_speed, -min_speed);
            speed_r = -max_speed;
        } else if (dir < 270) {
            speed_l = map(dir - 225, 0, 45, min_speed, max_speed);
            speed_r = -max_speed;
        } else if (dir < 315) {
            speed_l = max_speed;
            speed_r = map(dir - 270, 0, 45, -max_speed, -min_speed);
        } else {
            speed_l = max_speed;
            speed_r = map(dir - 315, 0, 45, min_speed, max_speed);
        }

        pf("dir: %d, l: %d, r: %d \n", dir, speed_l, speed_r);

    }

    @Test
    public void testDir() {
        int x;
        for (x = 0; x < 360; x += 10) {
            dir(x);
        }
    }


    @Test
    public void checkChallenges88() {

        String gameId = conf.get("GAMEID");

        Set<String> pIds = facade.getGamePlayers(gameId);
        for(String pId: pIds) {
            // PlayerStateDTO state = facade.getPlayerState(gameId, pId);
            if (existsChallenge(gameId, pId, "w93_")) continue;
            p(pId);
        }
    }

    private boolean existsChallenge(String gameId, String pId, String l) {
        List<it.smartcommunitylab.model.ChallengeConcept> currentChallenges = facade.getChallengesPlayer(gameId, pId);
        for (it.smartcommunitylab.model.ChallengeConcept cha: currentChallenges) {
            p(cha.getName());
            if (cha.getName().contains(l))
                return true;
        }
        return  false;
    }

    @Test
    public void checkChallengesWeek() throws ParseException {

        String gameId = conf.get("GAMEID");
p(gameId);
        Set<String> pIds = facade.getGamePlayers(gameId);
        for(String pId: pIds) {
            String sDate ="24/07/2020";
            Date date = new SimpleDateFormat("dd/MM/yyyy").parse(sDate);

            PlayerStateDTO state = facade.getPlayerState(gameId, pId);

            if (!checkIfPlaying(state, date)) continue;

            if (!existsWeekChallenge(gameId, pId, date)) continue;
            pf("%s \n", pId);
        }
    }

    private boolean existsWeekChallenge(String gameId, String pId, Date l) {


        List<it.smartcommunitylab.model.ChallengeConcept> currentChallenges = facade.getChallengesPlayer(gameId, pId);
        for (it.smartcommunitylab.model.ChallengeConcept cha: currentChallenges) {
            if (cha.getOrigin() == null || !cha.getOrigin().equals("gca")) continue;

            if (!cha.getStart().after(l)) continue;

                return true;
        }
        return  false;
    }

    private boolean checkIfPlaying(PlayerStateDTO player, Date date) {

        boolean active = false;

        Set<GameConcept> scores =
                player.getState().get("PointConcept");

        for (GameConcept gc : scores) {
            PointConcept pc = (PointConcept) gc;
            if (!pc.getName().equals("green leaves"))
                continue;

            Double sc = getPeriodScore(pc, "weekly", new DateTime(date));

            if (sc > 20)
                return true;

        }

        return false;
    }

    @Test
    // test per capire come mai "Un utente con due sfide di coppia. ID utente 29590"
    public void checkCoopGeneration() {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        // Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
        // RecommenderSystemAPI api = new RecommenderSystemImpl();
        conf.put("GAMEID", ferrara20_gameid);
        conf.put("execDate", "2020-11-06");
        RecommenderSystemGroup rsw = new RecommenderSystemGroup();
        String challengeType = "groupCooperative";
        rsw.go(conf, "all", challengeType, null);
    }

    @Test
    public void checkSingleGeneration() {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        p(ferrara20_gameid);
        // Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
        // RecommenderSystemAPI api = new RecommenderSystemImpl();
        conf.put("GAMEID", ferrara20_gameid);
        conf.put("execDate", "2022-03-08");
        RecommenderSystemWeekly rsw = new RecommenderSystemWeekly();
        // String challengeType = "groupCooperative";
        rsw.go(conf, "all", null, null);
    }
}
