package eu.fbk.das;

import static eu.fbk.das.rs.challenges.ChallengeUtil.getPeriodScore;
import static eu.fbk.das.rs.challenges.generation.RecommendationSystem.getChallengeWeek;
import static eu.fbk.das.utils.Utils.f;
import static eu.fbk.das.utils.Utils.p;
import static eu.fbk.das.utils.Utils.pf;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import eu.fbk.das.api.RecommenderSystemImpl;
import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.rs.GroupChallengesAssigner;
import eu.fbk.das.rs.TargetPrizeChallengesCalculator;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import it.smartcommunitylab.model.ext.GroupChallengeDTO;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import eu.fbk.das.api.exec.RecommenderSystemGroup;
import eu.fbk.das.api.exec.RecommenderSystemWeekly;
import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.utils.Utils;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.ChallengeConcept;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;

public class ProdRandomTest extends ChallengesBaseTest {

    public ProdRandomTest() {
        prod = false;
    }

    @Test
    public void assignPointInterest() throws ParseException {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        p(ferrara20_gameid);

        String[] pIds = {"33324", "31548", "29473", "30453"};
        // mauro: 28540
        String[] typePois = {"test"};
        Integer[] targets = {1, 2, 3};

        for (String pId: pIds) {
            for (String typePoi: typePois) {
                for (Integer target: targets) {
                    ChallengeExpandedDTO cha = new ChallengeExpandedDTO();
                    cha.setModelName("visitPointInterest");
                    cha.setInstanceName(f("visitPointInterest_%s_%s_%d_%s", pId, typePoi, target, UUID.randomUUID()));

                    cha.setData("target", target);
                    cha.setData("typePoi", typePoi);

                    cha.setData("bonusScore", target*1.0);
                    cha.setData("bonusPointType", "green leaves");
                    cha.setData("periodName", "weekly");

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

                    cha.setStart(sdf.parse("03/05/2022"));
                    cha.setEnd(sdf.parse("05/06/2022"));

                    rs.facade.assignChallengeToPlayer(cha, ferrara20_gameid, pId);
                }
            }
        }
    }

    @Test
    public void assignDeliverableChallenge() throws ParseException {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        p(ferrara20_gameid);

        String pId = "28540"; // mauro

        int target = 3;
        String mode = "Walk_Km";

        /*ChallengeExpandedDTO cha = new ChallengeExpandedDTO();
        cha.setModelName("absoluteIncrement");
        cha.setInstanceName(f("absoluteIncrement_%s_%s_%d_%s", pId, mode, target, UUID.randomUUID()));

        cha.setData("target", target);
        cha.setData("counterName", mode);

        cha.setData("bonusScore", 100.0);
        cha.setData("bonusPointType", "green leaves");
        cha.setData("periodName", "weekly");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        cha.setStart(sdf.parse("02/07/2022"));
        cha.setEnd(sdf.parse("09/07/2022"));

        rs.facade.assignChallengeToPlayer(cha, ferrara20_gameid, pId);*/

        DateTime d = new DateTime();

        GroupChallengesAssigner gca = new GroupChallengesAssigner(rs);
        // per mauro ed antonio
        String pId2 = "23513";
       //  GroupChallengeDTO gcd = gca.createPerfomanceChallenge("Walk_Km", pId, pId2, d.plusDays(3), d.plusDays(10));

        TargetPrizeChallengesCalculator tpcc = new TargetPrizeChallengesCalculator();
        tpcc.prepare(rs, ferrara20_gameid, new DateTime());
        Map<String, Double> result = tpcc.targetPrizeChallengesCompute(pId, pId2, mode, "groupCooperative");
        GroupExpandedDTO gcd = rs.facade.makeGroupChallengeDTO(rs.gameId,
                "groupCooperative", mode, pId, pId2,
                d.plusDays(3), d.plusDays(10), result
        );
        facade.assignGroupChallenge(gcd, conf.get("GAMEID"));
    }

    @Test
    public void showChallenge() throws ParseException {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        p(ferrara20_gameid);
        String pId = "29473";

        // String cha = "visitPointInterest_29473_test_2_e45d5353-cfc7-4b8c-87b7-c3c0624023e2";

        PlayerStateDTO pl = rs.facade.getPlayerState(ferrara20_gameid, pId);
        Set<GameConcept> scores = pl.getState().get("ChallengeConcept");
        if (scores != null) {
            for (GameConcept gc : scores) {
                ChallengeConcept cha = (ChallengeConcept) gc;
                String nm = cha.getName();
                if (nm.contains("visitPointInterest")) {
                    p(cha);
                    p(cha.getState());
                    p(nm);
                    p(pId);
                }
            }
        }
    }


