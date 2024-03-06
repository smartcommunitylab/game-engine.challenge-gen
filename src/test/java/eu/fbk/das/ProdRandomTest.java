package eu.fbk.das;

import static eu.fbk.das.rs.challenges.ChallengeUtil.getPeriodScore;
import static eu.fbk.das.utils.Utils.*;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import com.opencsv.CSVWriter;
import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.rs.GroupChallengesAssigner;
import eu.fbk.das.rs.TargetPrizeChallengesCalculator;
import org.joda.time.DateTime;

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

import com.google.common.math.Quantiles;

public class ProdRandomTest extends ChallengesBaseTest {
	
	private static HashMap<String, Double> modeMax = new HashMap<String, Double>();
	{
		modeMax.put("Walk_Km", 0.5);
		modeMax.put("Bike_Km", 210.0);
		modeMax.put("Train_Trips", 56.0);
		modeMax.put("Bus_Trips", 56.0);
		modeMax.put("green leaves", 3000.0);
	};
	
	private static HashMap<String, Double> modeMin = new HashMap<String, Double>();
	{
		modeMax.put("Walk_Km", 0.5);
		modeMax.put("Bike_Km", 0.5);
		modeMax.put("Train_Trips", 1.0);
		modeMax.put("Bus_Trips", 1.0);
		modeMax.put("green leaves", 50.0);
	};

    public ProdRandomTest() {
        prod = true;
    }

    public double getModePoint(String mode) {
        switch(mode) {
            case "Walk":
                return 10.0;
            case "Bike":
                return 6.0;
            case "Bus":
                return 3;
            case "Train":
                return 3;
            default:
                return 0.0;
        }
    }

    public double getModeLimit(String mode) {
        switch(mode) {
            case "Walk":
                return 5.0;
            case "Bike":
                return 10.0;
            case "Bus":
                return 15.0;
            case "Train":
                return 15.0;
            default:
                return 0.0;
        }
    }

    public double getScore(String mode, double distance) {
        double point = getModePoint(mode);
        double limit = getModeLimit(mode);
        double score = 0.0;
        int index = 0;
        while(index < 10) {
            // limit /= 2;
            score += Math.min(distance, limit) * point;
            distance -= limit;
            point /= 2;
            if (distance < 0) {
                break;
            }
            index++;
        }
        score += Math.max(distance, 0) * point;
        return score;
    }

    @Test
    public void testModeScore() throws ParseException {
        p(getScore("Walk", 16));
        p(getScore("Bike", 24));
        p(getScore("Bus", 60));

        p(getScore("Walk", 18));
        p(getScore("Bike", 26));
        p(getScore("Bus", 62));

        p(getScore("Walk", 5));
        p(getScore("Walk", 10));
        p(getScore("Walk", 15));
        p(getScore("Walk", 20));
        p(getScore("Walk", 30));

        p(getScore("Train", 200));

        p(getScore("Walk", 25));
    }

    @Test
    public void getMedianPerformance() throws ParseException {
        //String gameid = "620a568e554b276aba97d4a4"; // lecco gameid
        // String gameid = "5edf5f7d4149dd117cc7f17d"; // ferrara gameid
        String gameid = "5d9353a3f0856342b2dded7f"; // trento 2019

        String[] keys = new String[] { "Bus_Km", "green leaves", "Train_Km", "Walk_Km", "Bike_Km" };

        Map<String ,ArrayList<Double>> cacheAll = new HashMap<>();
        for (String k: keys) {
            cacheAll.put(k, new ArrayList<>());
        }

        Set<String> pIds = facade.getGamePlayers(gameid);
        for (String pId : pIds) {

            // get player performance, for each indicator, for each day
            PlayerStateDTO pl = rs.facade.getPlayerState(gameid, pId);
            Map<String, Set<GameConcept>> state = pl.getState();

            Map<String, PointConcept> cache = new HashMap<>();

            Set<GameConcept> pointConcepts = pl.getState().get("PointConcept");
            if (pointConcepts != null) {
                for (GameConcept gc : pointConcepts) {
                    PointConcept pt = (PointConcept) gc;

                    // list: Bus_Km, green leaves, Boat_Trips, PandR_Trips, Transit_Trips, BikeSharing_Km, Bike_Trips, Car_Km, BikeSharing_Trips, Walk_Trips, Train_Km, Walk_Km, Recommendations, Bus_Trips, Carpooling_Km, Car_Trips, Train_Trips, NoCar_Trips, Bike_Km, ZeroImpact_Trips, Carpooling_Trips
                    // p(pt.getName());

                    // acceptable ones
                    String slug_pt_name = slug(pt.getName());
                    for (String s: new String[] { "Bus_Km", "green leaves", "Train_Km", "Walk_Km", "Bike_Km" }) {
                        if (slug_pt_name.equals(slug(s)))
                            cache.put(pt.getName(), pt);
                    }

                }
            }

            String gl_label = "green leaves";
            if (!(cache.containsKey(gl_label))) continue;

            for (String mode: cache.keySet()) {
                Double score = cache.get(mode).getScore();
                if (score > 0)
                    cacheAll.get(mode).add(score);
            }

        }

        for (String k: keys) {
            ArrayList<Double> list = cacheAll.get(k);
            p(k);
            p(Quantiles.median().compute(list));
        }
    }

