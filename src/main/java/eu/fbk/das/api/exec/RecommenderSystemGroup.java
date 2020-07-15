package eu.fbk.das.api.exec;

import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import it.smartcommunitylab.model.GroupChallengeDTO;

import java.util.*;

public class RecommenderSystemGroup extends RecommenderSystemExec {

    public static void main(String[] args) {
        new RecommenderSystemGroup().exec(null, null, null);
    }

    private List<GroupExpandedDTO> go(Map<String, String> conf, String players, String challengeType) {
        if (conf == null) conf = this.conf;

        prepare();

        Set<String> modeList = new HashSet<String>(Arrays.asList(ChallengesConfig.WALK_KM,ChallengesConfig.BIKE_KM,ChallengesConfig.GREEN_LEAVES));

        return api.createCoupleChallengeWeekly(conf, modeList, challengeType, config, players, reward);
    }

    public boolean upload(Map<String, String> conf, GroupExpandedDTO cha) {
        if (conf == null) conf = this.conf;

        return api.assignGroupChallenge(conf, cha);
    }

    public boolean exec(Map<String, String> conf, String players, String challengeType) {
        if (conf == null) conf = this.conf;

        List<GroupExpandedDTO> chas = go(conf, players, challengeType);
        boolean res = true;
        for(GroupExpandedDTO cha: chas) {
            res = res & upload(conf, cha);
        }
        return res;
    }
}
