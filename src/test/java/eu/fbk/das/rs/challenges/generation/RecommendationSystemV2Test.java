package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.api.exec.RecommenderSystemWeekly;
import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.utils.Pair;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.ChallengeConcept;
import it.smartcommunitylab.model.ext.GameConcept;
import org.apache.commons.io.IOUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.joda.time.DateTime;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static eu.fbk.das.utils.Utils.*;

// Test per la versione V2 - con gestione del detect di intervenire con i repetitive
public class RecommendationSystemV2Test extends ChallengesBaseTest {

    public RecommendationSystemV2Test() {
        prod = true;
    }

    @Test
    public void testRepetitiveInterveneAnalyze() throws IOException, ParseException {
        for (int i = 1; i < 4; i++) {
            p(this.getClass().getResource("."));
            InputStream is = getClass().getClassLoader().getResourceAsStream(f("past-performances/response-%d.json", i));
            String response = IOUtils.toString(is, StandardCharsets.UTF_8.name());
            // p(response);
            Map<Integer, double[]> cache = rs.getTimeSeriesPerformance(response);
            double decision = rs.repetitiveInterveneAnalyze(cache);
            p(decision);
        }
    }

    @Test
    public void testRepetitiveQuery() throws IOException, ParseException {
        p(rs.getRepetitiveQuery("1345", new DateTime().toDate()));
    }

    @Test
    public void testRepetitiveInterveneSingle() throws Exception {
        String gameId = conf.get("FERRARA20_GAMEID");
        // String gameId = conf.get("TRENTO19_GAMEID");
        conf.put("GAMEID", gameId);
        conf.put("execDate", "2019-12-14");

        Set<String> pIds = facade.getGamePlayers(gameId);
         // for (String pId: pIds) p(pId);

        DateTime date = parseDate(conf.get("execDate"));
        Map<String, Object> challengeValues = new HashMap<>();
        challengeValues.put("exec", date);
        challengeValues.put("challengeWeek", 1);
        rs.prepare(challengeValues);

        PlayerStateDTO state = facade.getPlayerState(gameId, "28635");
        rs.repetitiveIntervene(state, date);
    }

    @Test
    public void testRepetitiveIntervene() throws ParseException, java.text.ParseException {
        // String gameId = conf.get("FERRARA20_GAMEID");
        String gameId = conf.get("TRENTO19_GAMEID");
        conf.put("GAMEID", gameId);
        conf.put("execDate", "2020-02-1");

        DateTime date = parseDate(conf.get("execDate"));
        Map<String, Object> challengeValues = new HashMap<>();
        challengeValues.put("exec", date);
        challengeValues.put("challengeWeek", 1);
        rs.prepare(challengeValues);

        rs.debug = true;

        Set<String> pIds = facade.getGamePlayers(gameId);
        for (String pId: pIds) {
            // p(pId);
            PlayerStateDTO state = facade.getPlayerState(gameId, pId);
            rs.repetitiveIntervene(state, date);
        }

    }

    @Test
    public void testExtractRipetitivePerformance() throws IOException, ParseException, java.text.ParseException {
        String gameId = conf.get("FERRARA20_GAMEID");
        // String gameId = conf.get("TRENTO19_GAMEID");
        rs.gameId = gameId;
        conf.put("GAMEID", gameId);
        conf.put("execDate", "2021-02-15");
        DateTime date = parseDate(conf.get("execDate"));

        Map<String, Object> challengeValues = new HashMap<>();
        challengeValues.put("exec", date);
        challengeValues.put("challengeWeek", 1);
        rs.prepare(challengeValues);

        Set<String> pIds = facade.getGamePlayers(gameId);

        // Player high performance / low entropy
        for (String pId: new String[] {"30264", "27443", "28607"}) {
            saveHistogramPerformance(gameId, date, pId, true);
        }

        // Player high performance / high entropy
        for (String pId: new String[] {"28464", "24349", "27583"}) {
            saveHistogramPerformance(gameId, date, pId, false);
        }
    }

