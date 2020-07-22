package eu.fbk.das.api;

import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.GroupChallengesAssigner;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import it.smartcommunitylab.model.GroupChallengeDTO;
import it.smartcommunitylab.model.RewardDTO;
import org.joda.time.DateTime;

import java.util.*;

import static eu.fbk.das.GamificationEngineRestFacade.jodaToOffset;

public class RecommenderSystemImpl implements RecommenderSystemAPI {

    private static RecommendationSystem rs;
    private Set<String> players;

    private void prepare(String playerSet) {

        if ("all".equals(playerSet))
            players = rs.facade.getGamePlayers(rs.gameId);
        else {
            players  = new HashSet<>();
            String[] aux = playerSet.split(",");
            for (String s: aux)
                players.add(s.trim());
        }

    }


    private void checkUpdateRs(Map<String, String> conf) {
        String host = conf.get("host");
        String user = conf.get("user");
        String pass = conf.get("pass");
        String gameId = conf.get("gameId");

        boolean create = rs == null;
        if (!create) {
            create = !(rs.host.equals(host) && rs.user.equals(user) && rs.pass.equals(pass) && rs.gameId.equals(gameId));
        }

        if (create)
            this.rs = new RecommendationSystem(host, user, pass, gameId);
    }

    @Override
    public List<ChallengeExpandedDTO> createSingleChallengeUnaTantum(Map<String, String> conf, String modelType, Map<String, Object> config,
                                                                     String playerSet, Map<String, String> rewards) {

        checkUpdateRs(conf);
        prepare(playerSet);

        List<ChallengeExpandedDTO> chas = new ArrayList<>();

        for (String pId: players) {
            // prepare
            ChallengeExpandedDTO cha = rs.rscg.prepareChallenge("T", modelType);
            // set challenge model
            cha.setModelName(modelType);
            // set data
            dataCha(cha, config);
            // set reward;
            reward(cha, rewards);

            cha.setInfo("gameId", rs.gameId);
            cha.setInfo("pId", pId);
            chas.add(cha);
        }

        return chas;
    }

    @Override
    public List<ChallengeExpandedDTO> createSingleChallengeWeekly(Map<String, String> conf, Set<String> modelTypes, Map<String, String> creationRules, Map<String, Object> config, String playerSet, Map<String, String> rewards) {

        if (playerSet == null || "".equals(playerSet))
            playerSet = "all";

        checkUpdateRs(conf);
        prepare(playerSet);

        List<ChallengeExpandedDTO> chas = new ArrayList<>();

        for (String pId: players) {
            List<ChallengeExpandedDTO> challenges = rs.recommend(pId, modelTypes, creationRules, config);

            for (ChallengeExpandedDTO cha: challenges) {
                // set data
                dataCha(cha, config);
                // set reward;
                reward(cha, rewards);

                cha.setInfo("gameId", rs.gameId);
                cha.setInfo("pId", pId);
                chas.add(cha);
            }
        }

        return chas;
    }

    @Override
    public List<GroupExpandedDTO> createCoupleChallengeWeekly(Map<String, String> conf, Set<String> modelTypes, String assignmentType, Map<String, Object> config, String playerSet, Map<String, String> rewards) {

        if (playerSet == null || "".equals(playerSet))
            playerSet = "all";

        if (assignmentType == null || "".equals(assignmentType))
            assignmentType = "groupCooperative";

        checkUpdateRs(conf);
        prepare(playerSet);

        List<GroupExpandedDTO> chas = new ArrayList<>();

        GroupChallengesAssigner gca = new GroupChallengesAssigner(rs);
        List<GroupExpandedDTO> groupChallenges = gca.execute(players, modelTypes, assignmentType, config);

        for (GroupExpandedDTO gcd: groupChallenges) {
            // set data
            dataGroup(gcd, config);
            // set reward;
            rewardGroup(gcd, rewards);

            chas.add(gcd);
        }

        return chas;
    }

