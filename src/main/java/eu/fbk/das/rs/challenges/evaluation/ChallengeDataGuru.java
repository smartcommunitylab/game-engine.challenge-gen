package eu.fbk.das.rs.challenges.evaluation;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.ChallengeUtil;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.utils.Utils;
import it.smartcommunitylab.model.ChallengeAssignmentDTO;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.GameConcept;
import it.smartcommunitylab.model.ext.PointConcept;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;

import static eu.fbk.das.rs.utils.Utils.*;

/**
 * This script collects the available data on the challenges assigned during the 2017 edition.
 * The goal is to have a training data for a classifier able to estimate the "likeness" of completion.
 */
public class ChallengeDataGuru extends ChallengeUtil  {

    private List<Integer> training_model = new ArrayList<>();
    private List<Integer> training_counter = new ArrayList<>();

    private Set<String> cha_completed;


    private List<double[]> training_data;
    // private List<Double> training_label;

    private String[] challengeType = new String[]{"absoluteIncrement", "percentageIncrement", "repetitiveBehaviour"};

    // private String[] challengeName = new String[]{"Bus_Km", "green leaves", "PandR_Trips", "Transit_Trips", "BikeSharing_Km", "Bike_Trips", "Car_Km", "BikeSharing_Trips", "Walk_Trips", "Train_Km", "Recommendations", "Walk_Km", "Bus_Trips", "Car_Trips", "Train_Trips", "Bike_Km", "NoCar_Trips", "ZeroImpact_Trips"};
    private String[] challengeName = new String[]{"green leaves", "Walk_Km", "Bike_Km", "Bus_Trips", "Train_Trips"};

    private String[] pointConcept = new String[]{"green leaves", "Walk_Km", "Bike_Km", "Bus_Trips", "Train_Trips", "NoCar_Trips", "ZeroImpact_Trips", "Walk_Trips", "Bike_Trips", "Bus_Km", "Train_Km", "PandR_Trips", "Transit_Trips", "BikeSharing_Km", "Car_Km", "BikeSharing_Trips", "Recommendations", "Car_Trips"};

    int datum_length = 206;
    private int datum_start;
    private DateTime date;
    private ArrayList<ChallengeAssignmentDTO> l_cha;

    protected SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public ChallengeDataGuru() {
        this(new RecommendationSystem ());
    }

    public ChallengeDataGuru(RecommendationSystem rs) {
        super(rs);

        // all = new HashMap<>();
        cha_completed = new HashSet<>();

        training_data = new ArrayList<>();
        // training_label = new ArrayList<>();
    }

    public void generate(String out_path, Map<String, List<ChallengeExpandedDTO>> challenges, DateTime date, String[] challengeColNames) throws IOException {

        // Read challenge cha_completed list
        //   readCompleted();
        //    p(cha_completed);

        // Read challenge assigned list
        //    readAll();

        this.date = date;

        computeData(challenges);

        writeDown(out_path, challengeColNames);
    }

    private void writeDown(String out_path, String[] challengeColNames) throws IOException {

        /*
        Writer wr = Utils.getWriter(out_path + "training_label");
        for (int i = 0; i < training_label.size(); i++)
            wf(wr, "%.2f\n", training_label.get(i));
        wr.close();
        */


        Writer wr = Utils.getWriter(out_path + ".csv");
        wf(wr, ",%s", joinArray(challengeColNames));
        for (String aChallengeType : challengeType) {
            wf(wr, ",is_%s", aChallengeType);
        }
        for (String aChallengeName : challengeName) {
            wf(wr, ",is_%s", aChallengeName);
        }
        for (String aPointConcept : pointConcept) {

            wf(wr, ",%s", aPointConcept);

            for (int j = 1; j < 4; j++)
                wf(wr, ",%s_w%d", aPointConcept, j);

            for (int j = 1; j < 8; j++)
                wf(wr, ",%s_d%d", aPointConcept, j);
        }
        wf(wr, "\n");


        for (int i = 0; i < training_data.size(); i++) {
            wf(wr, "%s,", l_cha.get(i));
            wf(wr, "%s\n", Utils.joinArray(training_data.get(i), ","));
            wr.flush();
        }
        wr.close();

        wr = Utils.getWriter(out_path + ".arff");
        wf(wr, "@relation playgo\n\n");
        wf(wr, "@attribute model {0,1,2}\n");
        wf(wr, "@attribute counter {0,1,2,3,4}\n", Utils.joinArray(challengeName).replace("\"", ""));
        for (int i = datum_start; i < datum_length; i++)
            wf(wr, "@attribute w%d numeric\n", i);

        wf(wr, "@attribute class {0, 1}\n");
        wf(wr, "\n@data\n");
        wr.flush();
        for (int i = 0; i < training_data.size(); i++) {
            double[] datum = training_data.get(i);
            wf(wr, "%d,%d", training_model.get(i), training_counter.get(i));
            for (int j = datum_start; j < datum_length; j++)
                wf(wr, ",%.3f", datum[j]);
            // wf(wr, ",%d\n", (int) Math.round(training_label.get(i)));
            wf(wr, ",%s\n", "?");
            wr.flush();
        }
        wr.close();

    }