    @Test
    public void countChallenge() throws ParseException {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        p(ferrara20_gameid);

        int totSingleCha = 0, complSingleCha = 0, forcedSingleCha = 0;

        int complForcedSingleCha = 0;

        int totMultiCha  = 0, complMultiCha  = 0, forcedMultiCha = 0;

        // String cha = "visitPointInterest_29473_test_2_e45d5353-cfc7-4b8c-87b7-c3c0624023e2";

        double totKmSingle = 0, totKmMult = 0;

        Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
        for (String pId : pIds) {
            PlayerStateDTO pl = rs.facade.getPlayerState(ferrara20_gameid, pId);
            Map<ChallengeConcept, Date> cache = new HashMap<>();


            Set<GameConcept> scores = pl.getState().get("ChallengeConcept");
            if (scores != null) {
                for (GameConcept gc : scores) {
                    ChallengeConcept cha = (ChallengeConcept) gc;

                    if (cha.getOrigin() == null) {
                        continue;
                    }

                    if (cha.getOrigin().equals("gca")) {
                        totMultiCha += 1;
                        if (cha.isCompleted()) {
                            complMultiCha += 1;
                            totKmMult += getDoubleValue(cha.getFields().get("challengeTarget"));
                        }


                            forcedMultiCha += 1;

                    } else if (cha.getOrigin().equals("rs")) {
                        totSingleCha += 1;
                        if (cha.isCompleted()) {
                            complSingleCha += 1;
                            totKmSingle += getDoubleValue(cha.getFields().get("target"));
                        }

                        if (cha.isForced())
                            forcedSingleCha += 1;

                        if (!cha.isForced() && cha.isCompleted())
                            complForcedSingleCha ++;


                    } else if (cha.getOrigin().equals("player")) {
                        totMultiCha += 1;
                        if (cha.isCompleted()) {
                            complMultiCha += 1;
                            totKmMult += getDoubleValue(cha.getFields().get("challengeTarget"));
                        }

                       //  forcedMultiCha += 1;
                    } else
                        p("attenzione!!");
                }
            }
        }

        p("finito");
    }

    private double getDoubleValue(Object obj) {
        if (obj instanceof Integer)
            return ((Integer) obj).doubleValue();
        else if (obj instanceof Double)
            return (Double) obj;
        else
            return 0;
    }


    @Test
    public void showInterest() throws ParseException {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");

        Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
        for (String pId : pIds) {
            PlayerStateDTO pl = rs.facade.getPlayerState(ferrara20_gameid, pId);
            Map<ChallengeConcept, Date> cache = new HashMap<>();


            Set<GameConcept> scores = pl.getState().get("ChallengeConcept");
            if (scores != null) {
                for (GameConcept gc : scores) {
                    ChallengeConcept cha = (ChallengeConcept) gc;
                    String nm = cha.getName();
                    if (cha.getOrigin() == null || !cha.getOrigin().equals("gca"))
                        continue;
                    p(cha);
                    /*if (nm.contains("visitPointInterest")) {
                        p(cha);
                        p(cha.getState());
                        p(nm);
                        p(pId);
                    }*/
                }
            }
/*
            Set<GameConcept> badges = pl.getState().get("BadgeCollectionConcept");
            if (badges != null) {
                for (GameConcept el : badges) {
                    BadgeCollectionConcept bcc = (BadgeCollectionConcept) el;
                    String nm = bcc.getName();
                    if (bcc.getBadgeEarned().size() == 0)
                        continue;

                    if (!("test").equals(nm) && !("airbreak").equals(nm) && !("bottmer").equals(nm))
                        continue;

                    p(pId);
                    p(nm);
                    p(bcc.getBadgeEarned());
                }
            }*/
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
        // String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        // p(ferrara20_gameid);
        // Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
        // RecommenderSystemAPI api = new RecommenderSystemImpl();
        conf.put("GAMEID", "640ef0a3d82bd2057035f94e");
        conf.put("execDate", "2022-03-08");
        RecommenderSystemWeekly rsw = new RecommenderSystemWeekly();
        // String challengeType = "groupCooperative";
        rsw.go(conf, "all", null, null);
    }

    @Test
    public void checkHSCGeneration() {

        conf.put("GAMEID", "640ef0a3d82bd2057035f94e");

        DateTime execDate = new DateTime("2023-03-28");

        DateTime startDate = new DateTime("2023-03-27");

        DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();

        RecommenderSystemImpl api = new RecommenderSystemImpl();

        Set<String> modelTypes = new HashSet<>();
        modelTypes.add(ChallengesConfig.BIKE_KM);
        modelTypes.add(ChallengesConfig.WALK_KM);
        modelTypes.add(ChallengesConfig.GREEN_LEAVES);

        Map<String, String> creationRules = new HashMap<>();
        creationRules.put("1", "mobilityAbsolute");
        creationRules.put("2", "mobilityRepetitive");

        Map<String, Object> config = cloneMap(conf);
        config.put("start", fmt.print(startDate));
        config.put("duration", "7d");
        config.put("exec", execDate.toDate());
        config.put("periodName", "weekly");
        config.put("challengeWeek", getChallengeWeek(execDate, startDate));

        Map<String, String> reward = new HashMap<>();
        reward.put("scoreType", "green leaves");

        List<ChallengeExpandedDTO> chas = api.createStandardSingleChallenges(conf, modelTypes, creationRules, true, config, "all", reward);
    }

    private Map<String, Object> cloneMap(Map<String, String> conf) {
        Map<String, Object> new_map = new HashMap<>();
        for (Map.Entry<String, String> entry : conf.entrySet()) {
            new_map.put(entry.getKey(),
                    entry.getValue());
        }
        return new_map;
    }
}
