package eu.fbk.das.rs.challenges.evaluation;

import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.trentorise.game.challenges.rest.ChallengeConcept;
import eu.trentorise.game.challenges.rest.Player;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static eu.fbk.das.rs.utils.Utils.*;

public class ChallengeAnalyzer extends ChallengeDataGuru {

    protected boolean checkIfChosen = true;
    private Map<String, List<Double>> incrByCounter;
    private ArrayList<List<Double>> incrByLvl;
    private ArrayList<Double> incrTot;

    // "challenges-2018-11-14-complete.csv", "challenges-2018-11-20-complete.csv", "challenges-2018-11-27-complete.csv"
    public String[] files = new String[] {"week-7/challenges-2018-12-12-complete.csv"};

    String path = "/home/loskana/Documents/GamifDrive/Trento_Play&Go/challenges/";
    private int next_p;

    String anim = "\\|/|-\\";

    private int linesTot;
    private int linesCurrent;

    private long last_adv;

    private Map<String, Map<String, Integer>> counter;

    private int cnt;

    private Set<String> playersChosen;
    private Set<String> playersAll;

    protected Map<String, List<ChallengeRecord>> challenges;

    private long now;

    private long last;

    protected int weekStart = 1;

    private int weekEnd = 24;

    public ChallengeAnalyzer(RecommendationSystem rs) {
        super(rs);
    }

    public static void main(String[] args) {
        ChallengeAnalyzer cdg = new ChallengeAnalyzer(new RecommendationSystem(conf.get("HOST"), conf.get("USER"), conf.get("PASS")));

        cdg.analyzeSelected();
        // cdg.analyzeAll();
    }

    protected void analyzeAll() {
        prepare();

        for (int i = weekStart; i < weekEnd; i++) {
            String n_path = f("%s/week-%d/", path, i);
            File[] listOfFiles = new File(n_path).listFiles();
            if (listOfFiles == null)
                continue;
            for (File f: listOfFiles)
                if (f.getAbsolutePath().endsWith(".csv") && !f.getName().contains("complete") && !f.getName().contains("group"))
                    analyze(n_path, f.getName());
        }
    }


    protected void analyzeSelected() {
        prepare();

        for (String s: files)
            analyze(path, s);
    }

    protected void analyze(String file, String s)  {

        p("");
        p(rs.gameId);
        p(file + s);

        playersAll = new HashSet<>();
        playersChosen = new HashSet<>();

        linesTot = 0;
        countLines(file + s);

        linesCurrent = 0;

        challenges = new HashMap<String, List<ChallengeRecord>>();

        analyzeFile(file , s);

        experiment(file);

        output();
    }

