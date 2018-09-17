package eu.fbk.das.rs;

import eu.trentorise.challenge.BaseTest;
import eu.trentorise.game.challenges.rest.ChallengeConcept;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.PointConcept;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static eu.fbk.das.rs.Utils.*;

/**
 * This script collects the available data on the challenges assigned during the 2017 edition.
 * The goal is to have a training data for a classifier able to estimate the "likeness" of completion.
 */
public class ChallengeDataGuru extends BaseTest {

    private final HashMap<String, Content> userCache;
    private GamificationEngineRestFacade facade;

    private String out_path = "/home/loskana/Desktop/tensorflow/play-classifier/";
    private List<Integer> training_model = new ArrayList<>();
    private List<Integer> training_counter = new ArrayList<>();

    public ChallengeDataGuru() {
        all = new HashMap<>();
        completed = new HashSet<>();
        userCache = new HashMap<String, Content>();
    }

    String path = "src/test/resources/eu/fbk/das/rs/";

    private Set<String> completed;
    private Map<String, ChallengeDt> all;

    private List<double[]> training_data = new ArrayList<>();
    private List<Double> training_label = new ArrayList<>();

    private String[] challengeType = new String[] {"absoluteIncrement", "percentageIncrement", "repetitiveBehaviour"};

    private String[] challengeName = new String[] {"Bus_Km", "green leaves", "PandR_Trips", "Transit_Trips", "BikeSharing_Km", "Bike_Trips", "Car_Km", "BikeSharing_Trips", "Walk_Trips", "Train_Km", "Recommendations", "Walk_Km", "Bus_Trips", "Car_Trips", "Train_Trips", "Bike_Km", "NoCar_Trips", "ZeroImpact_Trips"};

    int datum_length = 95;
    private int datum_start;

    private class ChallengeDt {

        private String pId;  // player id

        private String cntT;  // challenge type

        private String cntN; // counter name

        private String bsl; // baseline

        public String tgt; // target

        public String pz; // prize

        public String rw; // reward

        public String id; // id
    }

    @Before
    public void setup() {
        training_data = new ArrayList<>();
        training_label = new ArrayList<>();
        facade = new GamificationEngineRestFacade(HOST, USERNAME, PASSWORD);
    }


    @Test
    public void generate() throws IOException {

        // Read challenge completed list
     //   readCompleted();
    //    p(completed);

        // Read challenge assigned list
      //    readAll();

        computeData();

        writeDown();
    }

    private void writeDown() throws IOException {

        Writer wr = Utils.getWriter(out_path + "training_label");
        for (int i = 0; i < training_label.size(); i++)
            wf(wr, "%.2f\n", training_label.get(i));
        wr.close();


        wr = Utils.getWriter(out_path + "training_data");
        for (int i = 0; i < training_data.size(); i++) {
            wf(wr, "%s\n", Utils.joinArray(training_data.get(i), ","));
            wr.flush();
        }
        wr.close();

        wr = Utils.getWriter(out_path + "data.arff");
        wf(wr, "@relation playgo\n\n");
        wf(wr, "@attribute model {0,1,2}\n");
        wf(wr, "@attribute counter {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17}\n", Utils.joinArray(challengeName).replace("\"", ""));
        for (int i = datum_start ; i < datum_length; i++)
            wf(wr, "@attribute w%d numeric\n", i);

        wf(wr, "@attribute class {0, 1}\n");
        wf(wr, "\n@data\n");
        wr.flush();
        for (int i = 0; i < training_data.size(); i++) {
            double[] datum = training_data.get(i);
            wf(wr, "%d,%d", training_model.get(i), training_counter.get(i));
            for (int j = datum_start ; j < datum_length; j++)
                wf(wr, ",%.3f", datum[j]);
            wf(wr, ",%d\n", (int) Math.round(training_label.get(i)));
            wr.flush();
        }
        wr.close();

    }

    private void computeData() {

        Set<String> players = facade.getGamePlayers(GAMEID);


        for (String pId: players) {

            Content user = getContent(pId);

            for (ChallengeConcept cha: user.getState().getChallengeConcept()) {

                goChallenge(user, cha);
            }
        }

        datum_start = 20;

        // compute means
        double[] means = new double[datum_length];
        for (double[] datum: training_data) {
            for (int i = datum_start; i < means.length; i++)
                means[i] += datum[i];
        }
        for (int i = datum_start; i < means.length; i++)
            means[i] /= training_data.size();

        // compute std
        double[] std = new double[datum_length];
        for (double[] datum: training_data) {
            for (int i = datum_start; i < means.length; i++)
                std[i] += Math.pow(means[i] - datum[i], 2);
        }
        for (int i = datum_start; i < means.length; i++) {
            std[i] /= training_data.size();
            std[i] = Math.sqrt(std[i]);
        }

        // Normalize data
        for (double[] datum: training_data) {
            for (int i = datum_start; i < means.length; i++)
                if (std[i] != 0)
                    datum[i] = (datum[i] - means[i]) / std[i];
        }

        p(training_data.size());
    }

