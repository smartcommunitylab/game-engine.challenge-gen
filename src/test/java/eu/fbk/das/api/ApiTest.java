package eu.fbk.das.api;

import eu.fbk.das.api.exec.RecommenderSystemGroup;
import eu.fbk.das.api.exec.RecommenderSystemTantum;
import eu.fbk.das.api.exec.RecommenderSystemWeekly;
import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static eu.fbk.das.utils.Utils.p;

public class ApiTest extends ChallengesBaseTest {

    public ApiTest() {
        prod = true;
    }

    protected boolean quick = false;

    private Set<String> ps;
    private DateTime execDate;
    private DateTime startDate;
    private DateTime endDate;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private String players;

    @Before
    public void prepare() {
        setStartEnd(new DateTime());
    }

    private void setStartEnd(DateTime date) {
        this.execDate = date.withHourOfDay(0)
                .withMinuteOfHour(0)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);

        int week_day = execDate.getDayOfWeek();
        int d = (7 - week_day) + 1;
        startDate = execDate.plusDays(d);
        startDate = startDate.minusDays(2);
        endDate = startDate.plusDays(7);

        if (quick)
            players = "1069";
        else
            players = "all";
    }

    @Test
    public void checkStartEnd() throws ParseException {
        setStartEnd(getDT("2020-08-27"));
        DateTime start = getDT("2020-08-29");
        assert(start.compareTo(startDate) == 0);
        DateTime end = getDT("2020-09-05");
        assert(end.compareTo(endDate) == 0);

        setStartEnd(getDT("2020-08-17"));
        start = getDT("2020-08-22");
        assert(start.compareTo(startDate) == 0);
        end = getDT("2020-08-29");
        assert(end.compareTo(endDate) == 0);

    }

    private DateTime getDT(String s) throws ParseException {
        return new DateTime(sdf.parse(s));
    }

    @Test
    public void testSingleGeneration() {
        // tipi challenge random
        getRandomModelTypes();

        // Ottieni challenges
        RecommenderSystemTantum rsw = new RecommenderSystemTantum();
        List<ChallengeExpandedDTO> res = rsw.go(conf, "survey", new HashMap<>(), players);

        if (!quick)
            ps = facade.getGamePlayers(conf.get("GAMEID"));

        // Fai verifiche sulle challenges
        for (ChallengeExpandedDTO cha: res) {

            // Controlla date
            checkDates(cha);

            String pointType = (String) cha.getData("bonusPointType");
            assert("green leaves".equals(pointType));

            if (!quick) {
                String pId = (String) cha.getInfo("pId");
                ps.remove(pId);
            }
        }

        // Controlla che tutti i giocatori abbiano ricevuto la sfida
        if (!quick) {
            assert(ps.size() == 0);
        }

    }

    private HashSet<String> getRandomModelTypes() {
        HashSet<String> modelTypes = new HashSet<>();
        modelTypes.add(ChallengesConfig.GREEN_LEAVES);

        Random r = new Random();
        if (r.nextFloat() > 0.2)
            modelTypes.add(ChallengesConfig.BIKE_KM);
        if (r.nextFloat() > 0.2)
            modelTypes.add(ChallengesConfig.WALK_KM);
        if (r.nextFloat() > 0.2)
            modelTypes.add(ChallengesConfig.BUS_KM);
        if (r.nextFloat() > 0.2)
            modelTypes.add(ChallengesConfig.TRAIN_KM);
        return modelTypes;
    }

    @Test
    public void testWeeklyGeneration() {
        HashSet<String> modelTypes = getRandomModelTypes();

        // Ottieni challenges
        RecommenderSystemWeekly rsw = new RecommenderSystemWeekly();
        List<ChallengeExpandedDTO> res = rsw.go(conf, players, modelTypes, null);

        if (!quick)
            ps = facade.getGamePlayers(conf.get("GAMEID"));

        // Fai verifiche sulle challenges
        for (ChallengeExpandedDTO cha: res) {
            // Controlla date
            checkDates(cha);

            assert("rs".equals(cha.getOrigin()));

            assert(cha.isHide());

            double score = (double) cha.getData("bonusScore");
            assert(score > 0);

            String counterName = (String) cha.getData("counterName");
            assert(modelTypes.contains(counterName));

            String pointType = (String) cha.getData("bonusPointType");
            assert("green leaves".equals(pointType));

            String periodName = (String) cha.getData("periodName");
            assert("weekly".equals(periodName));

            // controlla target intero
            double target = (double) cha.getData("target");
            assert(target - Math.floor(target) == 0);

            Integer challengeWeek = (Integer) cha.getData("challengeWeek");
            assert(challengeWeek > 0);

            if (!quick) {
                String pId = (String) cha.getInfo("pId");
                ps.remove(pId);
            }
        }

        // Controlla che tutti i giocatori abbiano ricevuto la sfida
        if (!quick) {
            assert(ps.size() == 0);
        }

    }

    @Test
    public void testGroupGeneration() {
        String[] chaTypes = new String[]{"groupCooperative", "groupCompetitiveTime", "groupCompetitivePerformance"};
        for (String challengeType : chaTypes) {

            HashSet<String> modelTypes = getRandomModelTypes();

            // Ottieni challenges
            RecommenderSystemGroup rsw = new RecommenderSystemGroup();
            List<GroupExpandedDTO> res = rsw.go(conf, "all", challengeType, modelTypes);

            ps = facade.getGamePlayers(conf.get("GAMEID"));

            // Fai verifiche sulle challenges
            for (GroupExpandedDTO cha : res) {
                // Controlla date
                checkDates(cha);

                assert ("gca".equals(cha.getOrigin()));

                /*

                double score = (double) cha.getData("bonusScore");
                assert (score > 0);

                String counterName = (String) cha.getData("counterName");
                assert (modelTypes.contains(counterName));

                String pointType = (String) cha.getData("bonusPointType");
                assert ("green leaves".equals(pointType));

                String periodName = (String) cha.getData("periodName");
                assert ("weekly".equals(periodName));

                // controlla target intero
                double target = (double) cha.getData("target");
                assert (target - Math.floor(target) == 0);

                Integer challengeWeek = (Integer) cha.getData("challengeWeek");
                assert (challengeWeek > 0);


                if (!quick) {
                    String pId = (String) cha.getInfo("pId");
                    ps.remove(pId);
                }
                */

            }

            /*
            // Controlla che tutti i giocatori abbiano ricevuto la sfida
            if (!quick) {
                assert (ps.size() == 0);
            }
            */


        }
    }


    private void checkDates(ChallengeExpandedDTO cha) {
        DateTime start = new DateTime(cha.getStart());
        assert(start.compareTo(startDate) == 0);
        DateTime end = new DateTime(cha.getEnd());
        assert(end.compareTo(endDate) == 0);
    }

    private void checkDates(GroupExpandedDTO cha) {
        DateTime start = new DateTime(cha.getStart());
        assert(start.compareTo(startDate) == 0);
        DateTime end = new DateTime(cha.getEnd());
        assert(end.compareTo(endDate) == 0);
    }

    @Test
    public void testProblemGeneration() {
        // p(conf);
        conf.put("GAMEID", "5edf5f7d4149dd117cc7f17d");

        String challengeType = "groupCooperative";
        HashSet<String> modelTypes = new HashSet<>();
        modelTypes.add(ChallengesConfig.GREEN_LEAVES);
        modelTypes.add(ChallengesConfig.BIKE_KM);
        modelTypes.add(ChallengesConfig.WALK_KM);

        RecommenderSystemGroup rsw = new RecommenderSystemGroup();
        List<GroupExpandedDTO> res = rsw.go(conf, "all", challengeType, modelTypes);

        // Fai verifiche sulle challenges
        for (GroupExpandedDTO cha : res) {
            p(cha);
        }
        // List<GroupExpandedDTO> res = rsw.go(conf, "all", challengeType, modelTypes);

    }
}
