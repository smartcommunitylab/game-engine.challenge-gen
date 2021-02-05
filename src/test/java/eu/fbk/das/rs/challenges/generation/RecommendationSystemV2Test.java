package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import it.smartcommunitylab.model.PlayerStateDTO;
import org.apache.commons.io.IOUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.joda.time.DateTime;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static eu.fbk.das.utils.Utils.f;
import static eu.fbk.das.utils.Utils.p;

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
            boolean decision = rs.repetitiveInterveneAnalyze(cache);
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
        conf.put("execDate", "2020-02-15");

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
        // String gameId = conf.get("FERRARA20_GAMEID");
        String gameId = conf.get("TRENTO19_GAMEID");
        conf.put("GAMEID", gameId);
        conf.put("execDate", "2020-02-15");
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(conf.get("execDate"));

        Set<String> pIds = facade.getGamePlayers(gameId);

        // Player high performance / low entropy
        for (String pId: new String[] {"28635", "27443", "28607"}) {
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
}