    private void goChallenge(Content user, ChallengeConcept cha) {
        String cId = cha.getName();

        if (!cId.startsWith("w"))
            return;

        for (String s: new String[] {"check_in", "pioneer", "next_badge", "aficionado"})
            if (cId.contains(s))
                return;

        String s_week = cId.substring(0, cId.indexOf("_")).replace("w", "");

        int week =  Integer.valueOf(s_week);

        // consider only week 3 forward (we need the others as a baseline
        if (week < 3)
            return;

        computeDatum(cha, week, user);
    }

    private void computeDatum(ChallengeConcept cha, Integer week, Content user) {
        double[] datum = new double[datum_length];
        int ix = 0;

        // cntT -  challenge type: (3) absoluteIncrement, percentageIncrement, repetitiveBehaviour
        // encoding: 3 inputs, 1 active
        int ix_model = -1;
        for (int i = 0; i < challengeType.length; i++) {
            if (challengeType[i].equals(cha.getModelName())) {
                datum[ix++] = 1;
                ix_model = i;
            }
        }
        if (ix_model < 0)
            p("NOT FOUND!!!");

        // cntN - challenge name: (18) Bus_Km, green leaves, PandR_Trips, Transit_Trips, BikeSharing_Km, Bike_Trips, Car_Km, BikeSharing_Trips, Walk_Trips, Train_Km, Recommendations, Walk_Km, Bus_Trips, Car_Trips, Train_Trips, Bike_Km, NoCar_Trips, ZeroImpact_Trips
        // encoding: 18 inputs, 1 active
        int ix_counter = -1;
        for (int i = 0; i < challengeName.length; i++) {
            if (challengeName[i].equals(getField(cha, "counterName"))) {
                datum[ix++] = 1;
                ix_counter = i;
            }
        }
        if (ix_counter < 0)
            p("NOT FOUND!!!");

        try {
            // target - numerico
            datum[ix++] = (double) getField(cha, "target");

            // prize - numerico
            datum[ix++] = (double) getField(cha, "bonusScore");
        } catch (Exception e) {
            p(e);
        }


        // Per ogni PointConcept (18): Bus_Km, green leaves, PandR_Trips, Transit_Trips, BikeSharing_Km, Bike_Trips, Car_Km, BikeSharing_Trips, Walk_Trips, Train_Km, Recommendations, Walk_Km, Bus_Trips, Car_Trips, Train_Trips, Bike_Km, NoCar_Trips, ZeroImpact_Trips
        for (PointConcept pc : user.getState().getPointConcept()) {

            //  - Current score
            datum[ix++] = pc.getScore();

            double tot = 0;

            //  - weekly score: settimana precedente, quella prima e quella prima ancora (0 se non aveva giocato)
            for (int i = 0; i < 3; i++) {
                try {
                    Double a = pc.getPeriodScore("weekly", getDateFromStart(week - (i + 1), 0));
                    datum[ix++] = a;
                    tot += a;
                } catch (Exception e) {
                    p(e);
                }
            }

            if (tot <= 0 && "green leaves".equals(pc.getName()))
                return;

            //  - daily score: 7 giorni precedenti?
            // for (int i = 0; i < 3; i++) {
                 // datum[ix++] = pc.getPeriodScore("daily", getDateFromStart(week - 1, i));
            // }

        }


        training_data.add(datum);
        training_label.add((double) (cha.getCompleted() ? 1 : 0));
        training_model.add(ix_model);
        training_counter.add(ix_counter);
    }

    private Object getField(ChallengeConcept cha, String a) {
        return cha.getFields().get(a);
    }

    private double parse(String s) {
        if ("".equals(s))
            return 0;
        else
        return Double.parseDouble(s);
    }

    private long getDateFromStart(int n_week, int n_days) {

        Date dt = stringToDate("4/07/2017");

        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        int to_add = n_week * 7 + n_days;
        cal.add(Calendar.DATE, to_add);

        dt = cal.getTime();
        /// p(dt);
        return dt.getTime();
    }

    private Content getContent(String pId) {
        if (userCache.get(pId) == null) {
            Content cnt = facade.getPlayerState(GAMEID, pId);
            userCache.put(pId, cnt);
            return cnt;
        }

        return userCache.get(pId);
    }

    private void readAll() throws IOException {
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

    }

    private void readCompleted() throws IOException {
        BufferedReader rd = Utils.getReader(path + "ChallengeCompletedFiltered.csv");
        String ln = rd.readLine();
        ln = rd.readLine();

        while (ln != null) {
            String[] aux = ln.split(",");
            completed.add(aux[aux.length -1].replace("\"", ""));
            ln = rd.readLine();
        }

    }
}