    @Override
    public boolean assignSingleChallenge(Map<String, String> conf, ChallengeExpandedDTO cha) {
        checkUpdateRs(conf);
        String gameId = (String) cha.getInfo("gameId");
        String pId = (String) cha.getInfo("pId");
        for (String s: new String[]{"exec", "challengeWeek"})
            cha.delData(s);
        return rs.facade.assignChallengeToPlayer(cha, gameId, pId);
    }

    @Override
    public boolean assignGroupChallenge(Map<String, String> conf, GroupExpandedDTO cha) {
        checkUpdateRs(conf);
        String gameId = (String) cha.getInfo("gameId");
        return rs.facade.assignGroupChallenge(cha, gameId);
    }

    // assign group challenge rs.facade.assignGroupChallenge(gcd, rs.gameId);

    private void dataCha(ChallengeExpandedDTO cha, Map<String, Object> config) {
        for (String k: config.keySet()) {
            Object v = config.get(k);
            if ("start".equals(k) || "duration".equals(k)) continue;
            if ("hide".equals(k)) {
                cha.setHide((Boolean) v);
                continue;
            }

            cha.setData(k, v);
        }

        cha.setDates(config.get("start"), config.get("duration"));
    }

    private void reward(ChallengeExpandedDTO cha, Map<String, String> rewards) {
        String scoreType = rewards.get("scoreType");
        String calcType = rewards.get("calcType");
        String calcValue = rewards.get("calcValue");
        String maxValue = rewards.get("maxValue");

        cha.setData("bonusPointType", scoreType);

        // if fixed reward, simply set it
        if ("fixed".equals(calcType)) {
            cha.setData("bonusScore", Double.parseDouble(calcValue));
            return;
        }
        // otherwise use Evaluator
        rs.rscv.valuate(cha);
        Double r = (Double)cha.getData("bonusScore");
        // check if we have to increment reward
        if ("bonus".equals(calcType)) {
            r += Double.parseDouble(calcValue);
        } else if ("booster".equals(calcType)) {
            r *= Double.parseDouble(calcValue);
        }
        // check it there is a maximum reward
        if (maxValue != null) {
            r = Math.min(r, Double.parseDouble(maxValue));
        }

        cha.setData("bonusScore", r);
    }

    private void dataGroup(GroupChallengeDTO gcd, Map<String, Object> challengeValues) {
        for (String k : challengeValues.keySet()) {
            Object v = challengeValues.get(k);
            if ("start".equals(k)) gcd.setStart(jodaToOffset(new DateTime(v)));
            else if ("end".equals(k)) gcd.setEnd(jodaToOffset(new DateTime(v)));
            // else String v = challengeValues.get(k);
            // else cha.setData(k, v);
        }
    }

    private void rewardGroup(GroupChallengeDTO gcd, Map<String, String> rewards) {
        String scoreType = rewards.get("scoreType");
        String calcType = rewards.get("calcType");
        String calcValue = rewards.get("calcValue");
        String maxValue = rewards.get("maxValue");

        // TODO COME INSERIRE INFO SCORE TYPE
        RewardDTO rew = gcd.getReward();
        Map<String, Double> bs = rew.getBonusScore();

        Double v = 0.0;
        if (calcValue != null)
            v = Double.parseDouble(calcValue);

        for (String pId: bs.keySet()) {
            Double r = bs.get(pId);

            // if fixed reward, simply set it
            if ("fixed".equals(calcType)) {
                r = v;
            } else {

                // check if we have to increment reward
                if ("bonus".equals(calcType)) {

                    r += v;
                } else if ("booster".equals(calcType)) {
                    r *= v;
                }
                // check it there is a maximum reward
                if (maxValue != null) {
                    r = Math.min(r, Double.parseDouble(maxValue));
                }
            }

            bs.put(pId, r);
        }
    }
}
