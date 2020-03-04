package eu.fbk.das.innowee;

import eu.fbk.das.rs.utils.ArrayUtils;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static eu.fbk.das.rs.utils.Utils.*;

public class InnoWeeCostCalculator {

    private int rows = 13;

    private int cols = 3;

    private int[] cost_limit = new int[] {180, 30, 60};

    private int timelimit = 20;
    private Random random = new Random(System.currentTimeMillis());

    @Test
    public void test() {
        InnoWeeCostCalculator iwcc = new InnoWeeCostCalculator();

        for (int i = 0; i < 10; i++)
        iwcc.execute();
    }


    public void execute() {

        int rand = rand(30)-15 ;
        for (int c = 0; c < cols; c++) {
            // random perturbation between 1.1 and 0.9
            cost_limit[c] = (int) (cost_limit[c] + rand);
        }

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
        for (int c = 0; c < cols; c++) {
            IntVar[] a_r = new IntVar[rows];
            for(int r  = 0; r < rows; r++) {
                a_r[r] = vs[r][c];
            }
             model.sum(a_r, ">=", cost_limit[c]).post();
        }

        int row_cost_limit = 0;
        for (int c = 0; c < cols; c++) {
            row_cost_limit += cost_limit[c];
        }
        row_cost_limit /= rows;
            for(int r  = 0; r < rows; r++) {
                model.sum(vs[r], ">=", row_cost_limit).post();
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

         Solution best = solver.findOptimalSolution(tot_cost, false);
         solver.printStatistics();

        int[] first_shuf = new int[]{0, 1, 2};
        int[] second_shuf = new int[]{0, 1, 2};

        int[][] sol = new int[rows][];

        copySol(best, sol, 0, vs[0]);
        ArrayUtils.shuffleArray(first_shuf, random);
        for(int r1  = 0; r1 < 3; r1++) {
            int r1_new = first_shuf[r1];

            int row_1 = 1 + r1 + r1 * 3;
            int row_1_new = 1 + r1_new + r1_new * 3;
            pf("%d -> %d\n", row_1, row_1_new);
            copySol(best, sol, row_1_new, vs[row_1]);

            ArrayUtils.shuffleArray(second_shuf, random);
            for (int r2 = 0; r2 < 3; r2++) {
                int r2_new = second_shuf[r2];

                int row_2 = row_1 + (r2 + 1);
                int row_2_new = row_1_new + (r2_new + 1);
                pf("%d -> %d\n", row_2, row_2_new);
                copySol(best, sol, row_2_new, vs[row_2]);
            }
        }


        for(int r  = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                pf("%8d", sol[r][c]);
            }
            p("");
        }

        /*
        while(solver.solve()) {
            int tot = 0;

            for(int r  = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int v = vs[r][c].getValue();
                    // pf("%8d", v);
                    tot += v;
                }
                p("");
            }

            addSolution(tot, vs);
        }*/

        p("completed");
    }

    private void copySol(Solution best, int[][] sol, int i2, IntVar[] v) {
        sol[i2] = new int[cols];
        for (int c = 0; c < cols; c++) {
            sol[i2][c] = best.getIntVal(v[c]);
        }
    }

}
