package eu.fbk.das.rs.challenges.evaluation;

import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.trentorise.game.challenges.rest.ChallengeConcept;
import eu.trentorise.game.challenges.rest.Player;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static eu.fbk.das.rs.utils.Utils.*;

public class ChallengeAnalyzer extends ChallengeDataGuru {

    private Map<String, List<Double>> incrByCounter;
    private ArrayList<List<Double>> incrByLvl;
    private ArrayList<Double> incrTot;

    public String[] files = new String[] {"challenges-2018-11-14-complete.csv", "challenges-2018-11-20-complete.csv", "challenges-2018-11-27-complete.csv"};

    private int next_p;
    private int s;

    String anim = "\\|/|";

    private int linesTot;
    private int linesCurrent;

    private long last_adv;

    private Map<String, Integer> counterChosen;
    private Map<String, Integer> counterTgt;

    public ChallengeAnalyzer(RecommendationSystemConfig cfg) {
        super(cfg);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ChallengeAnalyzer cdg = new ChallengeAnalyzer(new RecommendationSystemConfig());
        cdg.analyze();
    }

    private void analyze() throws IOException, InterruptedException {
        p(cfg.get("GAME_ID"));

        facade = new GamificationEngineRestFacade(cfg.get("HOST"), cfg.get("USERNAME"), cfg.get("PASSWORD"));

        String path = "/home/loskana/Desktop/challenges/";

        prepare();

        linesTot = 0;
        for (String s: files)
            countLines(path + s);

        linesCurrent = 0;
        for (String s: files)
            analyzeFile(path , s);

        output();
    }

    private void countLines(String path) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            line = reader.readLine();
            while (line != null) {
                linesTot++;
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            logExp(e);
        }
    }

    private void output() {
        pf("\n\n DONE!");

        pf("\n\n");
        for (String s: counterTgt.keySet()) {
         pf("# Chosen on %s: %d / %d - %.2f %% \n", s, counterChosen.get(s), counterTgt.get(s), counterChosen.get(s) * 100.0/ counterTgt.get(s));
        }

        pf("\n\nGeneral improvement: %s ", an(incrTot));

        pf("\n\n");
        for (int i = 0; i < incrByLvl.size(); i++) {
            pf("# Level %d: %s\n", i, an(incrByLvl.get(i)));
        }

        pf("\n\n");
        for (String c: incrByCounter.keySet()) {
            pf("# Counter %s: %s\n", c, an(incrByCounter.get(c)));
        }
    }

    public static String an(List<Double> ld) {
            double sum = 0;
            for(double d: ld)
                sum += d;
            double mean = sum / ld.size();
            double sq_diff_sum = 0;
            for(double d: ld) {
                double diff = d - mean;
                sq_diff_sum += diff * diff;
            }
            double variance = sq_diff_sum / ld.size();
            return f("%.2f - %.2f (%d)", mean, Math.sqrt(variance), ld.size());
        }


    private void prepare() {
        incrTot = new ArrayList<Double>();
        incrByLvl = new ArrayList<List<Double>>();
        incrByCounter = new HashMap<>();

        // playersTot = facade.getGamePlayers(cfg.get("GAME_ID"));
        // playersDone = new HashSet<String>();
        next_p = 0;

        s = 1;

        last_adv = System.currentTimeMillis();

        counterTgt = new HashMap<>();
        counterChosen = new HashMap<>();
    }

    public void analyzeFile(String path, String file) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path + file));
            String line = reader.readLine();
            line = reader.readLine();
            while (line != null) {
                // System.out.println(line);
                experiment(new ChallengeRecord(line), file);
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            logExp(e);
        }
    }

    private void experiment(ChallengeRecord cr, String file) {

        showAdvancement(cr);

        // Ignore non-target experiments
        if (!"tgt".equals(cr.exp))
            return;

        incr(counterTgt, "gen", file);

        // Check only challenges that have been chosen (not automatically assigned)
        if (!cr.chosen)
            return;

        incr(counterChosen, "gen", file);

        incrTot.add(cr.impr);

        while (incrByLvl.size() <= cr.lvl)
            incrByLvl.add(new ArrayList<Double>());
        incrByLvl.get(cr.lvl).add(cr.impr);

        if (!incrByCounter.containsKey(cr.counter))
            incrByCounter.put(cr.counter, new ArrayList<Double>());
        incrByCounter.get(cr.counter).add(cr.impr);
    }

    private void incr(Map<String, Integer> counter, String s, String s2) {
        if (!counter.containsKey(s))
            counter.put(s, 0);
        counter.put(s, counter.get(s) +1);
        if (!counter.containsKey(s2))
            counter.put(s2, 0);
        counter.put(s2, counter.get(s2) +1);
    }

    private void showAdvancement(ChallengeRecord cr) {

        // playersDone.add(cr.pId);
        linesCurrent++;

        int r = (linesCurrent * 10) / linesTot;
        String h = "";
        for (int i = 0; i < r; i++)
            h += "=";

        long now = System.currentTimeMillis();
        if (now > last_adv + 500) {
            s++;
            if (s>3)
                s = 0;
            last_adv = now;
        }
        h += anim.charAt(s);


        while (h.length() < 10)
            h += " ";

        String data = f("\rAnalyzing: [%s] %d / %d",h,  linesCurrent, linesTot);

        try {
            System.out.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private class ChallengeRecord {

        private final String name;
        private final String pId;
        private final int lvl;

        private final double baseline;
        private final double target;
        private final String counter;
        private final String model;
        private final String exp;
        private final int id;
        private final double impr;

        private boolean chosen;
        private boolean completed;

        private Map<String, Date> stateDate;

        public ChallengeRecord(String line) {
            String[] spl = line.split(",");

            int ix = 0;
            name = s(spl[ix++]);
            pId = s(spl[ix++]);
            lvl = i(spl[ix++]);
            id = i(spl[ix++]);
            exp = s(spl[ix++]);
            model = s(spl[ix++]);
            counter = s(spl[ix++]);
            baseline = d(spl[ix++]);
            target = d(spl[ix++]);
            impr = d(spl[ix++]);

            searchChallenge();
        }

        private String s(String s) {
            return s.trim();
        }

        private double d(String s) {
            return Double.valueOf(s.trim());
        }

        private int i(String s) {
            return Integer.valueOf(s.trim());
        }

        private void searchChallenge() {

            Player state = facade.getPlayerState(cfg.get("GAME_ID"), pId);

            for (ChallengeConcept cha: state.getState().getChallengeConcept() ) {
                if (!cha.getName().equals(name))
                    continue;

                chosen = checkChosen(cha.getStateDate());
                completed = cha.isCompleted();

                return;
            }

            chosen = false;
            completed = false;
        }

        private boolean checkChosen(Map<String, Date> stateDate) {
            for (String k: stateDate.keySet()) {
                if (!k.equals("ASSIGNED"))
                    continue;

                DateTime d = new DateTime(stateDate.get(k));
                if (d.getDayOfWeek() < 5)
                    return true;

                if (d.getHourOfDay() < 12)
                    return true;

                return false;
            }

            p("ASSIGNED NOT FOUND!!!!!");
            return false;
        }

    }
}


