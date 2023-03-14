package eu.fbk.das.api.exec;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;

import java.util.*;

public class RecommenderSystemWeekly extends RecommenderSystemExec {

    public List<ChallengeExpandedDTO> go(Map<String, String> conf, String players, Set<String> modelTypes,  Map<String, String> creationRules) {
        prepare(conf);
        if (conf == null) conf = this.conf;

        if (creationRules == null) {
            creationRules = new HashMap<>();
            creationRules.put("0", "empty");
            creationRules.put("1", "fixedOne");
            creationRules.put("2", "choiceTwoV2");
            creationRules.put("3", "choiceThreeV2");
            creationRules.put("other", "choiceThreeV2");
        }

        if (modelTypes == null) {
            modelTypes = new HashSet<>();
            modelTypes.add(ChallengesConfig.BIKE_KM);
            modelTypes.add(ChallengesConfig.WALK_KM);
            modelTypes.add(ChallengesConfig.GREEN_LEAVES);
            modelTypes.add(ChallengesConfig.BUS_KM);
            modelTypes.add(ChallengesConfig.TRAIN_KM);
        }

        if (players == null)
            players = "all";

        return api.createStandardSingleChallenges(conf, modelTypes, creationRules, config, players, reward);
    }

    public boolean exec(Map<String, String> conf, String players, Set<String> modelTypes,  Map<String, String> creationRules) {
        if (conf == null) conf = this.conf;

        List<ChallengeExpandedDTO> chas = go(conf, players, modelTypes, creationRules);
        boolean res = true;
        for(ChallengeExpandedDTO cha: chas) {
            res = res & upload(conf, cha);
        }
        return res;
    }
}
