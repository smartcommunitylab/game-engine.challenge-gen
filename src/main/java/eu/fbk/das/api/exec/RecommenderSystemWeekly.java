package eu.fbk.das.api.exec;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;

import java.util.*;

public class RecommenderSystemWeekly extends RecommenderSystemExec {

    public static void main(String[] args) {
        new RecommenderSystemWeekly().go(null, null);
    }

    public List<ChallengeExpandedDTO> go(Map<String, String> conf, String players) {
        prepare();
        if (conf == null) conf = this.conf;

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
        modelTypes.add(ChallengesConfig.BUS_KM);
        modelTypes.add(ChallengesConfig.TRAIN_KM);

        if (players == null)
            players = "all";

        return api.createSingleChallengeWeekly(conf, modelTypes, creationRules, config, players, reward);
    }

    public boolean upload(Map<String, String> conf, ChallengeExpandedDTO cha) {
        if (conf == null) conf = this.conf;

        return api.assignSingleChallenge(conf, cha);
    }

    public boolean exec(Map<String, String> conf, String players) {
        if (conf == null) conf = this.conf;

        List<ChallengeExpandedDTO> chas = go(conf, players);
        boolean res = true;
        for(ChallengeExpandedDTO cha: chas) {
            res = res & upload(conf, cha);
        }
        return res;
    }
}
