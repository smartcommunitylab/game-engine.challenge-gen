package eu.fbk.das.innowee;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.fbk.das.utils.Utils.pf;

public class CostAssigner {

    String path = "/home/loskana/Desktop/choices.csv";

    private List<String> players;

    private HashMap<String, Map<Integer, Integer>> record;

    private int timelimit = 60;

    private int N;

    int[] costs = new int[] {0, 3, 5, 7, 9, 10, 11, 12};

    @Test
    public void test() {
        CostAssigner ca = new CostAssigner();
            ca.execute();
    }

    public void execute() {

        if (!readChoices()) return;

        N = players.size();

        int[][] costMatrix = getChoicesCost();
        HungarianAlgorithm ha = new HungarianAlgorithm(costMatrix);
        int[][] assignment = ha.findOptimalAssignment();

        int tot_cost = 0;
        for (int[] a: assignment) {
            String pl = players.get(a[1]);
            int ch = a[0] + 1;
            int cost = getPlayerCost(pl, ch);
            tot_cost += cost;
            pf("PlayerStateDTO %s: %d (%d) \n", pl, ch, cost);
        }

        pf("\n\nMean delusion: %.2f - tot delusion: %d \n", tot_cost * 1.0/N, tot_cost);
    }

    private int getPlayerCost(String s, int j) {
        int c = 99;
        if (record.get(s).containsKey(j))
            c = costs[record.get(s).get(j)];
        return c;
    }

/*
    public void execute() {

        if (!readChoices()) return;

        Model model = new Model("group assignment");

         N = players.size();

        int[][] costMatrix = getChoicesCost();

        // variables
        IntVar[][] vs = model.intVarMatrix("vs", N, N, new int[] {0, 1});

        // only one value for row
        for(int i  = 0; i < N; i++) {
            model.sum(vs[i], "=", 1).post();
        }

        // only one value for column
        for(int j  = 0; j < N; j++) {
            IntVar[] aux = new IntVar[N];
            for (int i = 0; i < N; i++)
                aux[i] = vs[i][j];
            model.sum(aux, "=", 1).post();
        }

        IntVar[] aux_cost = model.intVarArray("ac", N, 0, 999999,true);
        for(int i  = 0; i < N; i++) {
            model.scalar(vs[i], costMatrix[i], "=", aux_cost[i]).post();
        }
        IntVar tot_cost = model.intVar("C", 0, 999999, true);
        model.sum(aux_cost, "=", tot_cost).post();

        model.setObjective(Model.MINIMIZE, tot_cost);

        Solver solver = model.getSolver();
        // Find a solution that minimizes 'tot_cost'
        solver.limitTime(f("%ds", timelimit));


        Solution best = solver.findOptimalSolution(tot_cost, false);
        solver.printStatistics();

        for(int i  = 0; i < N; i++) {
            for(int j  =  i +1; j < N; j++) {
                if (best.getIntVal(vs[i][j]) > 0) {
                    pf("PlayerStateDTO %s: %d (%d) \n", players.get(i), j, costMatrix[i][j]);
                }
            }
        }

        p("completed");
    } */

    private int[][] getChoicesCost() {
        int[][] cost = new int[N][];
        int c;
        for (int i = 0; i < N; i++) {
            cost[i] = new int[N];
            Map<Integer, Integer> map = record.get(players.get(i));
            for (int j = 0; j < N; j++) {
                cost[i][j] = getPlayerCost(players.get(i), j+1);
            }
        }

        return cost;
    }

    private boolean readChoices() {
        players = new ArrayList<>();
        record = new HashMap<String, Map<Integer, Integer>>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
                players.add(values[0]);
                HashMap<Integer, Integer> choices = new HashMap<Integer, Integer>();
                for (int i = 1; i < values.length; i++)
                    choices.put(Integer.valueOf(values[i]), i-1);

                record.put(values[0], choices);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
