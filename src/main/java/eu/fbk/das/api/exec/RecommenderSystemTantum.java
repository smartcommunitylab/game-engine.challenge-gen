package eu.fbk.das.api.exec;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;

import java.util.*;

public class RecommenderSystemTantum extends RecommenderSystemExec {

    public List<ChallengeExpandedDTO> go(Map<String, String> conf, String modelType, Map<String, Object> config, String players) {
        prepare();
        if (conf == null) conf = this.conf;

        if (players == null)
            players = "all";

        return api.createSingleChallengeUnaTantum(conf, modelType, config, players, reward);
    }

    public boolean exec(Map<String, String> conf, String modelType, Map<String, Object> config, String players) {
        if (conf == null) conf = this.conf;

        List<ChallengeExpandedDTO> chas = go(conf, modelType, config, players);
        boolean res = true;
        for(ChallengeExpandedDTO cha: chas) {
            res = res & upload(conf, cha);
        }
        return res;
    }
}
