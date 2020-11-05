package eu.fbk.das.api.exec;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;

public class RecommenderSystemGroup extends RecommenderSystemExec {

    public List<GroupExpandedDTO> go(Map<String, String> conf, String players, String challengeType, Set<String> modelTypes) {
        if (conf == null) conf = this.conf;
        prepare();

        if (modelTypes == null) {
            modelTypes = new HashSet<String>(Arrays.asList(ChallengesConfig.WALK_KM, ChallengesConfig.BIKE_KM, ChallengesConfig.GREEN_LEAVES));
        }

        return api.createStandardGroupChallenges(conf, modelTypes, challengeType, config, players, reward);
    }

    public boolean upload(Map<String, String> conf, GroupExpandedDTO cha) {
        if (conf == null) conf = this.conf;

        return api.assignGroupChallenge(conf, cha);
    }

    public boolean exec(Map<String, String> conf, String players, String challengeType, Set<String> modelTypes) {
        if (conf == null) conf = this.conf;

        List<GroupExpandedDTO> chas = go(conf, players, challengeType, modelTypes);
        boolean res = true;
        for(GroupExpandedDTO cha: chas) {
            res = res & upload(conf, cha);
        }
        return res;
    }
}