    private void saveHistogramPerformance(String gameId, DateTime date, String pId, boolean b) throws IOException, ParseException {
        PlayerStateDTO state = facade.getPlayerState(gameId, pId);
        Map<Integer, double[]> cache = rs.extractRepetitivePerformance(pId, date);

        for (Integer w: cache.keySet()) {

            String filename = f("%s-%d", pId, w);

            HistogramDataset dataset = new HistogramDataset();
            dataset.addSeries("key", cache.get(w), 7);

            JFreeChart barChart = ChartFactory.createHistogram(
                    filename,
                    "Day",
                    "Performance",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true, true, false);

            String path = "repetNonChosen";
            if (b)  path = "repetChosen";

            File directory = new File(path);
            if (! directory.exists()) directory.mkdir();
            path += f("/%s", pId);
            directory = new File(path);
            if (! directory.exists()) directory.mkdir();
            path += "/" + filename + ".jpg";

            int width = 640;    /* Width of the image */
            int height = 480;   /* Height of the image */
            File BarChart = new File(path);
            ChartUtilities.saveChartAsJPEG( BarChart, barChart, width, height);

        }
    }

    @Test
    public void testRepetitiveChoice() throws ParseException, java.text.ParseException, IOException {
        String gameId = conf.get("FERRARA20_GAMEID");
        // String gameId = conf.get("TRENTO19_GAMEID");
        conf.put("GAMEID", gameId);
        rs.gameId = gameId;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        DateTime date = parseDate("2021-02-27");

        for (int j = 0; j < 5; j++ ) {

            String sdt = sdf.format(date);
            conf.put("execDate", sdt);

            Map<String, Object> challengeValues = new HashMap<>();
            challengeValues.put("exec", date);
            challengeValues.put("challengeWeek", 1);
            rs.prepare(challengeValues);

            rs.debug = true;

            Set<String> pIds = facade.getGamePlayers(gameId);
            for (String pId : pIds) {

                PlayerStateDTO state = facade.getPlayerState(gameId, pId);
                Map<Integer, double[]> cache = rs.extractRepetitivePerformance(pId, date);
                // if null does not intervene
                if (cache == null) continue;
                // analyze if we have to assign repetitive
                double ent = rs.repetitiveInterveneAnalyze(cache);
                Pair<Double, Double> res = rs.repetitiveTarget(state, 2);
                double target = res.getSecond();
                if (ent < -1.5)
                    continue;

                int slot = 2;
                if (ent < -1.3) slot = 5;
                else if (ent < -1.1) slot = 4;
                else if (ent < -0.9) slot = 3;

                String path = "../experiment";
                path = createDirectory(path, null);
                path = createDirectory("rep", path);
                path = createDirectory(gameId, path);
                path = createDirectory(sdf.format(date), path);
                path = createDirectory(f("%d", slot), path);
                path = createDirectory(f("%s", pId), path);

                // Create recap graph
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                for (Integer w : cache.keySet()) {
                    double[] gg = cache.get(w);
                    for (int i = 0; i < 7; i++)
                        dataset.addValue(gg[i], f("W%d", w), f("D%d", i + 1));
                }
                createBarPlot(target, path, dataset, f("%s-recap", pId));

                // Create single graphs
                for (Integer w : cache.keySet()) {

                    dataset = new DefaultCategoryDataset();
                    double[] gg = cache.get(w);
                    for (int i = 0; i < 7; i++)
                        dataset.addValue(gg[i], "L", f("D%d", i + 1));
                    // dataset.addSeries("key", cache.get(w), 7);

                    createBarPlot(target, path, dataset, f("%s-%d", pId, w));
                }

                pf("%s - %.2f - %d\n", pId, ent, slot);
            }

            DateTime aux = new DateTime(date);
            date = aux.minusDays(7);
        }

    }

