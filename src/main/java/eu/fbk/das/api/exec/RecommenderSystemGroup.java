package eu.fbk.das.api.exec;

import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RecommenderSystemGroup extends RecommenderSystemExec {

    public static void main(String[] args) {
        new RecommenderSystemGroup().go();
    }

    private void go() {
        prepare();

        Set<String> modeList = new HashSet<String>(Arrays.asList(ChallengesConfig.WALK_KM,ChallengesConfig.BIKE_KM,ChallengesConfig.GREEN_LEAVES));

        String challengeType = "groupCooperative";
        // String challengeType = "groupCompetitiveTime";
        // String challengeType = "groupCompetitivePerformance"

        api.createCoupleChallengeWeekly(conf, modeList, challengeType, config, "all", reward);
    }
}
