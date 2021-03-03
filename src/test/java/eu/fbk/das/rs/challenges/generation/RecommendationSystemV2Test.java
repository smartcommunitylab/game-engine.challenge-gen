package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.utils.Pair;
import it.smartcommunitylab.model.PlayerStateDTO;
import org.apache.commons.io.IOUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.CategoryDataset;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public void testRepetitiveInterveneSingle() throws java.text.ParseException {
        String gameId = conf.get("FERRARA20_GAMEID");
        // String gameId = conf.get("TRENTO19_GAMEID");
        conf.put("GAMEID", gameId);
        conf.put("execDate", "2019-12-14");

        Set<String> pIds = facade.getGamePlayers(gameId);
         // for (String pId: pIds) p(pId);

        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(conf.get("execDate"));
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

        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(conf.get("execDate"));
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
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(conf.get("execDate"));

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

    private void saveHistogramPerformance(String gameId, Date date, String pId, boolean b) throws IOException, ParseException {
        PlayerStateDTO state = facade.getPlayerState(gameId, pId);
        Map<Integer, double[]> cache = rs.extractRipetitivePerformance(state, date);

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
    /* TODO farlo andare su più date */
    public void testRepetitiveChoice() throws ParseException, java.text.ParseException, IOException {
        String gameId = conf.get("FERRARA20_GAMEID");
        // String gameId = conf.get("TRENTO19_GAMEID");
        conf.put("GAMEID", gameId);
        rs.gameId = gameId;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date date = sdf.parse("2021-02-27");

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
                Map<Integer, double[]> cache = rs.extractRipetitivePerformance(state, date);
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

            DateTime aux = new DateTime(date.getTime());
            date = aux.minusDays(7).toDate();
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
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(conf.get("execDate"));

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

}
