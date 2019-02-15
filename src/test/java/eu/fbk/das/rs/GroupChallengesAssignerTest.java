package eu.fbk.das.rs;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.*;

public class GroupChallengesAssignerTest extends ChallengesBaseTest {

    @Test
    public void execute() {
        GroupChallengesAssigner gca = new GroupChallengesAssigner(cfg, facade);
        gca.execute();
    }

    @Test
    public void choco() {
        testChoco();
    }

    @Test
    public void chocoModel() {
        GroupChallengesAssigner gca = new GroupChallengesAssigner(cfg, facade);

        HashMap<Integer, ArrayList<String>> test = new HashMap<>();
        test.put(1, new ArrayList<String>(Arrays.asList("One", "Two", "Three")));
        test.put(2, new ArrayList<String>(Arrays.asList("Four", "Fixe", "Siv")));
        gca.chocoModel(test);
    }


    public void testChoco() {
        int N = 100;
        // 1. Modelling part
        Model model = new Model("all-interval series of size "+ N);
        // 1.a declare the variables
        IntVar[] S = model.intVarArray("s", N, 0, N - 1, false);
        IntVar[] V = model.intVarArray("V", N - 1, 1, N - 1, false);
        // 1.b post the constraints
        for (int i = 0; i < N - 1; i++) {
            model.distance(S[i + 1], S[i], "=", V[i]).post();
        }
        model.allDifferent(S).post();
        model.allDifferent(V).post();
        S[1].gt(S[0]).post();
        V[1].gt(V[N - 2]).post();

        // 2. Solving part
        Solver solver = model.getSolver();
        // 2.a define a search strategy
        solver.setSearch(Search.minDomLBSearch(S));
        if(solver.solve()){
            System.out.printf("All interval series of size %d%n", N);
            for (int i = 0; i < N - 1; i++) {
                System.out.printf("%d <%d> ",
                        S[i].getValue(),
                        V[i].getValue());
            }
            System.out.printf("%d", S[N - 1].getValue());
        }
    }

}