package eu.fbk.das.rs.challenges.generation;

import com.google.common.math.Quantiles;
import eu.fbk.das.rs.utils.ArrayUtils;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.trentorise.game.challenges.rest.Player;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.PointConcept;
import eu.trentorise.game.challenges.rest.State;
import org.joda.time.DateTime;

import java.io.*;
import java.util.*;

import static eu.fbk.das.rs.utils.Utils.formatDate;
import static eu.fbk.das.rs.utils.Utils.*;
import static eu.fbk.das.rs.challenges.generation.RecommendationSystem.fixMode;

public class RecommendationSystemStatistics {

    private String STATS_FILENAME = "rs.statistics";
    protected GamificationEngineRestFacade facade;

    protected DateTime execDate;
    private String[] l_mode;
    private Map<String, Map<Integer, Double>> quartiles;
    private boolean offline = true;
    protected RecommendationSystemConfig cfg;

    private DateTime lastMonday;

    public RecommendationSystemStatistics() {
        quartiles = new HashMap<>();

        this.l_mode = ArrayUtils.cloneArray(ChallengesConfig.defaultMode);
        for (int i = 0; i < l_mode.length; i++)
            l_mode[i] = fixMode(l_mode[i]);
        Arrays.sort(this.l_mode);
    }

    public Map<String, Map<Integer, Double>> checkAndUpdateStats(GamificationEngineRestFacade facade, DateTime date, RecommendationSystemConfig cfg) {
        this.facade = facade;
        this.cfg = cfg;
        this.execDate = date;

        if (offline)
            return updateStatsOffline();

        return updateStatsOnline();

    }

    private Map<String, Map<Integer, Double>> updateStatsOnline() {

        facade.readGameStatistics(cfg.get("GAME_ID"));

        return  null;
    }

    private Map<String, Map<Integer, Double>> updateStatsOffline() {
        quartiles = tryReadStats();

        if (quartiles == null)
            quartiles = updateStats();

        return quartiles;

    }

    private Map<String, Map<Integer, Double>> tryReadStats() {

        if (!new File(STATS_FILENAME).exists())
            return null;

        try {
            BufferedReader r = getReader(STATS_FILENAME);

            String fileDateStr = r.readLine();
            if (fileDateStr == null || fileDateStr.length() == 0)
                return null;

            DateTime fileDate = stringToDate(fileDateStr.trim());
            if (fileDate == null)
                return null;

            if (daysApart(fileDate, execDate) >= 3)
                return null;

            return readStats(r);

        } catch (IOException e) {
            logExp(e);
        }

        return null;
    }

    private Map<String, Map<Integer, Double>> readStats(BufferedReader r) throws IOException {
        Map<String, Map<Integer, Double>> stats = new HashMap<>();
        String line = r.readLine();
        while (line != null) {
            String[] aux = line.split(":");
            String name = aux[0];
            aux = aux[1].split("\\s+");
            Map<Integer, Double> qua = new HashMap<>();
            for (int i = 1; i < aux.length ; i++)
                qua.put(i-1, Double.valueOf(aux[i]));

            stats.put(name, qua);
            line = r.readLine();
        }

        return stats;
    }

    private void writeStats(Writer wr) {
        try {
            wf(wr, "%s\n", formatDate(execDate));
            for (String s : l_mode) {
                wf(wr, "%s:", s);
                for (int i = 0; i < 10; i++) {
                    wf(wr, " %.2f", quartiles.get(s).get(i));
                }
                wf(wr, "\n");
                wr.flush();
            }
            wr.close();
        } catch (IOException e) {
            logExp(e);
        }

    }

    public Map<String, Map<Integer, Double>> updateStats() {

        int week_day = execDate.getDayOfWeek();
        int d = (7 - week_day) + 1;

        lastMonday = execDate.minusDays(week_day-1).minusDays(7);

        Writer wr;
        try {
            wr = getWriter(STATS_FILENAME);
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            logExp(e);
            return null;
        }

        HashMap<String, List<Double>> stats = new HashMap<>();

        for (String mode : l_mode) {
            stats.put(mode, new ArrayList<Double>());
        }

        Map<String, Player> m_player = facade.readGameState(cfg.get("GAME_ID"));

        // update(stats, "24440");

        for (String pId: m_player.keySet()) {
            update(stats, m_player.get(pId));
        }

        /*

        HashMap<String, double[]> t_stats = new HashMap<String, double[]>();


        for (String s : stats.keySet()) {
            List<Double> aux = stats.get(s);
            double[] aux2 = new double[aux.size()];
            for (int i = 0; i < aux.size(); i++)
                aux2[i] = aux.get(i);
            Arrays.sort(aux2);
            t_stats.put(s, aux2);
        } */

        quartiles = new HashMap<>();

        for (String mode : l_mode) {

            if (stats.get(mode).isEmpty()) {
                quartiles.put(mode, emptyQuartiles());
            } else {
                Map<Integer, Double> res = Quantiles.scale(10).indexes(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).compute(stats.get(mode));
                quartiles.put(mode, res);
            }
        }

        writeStats(wr);

        return quartiles;
    }

    private Map<Integer, Double> emptyQuartiles() {
        Map<Integer, Double> q = new HashMap<>();
        for (int i = 0; i <10; i++)
            q.put(i, 0.0);
        return q;
    }

    private void update(HashMap<String, List<Double>> stats, Player cnt) {

        State st = cnt.getState();

        // p(st.getPointConcept());

        for (PointConcept pc : st.getPointConcept()) {

            String m = fixMode(pc.getName());

            /*
            if (pc.getName().equals(cfg.gLeaves)) {
                stats.get(cfg.gLeaves).add(pc.getPeriodScore("weekly", execDate.getTime()));
            }*/

            if (!ArrayUtils.find(m, l_mode))
                continue;

            Double score = pc.getPeriodScore("weekly", lastMonday);
            if (score > 0)
                stats.get(m).add(score);
        }
    }


    public Map<Integer, Double> getQuantiles(String mode) {
        return quartiles.get(fixMode(mode));
    }

    public int getPosition(String mode, Double modeValue) {
       Map<Integer, Double> quan = getQuantiles(fixMode(mode));
       if (quan == null)
           p("EHM");
            for (int i = 0; i < 10; i++)
                if (modeValue < quan.get(i))
                    return i;

            return 10;
        }

}
