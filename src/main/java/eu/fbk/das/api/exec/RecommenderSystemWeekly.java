package eu.fbk.das.api.exec;

import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

        Set<String> modelTypes = new HashSet<>();
        modelTypes.add(ChallengesConfig.BIKE_KM);
        modelTypes.add(ChallengesConfig.WALK_KM);
        modelTypes.add(ChallengesConfig.GREEN_LEAVES);

        api.createSingleChallengeWeekly(conf, modelTypes, creationRules, challengeValues, "all", reward);
    }

}