    protected void experiment(String file) {
        for (String pId: challenges.keySet()) {
            List<ChallengeRecord> chas = challenges.get(pId);
            for (ChallengeRecord cha: chas) {
                experimentChallenge(cha, file);
            }
        }
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

    protected void output() {
        pf("\n\n DONE!");

        pf("\n\n");
        pf("# Chosen: %d / %d - %.2f %% \n", playersChosen.size(), playersAll.size(), playersChosen.size() * 100.0 / playersAll.size());

        pf("\n\nGeneral improvement: %s ", an(incrTot));

        pf("\n\n");
        for (int i = 0; i < incrByLvl.size(); i++) {
            pf("# Level %d: %s\n", i, an(incrByLvl.get(i)));
        }

        pf("\n\n");
        for (String c: incrByCounter.keySet()) {
            pf("# Counter %s: %s\n", c, an(incrByCounter.get(c)));
        }

        pf("\n\n");
        pf("%.2f\n", counter.get("counterTgtChosen").get("gen") * 1.0 / counter.get("counterTgtProposed").get("gen"));
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


    protected void prepare() {
        incrTot = new ArrayList<Double>();
        incrByLvl = new ArrayList<List<Double>>();
        incrByCounter = new HashMap<>();

        // playersTot = facade.getGamePlayers(cfg.get("GAME_ID"));
        // playersDone = new HashSet<String>();
        next_p = 0;

        cnt = 1;

        last_adv = System.currentTimeMillis();

        counter = new HashMap<>();

        challenges= new HashMap<>();

    }

    public void analyzeFile(String path, String file) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path + file));
            String line = reader.readLine();
            line = reader.readLine();
            while (line != null) {
                // System.out.println(line);
                ChallengeRecord cha = new ChallengeRecord(line);
                if (!challenges.containsKey(cha.pId))
                    challenges.put(cha.pId, new ArrayList<>(3));
                challenges.get(cha.pId).add(cha);
                // read next line
                line = reader.readLine();

                showAdvancement(cha);
            }
            reader.close();
        } catch (IOException e) {
            logExp(e);
        }
    }

    private void experimentChallenge(ChallengeRecord cr, String file) {


        // Ignore non-target experiments
        // if (!"tgt".equals(cr.exp))
        //     return;

        incr("counterTgt", "gen", file);

        playersAll.add(cr.pId);

        if ("tgt".equals(cr.exp))
            incr("counterTgtProposed", "gen", file);

        // Check only challenges that have been chosen (not automatically assigned)
        if (!cr.chosen)
            return;

        if ("tgt".equals(cr.exp))
            incr("counterTgtChosen", "gen", file);

        playersChosen.add(cr.pId);

        incr("counterChosen", "gen", file);

        incrTot.add(cr.impr);

        while (incrByLvl.size() <= cr.lvl)
            incrByLvl.add(new ArrayList<Double>());
        incrByLvl.get(cr.lvl).add(cr.impr);

        if (!incrByCounter.containsKey(cr.counter))
            incrByCounter.put(cr.counter, new ArrayList<Double>());
        incrByCounter.get(cr.counter).add(cr.impr);
    }

    private void incr(String name, String s, String s2) {

        if (!counter.containsKey(name))
            counter.put(name, new HashMap<>());
        Map<String, Integer> counterS = counter.get(name);

        if (!counterS.containsKey(s))
            counterS.put(s, 0);
        counterS.put(s, counterS.get(s) +1);
        if (!counterS.containsKey(s2))
            counterS.put(s2, 0);
        counterS.put(s2, counterS.get(s2) +1);
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
            cnt++;
            if (cnt>4)
                cnt = 0;
            last_adv = now;
        }
        h += anim.charAt(cnt);


        while (h.length() < 10)
            h += " ";

        String data = f("\rAnalyzing: [%s] %d / %d",h,  linesCurrent, linesTot);

        try {
            System.out.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected class ChallengeRecord {

        protected final String name;
        protected final String pId;
        protected final int lvl;

        public final double baseline;
        public final double target;
        protected final String counter;
        protected final String model;
        public final String exp;
        public final int id;
        protected final double impr;
        public final int priority;
        private final Integer week;
        public final DateTime start;
        private final int difficulty;
        private final int bonus;
        private final String state;

        public boolean chosen;
        protected boolean completed;

        protected Map<String, Date> stateDate;

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
            difficulty = n(spl[ix++]);
            bonus = n(spl[ix++]);
            state = spl[ix++];
            priority = n(spl[ix++]);
            start = dt(spl[ix++]);

            if (checkIfChosen)
            searchChallenge();

            week = getChallengeWeek(start);
        }

        private DateTime dt(String s) {
            try {
                return new DateTime(sdf.parse(s));
            } catch (ParseException e) {
                e.printStackTrace();
            } return null;
        }

        private int n(String s) {
            s = s.trim();
            if ("null".equals(s))
                return -1;
            else if (s.contains("."))
                return (int) d(s);
            else
                return i(s);
        }

        private String s(String s) {
            return s.trim();
        }

        private double d(String s) {
            s = s.trim();
            if ("null".equals(s))
                return -1;
            return Double.valueOf(s.trim());
        }

        private int i(String s) {
            s = s.trim();
            if ("null".equals(s))
                return -1;
                return Integer.valueOf(s.trim());
        }

        private void searchChallenge() {

            Player state = rs.facade.getPlayerState(rs.gameId, pId);

            List<ChallengeConcept> l_cha = state.getState().getChallengeConcept();
            if (l_cha != null)
            for (ChallengeConcept cha: l_cha ) {
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

    protected Integer getChallengeWeek(DateTime d) {
        // get week
        Integer w = d.getWeekOfWeekyear();
        if (d.getYear() == 2019)
            w += 52;
        w-= 43;
        return w;
    }


    protected double getPlayerChosen(List<ChallengeRecord> chas) {
        for (ChallengeRecord cha: chas)
            if (cha.chosen)
                return cha.target;

        return -1;
    }

    protected double getTargetProposed(List<ChallengeRecord> chas) {
        for (ChallengeRecord cha: chas)
            if (cha.priority == 2)
                return cha.target;

        return -1;
    }
}


