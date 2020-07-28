package eu.fbk.das.rs.challenges.generation;

import com.google.common.math.Quantiles;
import eu.fbk.das.rs.challenges.ChallengeUtil;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.utils.ArrayUtils;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.*;
import java.util.*;

import static eu.fbk.das.rs.challenges.generation.RecommendationSystem.fixMode;
import static eu.fbk.das.utils.Utils.*;

public class RecommendationSystemStatistics extends ChallengeUtil {

    private String STATS_FILENAME = "rs.statistics";

    protected DateTime execDate;
    private String[] l_mode;
    private Map<String, Map<Integer, Double>> quartiles;
    private boolean offline = true;

    private DateTime lastMonday;
    private String host;

    private Map<String, Map<String, Map<Integer, Double>>> cache;
    private String lastMondayKey;

    public RecommendationSystemStatistics(RecommendationSystem rs) {
        super(rs);
        quartiles = new HashMap<>();

        this.l_mode = ArrayUtils.cloneArray(ChallengesConfig.defaultMode);
        for (int i = 0; i < l_mode.length; i++)
            l_mode[i] = fixMode(l_mode[i]);
        Arrays.sort(this.l_mode);

        cache = new HashMap<>();
    }

    public RecommendationSystemStatistics(RecommendationSystem rs, boolean takeOnlineStats) {
        this(rs);
        offline = !takeOnlineStats;
    }

    public Map<String, Map<Integer, Double>> checkAndUpdateStats(DateTime date) {
        this.execDate = date;

        int week_day = execDate.getDayOfWeek();
        int d = (7 - week_day) + 1;

        lastMonday = execDate.minusDays(week_day-1).minusDays(7);
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
        lastMondayKey = fmt.print(lastMonday);
        if (cache.containsKey(lastMondayKey)) return cache.get(lastMondayKey);

        if (offline)
            return updateStatsOffline();

        return updateStatsOnline();

    }

    private Map<Integer,Double> convert(Map<String,Double> statsData) {
        Map<Integer,Double> data = new HashMap<>();
        for(String key : statsData.keySet()) {
            data.put(Integer.valueOf(key), statsData.get(key));
        }
        return data;
    }

    private Map<String, Map<Integer, Double>> updateStatsOnline() {
        Map<String, Map<Integer, Double>> stats = new HashMap<>();
        for (String pointConceptName : ChallengesConfig.defaultMode) {
            Map<Integer, Double> statsData =
                    rs.facade.readGameStatistics(rs.gameId, lastMonday, pointConceptName).stream()
                            .map(data -> convert(data.getQuantiles())).findFirst()
                            .orElse(emptyQuartiles());
            stats.put(fixMode(pointConceptName), statsData);
        }
        quartiles = stats;

        cache.put(lastMondayKey, quartiles);

        return stats;
    }

    private Map<String, Map<Integer, Double>> updateStatsOffline() {
        quartiles = tryReadStats();

        if (quartiles == null)
            quartiles = updateStats();

        cache.put(lastMondayKey, quartiles);

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

            String[] aux = fileDateStr.trim().split(" ");

            if (aux.length < 2)
                return null;

            DateTime fileDate = stringToDate(aux[0]);
            if (fileDate == null)
                return null;

            if (!aux[1].equals(host))
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
            wf(wr, "%s %s\n", formatDate(execDate), host);
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
        stats.put(ChallengesConfig.gLeaves, new ArrayList<Double>());

        Set <String> m_player = rs.facade.getGamePlayers(rs.gameId);

        // update(stats, "24440");

        for (String pId: m_player) {
            PlayerStateDTO st = rs.facade.getPlayerState(rs.gameId, pId);
            update(stats, st);
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

    private void update(HashMap<String, List<Double>> stats, PlayerStateDTO cnt) {

        Set<GameConcept> scores =  cnt.getState().get("PointConcept");
        for (GameConcept gc : scores) {
            PointConcept pc = (PointConcept) gc;

            String m = fixMode(pc.getName());

            if (pc.getName().equals(ChallengesConfig.gLeaves)) {
                stats.get(ChallengesConfig.gLeaves).add(getPeriodScore(pc,"weekly", execDate));
            }

            if (!ArrayUtils.find(m, l_mode))
                continue;

            Double score = getPeriodScore(pc,"weekly", lastMonday);
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