    private void computeData(Map<String, List<ChallengeExpandedDTO>> challenges) {

        l_cha = new ArrayList<>();

        for (String pId : challenges.keySet()) {
            PlayerStateDTO cnt = rs.facade.getPlayerState(rs.gameId, pId);

            for (ChallengeExpandedDTO cha : challenges.get(pId)) {
                goChallenge(cnt, cha);

                l_cha.add(cha);
            }
        }

        // p(training_data.size());
    }

    private void normalizeData() {

        // TODO usage

        datum_start = 10;

        // compute means
        double[] means = new double[datum_length];
        for (double[] datum : training_data) {
            for (int i = datum_start; i < means.length; i++)
                means[i] += datum[i];
        }
        for (int i = datum_start; i < means.length; i++)
            means[i] /= training_data.size();

        // compute std
        double[] std = new double[datum_length];
        for (double[] datum : training_data) {
            for (int i = datum_start; i < means.length; i++)
                std[i] += Math.pow(means[i] - datum[i], 2);
        }
        for (int i = datum_start; i < means.length; i++) {
            std[i] /= training_data.size();
            std[i] = Math.sqrt(std[i]);
        }

        // Normalize data
        for (double[] datum : training_data) {
            for (int i = datum_start; i < means.length; i++)
                if (std[i] != 0)
                    datum[i] = (datum[i] - means[i]) / std[i];
        }
    }

    private void goChallenge(PlayerStateDTO user, ChallengeExpandedDTO cha) {

        String cId = cha.getInstanceName();

        if (!cId.startsWith("w"))
            return;

        for (String s : new String[]{"check_in", "pioneer", "next_badge", "aficionado"})
            if (cId.contains(s))
                return;

        String s_week = cId.substring(0, cId.indexOf("_")).replace("w", "");

        int week = Integer.valueOf(s_week);

        // TODO remove ? consider only week 3 forward (we need the others as a baseline
        // if (week < 3)
        //     return;

        computeDatum(cha, user);
    }