    @Test
    public void testNeuralNetwork() throws ParseException, IOException {
        String gameid = "620a568e554b276aba97d4a4"; // lecco gameid

        Long start_game = Long.MAX_VALUE;

        HashMap<String, Map<String, Map<Long, Double>>> cacheAll = new HashMap<>();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
        String filePath = f("%s/Dropbox/skana@fbk/test-neural-network/first-%s-%s.csv", System.getProperty("user.home"), gameid, dtf.format(LocalDateTime.now()));

        File file = new File(filePath);
        FileWriter outputfile = new FileWriter(file);
        CSVWriter writer = new CSVWriter(outputfile,',','\0');

        String[] modes = new String[] { "green leaves", "Walk_Km", "Bike_Km", "Bus_Km", "Train_Km" };

        Map<Integer, List<List<String>>> records = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(f("%s/Dropbox/skana@fbk/test-neural-network/weather-%s-2022.csv", System.getProperty("user.home"), gameid)))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                Integer dt = Integer.valueOf(values[1]);
                p(dt);
                if (dt < 0) continue;
                int week = dt / 7;
                if (!records.containsKey(week)) records.put(week, new ArrayList<>());
                records.get(week).add(Arrays.asList(values));
            }
        }

        p(records);

        Set<String> pIds = facade.getGamePlayers(gameid);
        for (String pId : pIds) {

            // get player performance, for each indicator, for each day
            PlayerStateDTO pl = rs.facade.getPlayerState(gameid, pId);
            Map<String, Set<GameConcept>> state = pl.getState();

             for (String key: state.keySet()) {
                // list: BadgeCollectionConcept, ChallengeConcept, PointConcept
                // p(key);
            }

             Map<String, PointConcept> cache = new HashMap<>();

            Set<GameConcept> pointConcepts = pl.getState().get("PointConcept");
            if (pointConcepts != null) {
                for (GameConcept gc : pointConcepts) {
                    PointConcept pt = (PointConcept) gc;

                    // list: Bus_Km, green leaves, Boat_Trips, PandR_Trips, Transit_Trips, BikeSharing_Km, Bike_Trips, Car_Km, BikeSharing_Trips, Walk_Trips, Train_Km, Walk_Km, Recommendations, Bus_Trips, Carpooling_Km, Car_Trips, Train_Trips, NoCar_Trips, Bike_Km, ZeroImpact_Trips, Carpooling_Trips
                    // p(pt.getName());

                    // acceptable ones
                    String slug_pt_name = slug(pt.getName());
                    for (String s: modes) {
                        if (slug_pt_name.equals(slug(s)))
                            cache.put(pt.getName(), pt);
                    }

                }
            }

            String gl_label = "green leaves";
            if (!(cache.containsKey(gl_label))) continue;

            Double gl_score = cache.get(gl_label).getScore();
            if (gl_score <= 0) continue;

            HashMap<String, Map<Long, Double>> cachePlayer = new HashMap<String, Map<Long, Double>>();

            for (String mode: cache.keySet()) {
                Map<String, PointConcept.PeriodInternal> periods = cache.get(mode).getPeriods();
                PointConcept.Period period = periods.get("weekly");
                // p(period);

              /*  Map<Long, Double> val = period.getValues();

                for (Long key: val.keySet()) {
                    if (key < start_game)
                        start_game = key;
                }*/

                // p(val);

                // cachePlayer.put(mode, val);

                // TreeMap<LocalDateTime, PointConcept.PeriodInstanceImpl> instances = period.getInstances();

            }

            cacheAll.put(pId, cachePlayer);
        }

        int min_week = Integer.MAX_VALUE;
        int max_week = Integer.MIN_VALUE;

        Map<String, Double> max_mode_values = new HashMap<>();
        for (String s: modes) {
            max_mode_values.put(s, Double.MIN_VALUE);
        }

        HashMap<String, Map<String, Map<Integer, Double>>> cacheNew = new HashMap<>();
        for (String pId: cacheAll.keySet()) {
            Map<String, Map<Long, Double>> cachePlayerOld = cacheAll.get(pId);
            Map<String, Map<Integer, Double>> cachePlayerNew = new HashMap<>();
            for (String mode: cachePlayerOld.keySet()) {
                Map<Long, Double> cacheModeOld = cachePlayerOld.get(mode);
                Map<Integer, Double> cacheModeNew = new HashMap<>();
                for (Long start: cacheModeOld.keySet()) {
                        int week = (int) Math.ceil ((start - start_game) * 1.0 / (1000*60*60*24*7));
                    //p(week);
                    //p(start);
                    //p(start_game);

                    if (week < min_week) {
                        min_week = week;
                        pf("min_week - %d\n", start);
                    }
                    if (week > max_week) {
                        max_week = week;
                        pf("max_week - %d\n", start);
                    }

                    double v = cacheModeOld.get(start);
                    if (v > max_mode_values.get(mode))
                        max_mode_values.put(mode, v*1.1);

                    cacheModeNew.put(week, v);
                }
                cachePlayerNew.put(mode, cacheModeNew);
            }
            cacheNew.put(pId,cachePlayerNew);
        }

        p("Max modes");
        p(max_mode_values);

        for (int week = min_week; week < max_week; week++) {
            for (String pId: cacheNew.keySet()) {
                Map<String, Map<Integer, Double>> cachePlayer = cacheNew.get(pId);
                List<String> data = new ArrayList<>();
                double cumulative_sum = 0;
                for (int cnt = 0; cnt < 6; cnt++) {
                    // put data on modes
                    int ix = week - cnt;
                    for (String mode: modes) {
                        Map<Integer, Double> cacheMode = cachePlayer.get(mode);
                        double value = 0.0;
                        if (cacheMode.containsKey(ix))
                            value = cacheMode.get(ix);

                        // don't normalize, tensorflow will do that
                        // double normalized = value / max_mode_values.get(mode);

                        data.add(String.valueOf(round(value, 2)));

                        cumulative_sum += value;
                    }

                    // put data on weather days
                    if (!records.containsKey(ix)) {
                        for (int y = 0; y < 7; y++)
                            for (int i = 0; i < 4; i++)
                                data.add("0");
                    } else {
                        List<List<String>> weather = records.get(ix);
                        for (List<String> day : weather) {
                            // tmedia
                            data.add(String.valueOf(round(Double.parseDouble(day.get(3)), 2)));
                            // umidita
                            data.add(String.valueOf(round(Double.parseDouble(day.get(7)), 2)));
                            // pioggia
                            data.add(String.valueOf(round(Double.parseDouble(day.get(16)), 2)));
                            // temporale
                            data.add(String.valueOf(round(Double.parseDouble(day.get(17)), 2)));
                        }
                    }
                }

                if (cumulative_sum > 0)
                    writer.writeNext(data.toArray(new String[0]));

            }

        }

        writer.close();
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
        tpcc.prepare(rs, ferrara20_gameid, new DateTime(), modeMax, modeMin);
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

    @Test
    public void checkSingleGenerationProd() {
        String gameid = "62752181ae6e2235a9544463";
        // Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
        // RecommenderSystemAPI api = new RecommenderSystemImpl();
        conf.put("HOST", "https://gedev.playngo.it/gamification/");
        conf.put("API_USER", "long-rovereto");
        conf.put("API_PASS", "rov");
        conf.put("GAMEID", gameid);
        conf.put("execDate", "2022-11-08");
        RecommenderSystemWeekly rsw = new RecommenderSystemWeekly();
        // String challengeType = "groupCooperative";
        //rsw.go(conf, "all", null, null);
         rsw.go(conf, "u_b8d1f41f70c448e68fcf6479359369dd", null, null);

    }
}
