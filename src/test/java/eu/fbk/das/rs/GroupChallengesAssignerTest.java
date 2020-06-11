package eu.fbk.das.rs;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import it.smartcommunitylab.model.GroupChallengeDTO;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class GroupChallengesAssignerTest extends ChallengesBaseTest {

    private GroupChallengesAssigner gca;

    @Before
    public void prepare() {
        // cfg.put("HOST", "https://dev.smartcommunitylab.it/gamification/");
        cfg.put("HOST", "https://tn.smartcommunitylab.it/gamification2/");

         rs = new RecommendationSystem(cfg);
        facade = rs.facade;
         gca = new GroupChallengesAssigner(rs);
    }

    @Test
    public void test() {
        // gca.execute();
        DateTime d = new DateTime();

        GroupChallengeDTO gcd = gca.createPerfomanceChallenge("Walk_Km", "7", "225", d.minusDays(3), d.plusDays(3));
        facade.assignGroupChallenge(gcd, cfg.get("GAME_ID"));
    }

    @Test
    public void execute() {
      // FIX  gca.execute(players, modelType);
    }

    @Test
    public void choco() {
        testChoco();
    }
    
    @Test
    public void testStrange() {
        HashMap<String, Integer> p = new HashMap<String, Integer>();
        
        p.put("16751", 0);
        p.put("2176", 0);

        p.put("24232", 0);
        p.put("27393", 0);

        p.put("24553", 0);
        p.put("27144", 0);

        p.put("25541", 0);
        p.put("27428", 0);

        p.put("25866", 0);
        p.put("27270", 0);

        p.put("26588", 0);
        p.put("27610", 0);

        p.put("27256", 0);
        p.put("27311", 0);

        p.put("27258", 0);
        p.put("3276", 0);

        p.put("27752", 1);
        p.put("27381", 1);

        p.put("27370", 4);
        p.put("27453", 7);

        p.put("25925", 8);
        p.put("24060", 8);

        p.put("27454", 8);
        p.put("24206", 8);

        p.put("25589", 8);
        p.put("27402", 9);

        p.put("27943", 9);
        p.put("24471", 9);

        p.put("27418", 9);
        p.put("24881", 9);

        p.put("27345", 9);
        p.put("27742", 9);

        gca.reduce(p);
        gca.chocoModel(p);
    }

    @Test
    public void testStrange2() {
        HashMap<String, Integer> p = new HashMap<String, Integer>();


        p.put("27256", 0);
        p.put("27311", 0);

        p.put("27752", 1);
        p.put("27381", 1);

        p.put("27370", 4);
        p.put("27453", 7);

        p.put("25925", 8);
        p.put("24060", 8);

        p.put("25589", 8);
        p.put("27402", 9);

        p.put("27943", 9);
        p.put("24471", 9);

        gca.chocoModel(p);
    }
    

    @Test
    public void chocoModel() {

        HashMap<Integer, ArrayList<String>> test = new HashMap<>();
        test.put(1, new ArrayList<String>(Arrays.asList("One", "Two", "Three")));
        test.put(2, new ArrayList<String>(Arrays.asList("Four", "Fixe", "Siv")));
        // gca.chocoModel(test);
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