    private void createBarPlot(double target, String path, DefaultCategoryDataset dataset, String filename) throws IOException {
        JFreeChart barChart = ChartFactory.createBarChart(
                filename,
                "Day",
                "Performance",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        ValueMarker marker = new ValueMarker(target);  // position is the value on the axis
        marker.setPaint(Color.black);

        CategoryPlot plot = (CategoryPlot) barChart.getPlot();
        plot.addRangeMarker(marker);

        int width = 1000;    /* Width of the image */
        int height = 600;   /* Height of the image */
        File BarChart = new File(path + "/" + filename + ".jpg");
        ChartUtilities.saveChartAsJPEG(BarChart, barChart, width, height);
    }

    private String createDirectory(String new_p, String path) {
        if (path == null) path = new_p;
        else path += "/" + new_p;
        File directory = new File(path);
        if (!directory.exists()) directory.mkdir();
        return path;
    }


    @Test
    public void tesRepetitiveIntervene() throws IOException, ParseException, java.text.ParseException {
        String gameId = conf.get("FERRARA20_GAMEID");
        // String gameId = conf.get("TRENTO19_GAMEID");
        rs.gameId = gameId;
        conf.put("GAMEID", gameId);
        conf.put("execDate", "2021-02-24");
        DateTime date = parseDate(conf.get("execDate"));

        Map<String, Object> challengeValues = new HashMap<>();
        challengeValues.put("exec", date);
        challengeValues.put("challengeWeek", 1);
        rs.prepare(challengeValues);

        Set<String> pIds = facade.getGamePlayers(gameId);

        // Player high performance / low entropy
        for (String pId: pIds) {
            PlayerStateDTO state = facade.getPlayerState(gameId, pId);
            List<ChallengeExpandedDTO> ls = rs.repetitiveIntervene(state, date);
            if (ls == null) continue;
            ChallengeExpandedDTO rep = ls.get(0); 
            pf("%s %.2f %.2f %.2f \n", pId, rep.getData("periodTarget"), rep.getData("target"), rep.getData("bonusScore"));
        }

    }

    @Test

    public void checkWholeGeneration() {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        // Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
        // RecommenderSystemAPI api = new RecommenderSystemImpl();
        conf.put("GAMEID", ferrara20_gameid);
        conf.put("execDate", "2021-04-07");
        RecommenderSystemWeekly rsw = new RecommenderSystemWeekly();

        rsw.go(conf, "all", null, null);
    }

    @Test
    public void testRepetitiveTrend() throws ParseException, java.text.ParseException, IOException {
        String gameId = conf.get("FERRARA20_GAMEID");
        // String gameId = conf.get("TRENTO19_GAMEID");
        conf.put("GAMEID", gameId);
        rs.gameId = gameId;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        DateTime date = parseDate("2021-06-26");

        Set<String> pIds = facade.getGamePlayers(gameId);

        for (int j = 0; j < 10; j++ ) {

            String sdt = sdf.format(date);
            conf.put("execDate", sdt);

            Map<String, Object> challengeValues = new HashMap<>();
            challengeValues.put("exec", date);
            challengeValues.put("challengeWeek", 1);
            rs.prepare(challengeValues);

            rs.debug = true;

            int cnt_playing = 0;
            int cnt_rep = 0;

            double mean_ent = 0;

            for (String pId : pIds) {

                PlayerStateDTO state = facade.getPlayerState(gameId, pId);

                // check if that week it was active
                Double currentValue = rs.getWeeklyContentMode(state, "green leaves", new DateTime(date));
                if (currentValue == 0) {
                    continue;
                }

                Map<Integer, double[]> cache = rs.extractRepetitivePerformance(pId, date);
                // if null does not intervene
                if (cache == null) continue;

                // Check only playing in that week
                cnt_playing++;

                // analyze if we have to assign repetitive
                double ent = rs.repetitiveInterveneAnalyze(cache);
                mean_ent += ent;

                int slot = rs.repetitiveSlot(ent);
                if (slot == 0)
                    continue;

                Pair<Double, Double> tg = rs.repetitiveTarget(state, rs.repetitiveDifficulty);
                Double repScore = tg.getSecond();
                double repTarget = tg.getFirst();

                pf("%s # %d # %.2f # %.2f # %.2f\n", pId, slot, repScore, repTarget, ent);

                cnt_rep++;
            }

            pf("### WEEK %s - %d - %d - %.2f - %.2f\n", sdt, cnt_playing, cnt_rep, cnt_rep * 1.0 / cnt_playing, mean_ent / cnt_playing);

            DateTime aux = new DateTime(date);
            date = aux.minusDays(7);
        }

    }


    @Test
    public void extractCorrectErraticBehaviour() {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        conf.put("GAMEID", ferrara20_gameid);

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd");
        Map<String, List<String>> cache = new HashMap<>();

        Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
        for (String pId: pIds) {
            PlayerStateDTO pl = facade.getPlayerState(conf.get("GAMEID"), pId);

            Set<GameConcept> scores =  pl.getState().get("ChallengeConcept");
            if (scores == null) continue;
            for (GameConcept gc : scores) {
                ChallengeConcept cha = (ChallengeConcept) gc;

                String chNm = cha.getName();
                if (!(chNm.contains("correctErraticBehaviour")))
                    continue;

                String chSt = fmt.format(cha.getStart());
                Map<String, Object> fields = cha.getFields();
                int cmp = 0;
                if (cha.isCompleted()) cmp = 1;
                String res = f("%s,%.2f,%s,%d", pId, fields.get("target"),  fields.get("periodTarget"), cmp);

                if (!cache.containsKey(chSt)) cache.put(chSt, new ArrayList<>());
                cache.get(chSt).add(res);
            }
        }

        TreeSet<String> keys = new TreeSet<>(cache.keySet());
        for (String k: keys.descendingSet()) {
            pf("###### %s\n", k);
            for (String r: cache.get(k))
                p(r);
        }

    }

    @Test
    public void extractChallengeData() {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        conf.put("GAMEID", ferrara20_gameid);

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd");

        Map<String, Map<String, Integer>> cache = new HashMap<>();
        Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);

        for (String pId: pIds) {
            PlayerStateDTO pl = facade.getPlayerState(conf.get("GAMEID"), pId);
            Set<GameConcept> scores =  pl.getState().get("ChallengeConcept");
            if (scores == null) continue;

            for (GameConcept gc : scores) {
                ChallengeConcept cha = (ChallengeConcept) gc;

                String nm = cha.getName();
                if (nm.contains("survey") || nm.contains("initial") || nm.contains("recommend"))
                    continue;

                String mName = cha.getModelName();
                //if (mName.contains("group") || mName.contains("repetitive") || mName.contains("absolute"))
                //     continue;

                String chSt = fmt.format(cha.getStart());
                addCache(cache, chSt, "tot");
                Map<String, Object> fields = cha.getFields();

                // check if that week it was active
                Double currentValue = rs.getWeeklyContentMode(pl, "green leaves", new DateTime(cha.getStart()));
                if (currentValue == 0) {
                    addCache(cache, chSt, "active0");
                    continue;
                }

                addCache(cache, chSt, "active1");

                // if active, check if also completed
                if (cha.isCompleted())
                    addCache(cache, chSt, "comp1");
                else
                    addCache(cache, chSt, "comp0");
            }
        }

        SortedSet<String> keys = new TreeSet<>(cache.keySet());
        for (String k: keys) {
            pf("###### %s\n", k);
            for (String r: cache.get(k).keySet()) {
                pf("%s - %d\n", r, cache.get(k).get(r));
            }
        }

    }

    private void addCache(Map<String, Map<String, Integer>> cache, String chSt, String ind) {
        if (!cache.containsKey(chSt)) cache.put(chSt, new HashMap<String, Integer>());
        Map<String, Integer> chWeek = cache.get(chSt);
        if (!chWeek.containsKey(ind)) chWeek.put(ind, 0);
        chWeek.put(ind, chWeek.get(ind) + 1);
    }

}
