package eu.fbk.das.innowee;

import eu.fbk.das.rs.TargetPrizeChallengesCalculator;
import eu.fbk.das.rs.challenges.ChallengeUtil;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemStatistics;
import eu.fbk.das.rs.utils.ArrayUtils;
import eu.fbk.das.rs.utils.Pair;
import eu.trentorise.game.challenges.model.GroupChallengeDTO;
import eu.trentorise.game.challenges.rest.ChallengeConcept;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.Player;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.*;

import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.getWeeklyContentMode;
import static eu.fbk.das.rs.utils.Utils.*;

public class InnoWeeCostCalculator {

    private int rows = 13;

    private int cols = 3;

    private int[] cost_limit = new int[] {180, 30, 60};

    private int timelimit = 20;

    @Test
    public void test() {
        InnoWeeCostCalculator iwcc = new InnoWeeCostCalculator();
        iwcc.execute();
    }


    public void execute() {

        Model model = new Model("InnoWeeCostCalculator");

        // variables
        IntVar[][] vs = model.intVarMatrix("vs", rows, cols, 1, 99999);

        // for each row: every value different
        for(int r  = 0; r < rows; r++) {
            for (int c_i = 0; c_i < cols; c_i++)
                for (int c_j = c_i + 1; c_j < cols; c_j++)
                    vs[r][c_i].ne(vs[r][c_j]).post();
        }

        // for every row: max value between prices
        for (int c = 0; c < cols; c++) {
            int row_limit = (int) Math.ceil(cost_limit[c] * 1.0 / rows);
            for (int r = 0; r < rows; r++) {
                vs[r][c].gt(row_limit).post();
            }
        }

        // for every 2-nd level row, for every cols: higher than corresponding value in 1-lvl
        for (int r = 0; r < 3; r++) {
            int row = 1 + r + r * 3;
            for (int c = 0; c < cols; c++) {
                vs[row][c].gt(vs[0][c]).post();
                // p(row);
            }
        }

        List<Integer> aux_1 = new ArrayList<Integer>();
        List<Integer> aux_2 = new ArrayList<Integer>();

        // for every 3-nd level row, for every cols: higher than corresponding value in 1-lvl
        for (int r_i = 0; r_i < 3; r_i++) {
            int row_1 = 1 + r_i + r_i * 3;
            for (int r_j = 0; r_j < 3; r_j++) {
                int row_2 = row_1 + (r_j + 1);

               //  pf("%d - %d \n", row_1, row_2);

                 for (int c = 0; c < cols; c++)
                   vs[row_2][c].gt(vs[row_1][c]).post();
                
                aux_2.add(row_2);
            }

            aux_1.add(row_1);
        }

        // for each 3-nd level between them
        for (int i  = 0; i < aux_1.size(); i++)
            for (int j  = i + 1; j < aux_1.size(); j++)
                // for every column
                for (int c = 0; c < cols; c++) {
                    vs[aux_1.get(i)][c].ne(vs[aux_1.get(j)][c]).post();
                }


        // for each 3-nd level between them
        for (int i  = 0; i < aux_2.size(); i++)
            for (int j  = i + 1; j < aux_2.size(); j++)
                // for every column
                for (int c = 0; c < cols; c++) {
                    vs[aux_2.get(i)][c].ne(vs[aux_2.get(j)][c]).post();
                }


        // for every column, sum over all the rows
        /*
        for (int c = 0; c < cols; c++) {
            IntVar[] a_r = new IntVar[rows];
            for(int r  = 0; r < rows; r++) {
                a_r[r] = vs[r][c];
            }
             model.sum(a_r, ">=", cost_limit[c]).post();
        }*/

        int row_cost_limit = 0;
        for (int c = 0; c < cols; c++) {
            row_cost_limit += cost_limit[c];
        }
        row_cost_limit /= rows;
            for(int r  = 0; r < rows; r++) {
           //  model.sum(vs[r], ">=", row_cost_limit).post();
        }

        IntVar[] aux_cost = model.intVarArray("ac", rows, 0, 99999,true);
        for(int r  = 0; r < rows; r++) {
            model.sum(vs[r], "=", aux_cost[r]).post();
        }
        IntVar tot_cost = model.intVar("C", 0, 99999, true);
        model.sum(aux_cost, "=", tot_cost).post();
        

        Solver solver = model.getSolver();
        // Find a solution that minimizes 'tot_cost'
        solver.limitTime(f("%ds", timelimit));

        /* Solution best = solver.findOptimalSolution(tot_cost, false);
        solver.printStatistics();
        for(int r  = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                pf("%8d", best.getIntVal(vs[r][c]));
            }
            p("");
        } */

        while(solver.solve()) {
            p("New solution found! \n");

            int tot = 0;

            for(int r  = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int v = vs[r][c].getValue();
                    pf("%8d", v);
                    tot += v;
                }
                p("");
            }

            pf("\n\n %d \n", tot);
        }

        p("completed");
    }

}
