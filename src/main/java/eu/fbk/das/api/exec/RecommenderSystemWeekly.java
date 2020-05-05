package eu.fbk.das.api.exec;

import java.util.HashMap;
import java.util.Map;

public class RecommenderSystemWeekly extends RecommenderSystemExec {

    public static void main(String[] args) {
        new RecommenderSystemWeekly().go();
    }

    private void go() {
        prepare();

        Map<String, String> creationRules = new HashMap<>();
        creationRules.put("0", "empty");
        creationRules.put("1", "fixedOne");
        creationRules.put("2", "choiceTwo");
        creationRules.put("3", "choiceThree");
        creationRules.put("other", "choiceThree");

        api.createSingleChallengeWeekly(conf, creationRules, challengeValues, "all", reward);
    }

}
