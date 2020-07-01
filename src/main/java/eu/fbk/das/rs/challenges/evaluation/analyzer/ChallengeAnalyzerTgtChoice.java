package eu.fbk.das.rs.challenges.evaluation.analyzer;

import eu.fbk.das.rs.challenges.evaluation.ChallengeAnalyzer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.fbk.das.rs.utils.Utils.pf;

public class ChallengeAnalyzerTgtChoice extends ChallengeAnalyzer {

    private int c_player;

    private int c_chosen;

    private Map<String, Integer> c_chosen_ours;
    private Map<String, Integer> c_chosen_exp;
    private Map<String, Integer> c_proposed_exp;

    public static void main(String[] args) {
        ChallengeAnalyzerTgtChoice cdg = new ChallengeAnalyzerTgtChoice();

        cdg.analyzeSelected();
    }

    protected void prepare() {
        files = new String[] {"week-22/challenges-2019-03-26-complete.csv"};
    }

    protected void experiment(String file) {


         c_player = 0;

        c_chosen = 0;

        c_chosen_ours = new HashMap<>();

        c_chosen_exp = new HashMap<>();

        c_proposed_exp = new HashMap<>();

        for (String pId : challenges.keySet()) {

            List<ChallengeRecord> chas = challenges.get(pId);

            c_player++;

            String exp = chas.get(0).exp;

            if (!c_proposed_exp.containsKey(exp))
                c_proposed_exp.put(exp, 0);

            c_proposed_exp.put(exp,c_proposed_exp.get(exp) +1);

            // check if the user chose any challenges
            double targetChosen = getPlayerChosen(chas);
            if (targetChosen <= 0)
                continue;

            c_chosen++;

            if (!c_chosen_exp.containsKey(exp))
                c_chosen_exp.put(exp, 0);

            c_chosen_exp.put(exp,c_chosen_exp.get(exp) +1);

            if (!"tgt".equals(chas.get(0).exp))
                continue;

            double targetProposed = getTargetProposed(chas);
            if (targetProposed == -1)
                return;

            String s = "err";
            if ((targetChosen - targetProposed) < 0.01)
                s = "ours";
            else if (targetChosen > targetProposed)
                s = "more";
            else if (targetChosen < targetProposed)
                s = "less";

            if (!c_chosen_ours.containsKey(s))
                c_chosen_ours.put(s, 0);

            c_chosen_ours.put(s, c_chosen_ours.get(s) + 1);
        }
    }

    protected void output() {

        pf("\nChoice rate of target: %.2f (%d / %d) \n", c_chosen * 1.0 / c_player, c_chosen, c_player);

        for (String s: c_chosen_exp.keySet())
            pf("\nExp Choice of %s: %.2f (%d / %d) \n", s,  c_chosen_exp.get(s) * 1.0 / c_proposed_exp.get(s), c_chosen_exp.get(s), c_proposed_exp.get(s));

        for (String s: c_chosen_ours.keySet())
        pf("\nChoice of %s: %.2f (%d / %d) \n", s,  c_chosen_ours.get(s) * 1.0 / c_chosen_exp.get("tgt"), c_chosen_ours.get(s), c_chosen_exp.get("tgt"));
    }

}
