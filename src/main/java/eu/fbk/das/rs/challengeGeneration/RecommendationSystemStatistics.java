package eu.fbk.das.rs.challengeGeneration;

import eu.fbk.das.rs.ArrayUtils;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.PointConcept;

import java.io.*;
import java.util.*;

import static eu.fbk.das.rs.ArrayUtils.cloneArray;
import static eu.fbk.das.rs.Utils.*;

public class RecommendationSystemStatistics {

    private final RecommendationSystemConfig cfg;
    private String STATS_FILENAME = "rs.statistics";
    private GamificationEngineRestFacade facade;

    private Date execDate;
    private String[] l_mode;

    public RecommendationSystemStatistics(RecommendationSystemConfig cfg) {
        this.cfg = cfg;
    }

    public HashMap<String, double[]> checkAndUpdateStats(GamificationEngineRestFacade facade, Date date, String[] l_mode) {
        this.facade = facade;

        this.l_mode = cloneArray(l_mode);
        Arrays.sort(this.l_mode);

        execDate = date;

        HashMap<String, double[]> result = tryReadStats();

        if (result != null)
            return result;

        return updateStats();
    }

    private HashMap<String, double[]> tryReadStats() {

        if (!new File(STATS_FILENAME).exists())
            return null;

        try {
            BufferedReader r = getReader(STATS_FILENAME);

            String fileDateStr = r.readLine();
            if (fileDateStr == null || fileDateStr.length() == 0)
                return null;

            Date fileDate = stringToDate(fileDateStr.trim());
            if (fileDate == null)
                return null;

            if (daysApart(fileDate, execDate) >= 6)
                return null;

            return readStats(r);

        } catch (IOException e) {
            logExp(e);
        }

        return null;
    }

    private HashMap<String, double[]> readStats(BufferedReader r) throws IOException {
        HashMap<String, double[]> stats = new HashMap<String, double[]>();
        String line = r.readLine();
        while (line != null) {
            String[] aux = line.split(":");
            String name = aux[0];
            aux = aux[1].split("\\s+");
            double[] values = new double[aux.length];
            for (int i = 1; i < aux.length; i++)
                values[i] = Double.valueOf(aux[i]);
            stats.put(name, values);
            line = r.readLine();
        }

        return stats;
    }

    private void writeStats(Writer wr, HashMap<String, double[]> stats) {
        try {
            wf(wr, "%s\n", sdf.format(execDate));
            for (String s : stats.keySet()) {
                wf(wr, "%s:", s);
                double[] aux = stats.get(s);
                for (int i = 0; i < aux.length; i++) {
                    wf(wr, " %.2f", aux[i]);
                }
                wf(wr, "\n");
                wr.flush();
            }
            wr.close();
        } catch (IOException e) {
            logExp(e);
        }

    }

    private HashMap<String, double[]> updateStats() {

        Writer wr;
        try {
            wr = getWriter(STATS_FILENAME);
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            logExp(e);
            return null;
        }

        HashMap<String, List<Double>> stats = new HashMap<String, List<Double>>();

        for (String mode : l_mode) {
            stats.put(mode, new ArrayList<Double>());
        }
        stats.put(cfg.gLeaves,  new ArrayList<Double>());

        List<Content> l_player = facade.readGameState(cfg.get("GAME_ID"));

        // update(stats, "24440");

        for (Content player : l_player) {
            update(stats, player);
        }

        HashMap<String, double[]> t_stats = new HashMap<String, double[]>();

        for (String s : stats.keySet()) {
            List<Double> aux = stats.get(s);
            double[] aux2 = new double[aux.size()];
            for (int i = 0; i < aux.size(); i++)
                aux2[i] = aux.get(i);
            Arrays.sort(aux2);
            t_stats.put(s, aux2);
        }

        writeStats(wr, t_stats);

        return t_stats;
    }

    private void update(HashMap<String, List<Double>> stats, Content cnt) {


        for (PointConcept pc : cnt.getState().getPointConcept()) {

            String m = pc.getName();
            
            if (pc.getName().equals(cfg.gLeaves)) {
                stats.get(cfg.gLeaves).add(pc.getPeriodScore("weekly", execDate.getTime()));
            }

            if (!ArrayUtils.find(pc.getName(), l_mode))
                continue;

            Double score = pc.getPeriodScore("weekly", execDate.getTime());
            if (score > 0)
                stats.get(m).add(score);
        }
    }


}