    private void computeDatum(ChallengeExpandedDTO cha, PlayerStateDTO user) {
        double[] datum = new double[datum_length];
        int ix = 0;

        // cntT -  challenge type: (3) absoluteIncrement, percentageIncrement, repetitiveBehaviour
        // encoding: 3 inputs, 1 active
        int ix_model = -1;
        for (int i = 0; i < challengeType.length; i++) {
            if (challengeType[i].equals(cha.getModelName())) {
                datum[ix] = 1;
                ix_model = i;
            }
            ix++;
        }
        if (ix_model < 0)
            p("challengeType NOT FOUND!!!");

        // cntN - challenge name: (18) Bus_Km, green leaves, PandR_Trips, Transit_Trips, BikeSharing_Km, Bike_Trips, Car_Km, BikeSharing_Trips, Walk_Trips, Train_Km, Recommendations, Walk_Km, Bus_Trips, Car_Trips, Train_Trips, Bike_Km, NoCar_Trips, ZeroImpact_Trips
        // encoding: 18 inputs, 1 active
        int ix_counter = -1;
        for (int i = 0; i < challengeName.length; i++) {
            if (challengeName[i].equals(getField(cha, "counterName"))) {
                datum[ix] = 1;
                ix_counter = i;
            }
            ix++;
        }
        if (ix_counter < 0)
            p("challengeName NOT FOUND!!!");

        // p(ix);


        // Per ogni PointConcept (18): Bus_Km, green leaves, PandR_Trips, Transit_Trips, BikeSharing_Km, Bike_Trips, Car_Km, BikeSharing_Trips, Walk_Trips, Train_Km, Recommendations, Walk_Km, Bus_Trips, Car_Trips, Train_Trips, Bike_Km, NoCar_Trips, ZeroImpact_Trips
        for (String aPointConcept : pointConcept) {

            PointConcept pc = getPointConcept(aPointConcept, user);
            if (pc == null)
                continue;

            //  - Current score
            datum[ix++] = pc.getScore();

            DateTime d;

            //  - weekly score: settimana precedente, quella prima e quella prima ancora (0 se non aveva giocato)
            for (int i = 1; i < 4; i++) {
                try {
                    d = date.minusDays(7 * i);
                    // p(Utils.printDate(d));
                    Double a = pc.getPeriodScore("weekly", d.getMillis());
                    datum[ix++] = a;
                } catch (Exception e) {
                    p(e);
                }
            }

            // 6 giorni precedenti
            for (int i = 1; i < 8; i++) {
                d = date.minusDays(i);
                // p(Utils.printDate(d));
                try {
                    datum[ix++] = pc.getPeriodScore("daily", d.getMillis());
                } catch (Exception e) {
                    p(e);
                }
            }

            // p(pc.getName());

        }

        // p(ix);


        training_data.add(datum);
        // training_label.add((double) (cha.isCompleted() ? 1 : 0));
        training_model.add(ix_model);
        training_counter.add(ix_counter);
    }

    private PointConcept getPointConcept(String s, PlayerStateDTO state) {

        for (GameConcept pc2 : state.getState().get("PointConcept")) {
            if (pc2.getName().equals(s)) {
                return (PointConcept) pc2;
            }

        }

        pf("POINT CONCEPT NOT FOUND!!! %s", s);
        return null;
    }

    private Object getField(ChallengeExpandedDTO cha, String s) {
        return cha.getData(s);
    }

    private double parse(String s) {
        if ("".equals(s))
            return 0;
        else
            return Double.parseDouble(s);
    }

    private long getDateFromStart(int n_week, int n_days) {

        DateTime dt = stringToDate("4/07/2017");

        Calendar cal = Calendar.getInstance();
        int to_add = n_week * 7 + n_days;
        dt.plusDays(to_add);

        /// p(dt);
        return dt.getMillis() / 1000;
    }

    /*
    private Map<String, ChallengeDt> all;

    private class ChallengeDt {

        private String pId;  // PlayerStateDTO id

        private String cntT;  // challenge type

        private String cntN; // counter name

        private String bsl; // baseline

        public String tgt; // target

        public String pz; // prize

        public String rw; // reward

        public String id; // id
    }



    protected void readAll(String path) throws IOException {
        BufferedReader rd = Utils.getReader(path + "challenges_combined.csv");
        String ln = rd.readLine();
        while (ln != null) {
            String[] aux = ln.split(";");
            if (!aux[8].startsWith("w"))
                continue;

            ChallengeDt cdt = new ChallengeDt();
            cdt.pId = aux[0];
            cdt.cntT = aux[2];
            cdt.cntN = aux[3];
            cdt.bsl = aux[4];
            cdt.tgt = aux[5];
            cdt.pz = aux[6];
            cdt.rw = aux[7];
            cdt.id = aux[8];
            ln = rd.readLine();

            all.put(cdt.id, cdt);
        }

    } */

    protected void readCompleted(String path) throws IOException {
        BufferedReader rd = Utils.getReader(path + "ChallengeCompletedFiltered.csv");
        String ln = rd.readLine();
        ln = rd.readLine();

        while (ln != null) {
            String[] aux = ln.split(",");
            cha_completed.add(aux[aux.length - 1].replace("\"", ""));
            ln = rd.readLine();
        }

    }
}





