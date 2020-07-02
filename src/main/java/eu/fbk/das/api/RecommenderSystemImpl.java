package eu.fbk.das.api;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.GroupChallengesAssigner;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import it.smartcommunitylab.model.GroupChallengeDTO;
import it.smartcommunitylab.model.RewardDTO;
import org.joda.time.DateTime;

import java.util.*;

import static eu.fbk.das.GamificationEngineRestFacade.jodaToOffset;

public class RecommenderSystemImpl implements RecommenderSystemAPI {

    private RecommendationSystem rs;
    private Set<String> players;

    private void prepare(Map<String, String> conf, String playerSet) {

        this.rs = new RecommendationSystem(conf.get("host"), conf.get("user"), conf.get("pass"), conf.get("gameId"));

        if ("all".equals(playerSet))
            players = rs.facade.getGamePlayers(rs.gameId);
        else {
            players  = new HashSet<>();
            String[] aux = playerSet.split(",");
            for (String s: aux)
                players.add(s.trim());
        }

    }

    @Override
    public List<ChallengeExpandedDTO> createSingleChallengeUnaTantum(Map<String, String> conf, String modelType, Map<String, Object> config,
                                                                     String playerSet, Map<String, String> rewards) {

        prepare(conf, playerSet);

        List<ChallengeExpandedDTO> chas = new ArrayList<>();

        for (String pId: players) {
            // prepare
            ChallengeExpandedDTO cha = rs.rscg.prepareChallange(modelType);
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

        prepare(conf, playerSet);

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
    public List<GroupChallengeDTO> createCoupleChallengeWeekly(Map<String, String> conf, Set<String> modelTypes, String assignmentType, Map<String, Object> config, String playerSet, Map<String, String> rewards) {

        prepare(conf, playerSet);

        List<GroupChallengeDTO> chas = new ArrayList<>();

        GroupChallengesAssigner gca = new GroupChallengesAssigner(rs);
        List<GroupChallengeDTO> groupChallenges = gca.execute(players, modelTypes, assignmentType, config);

        for (GroupChallengeDTO gcd: groupChallenges) {
            // set data
            dataGroup(gcd, config);
            // set reward;
            rewardGroup(gcd, rewards);

            chas.add(gcd);
        }

        return chas;
    }

    @Override
    public boolean assignSingleChallenge(ChallengeExpandedDTO cha) {
        String gameId = (String) cha.getInfo("gameId");
        String pId = (String) cha.getInfo("pId");
        return rs.facade.assignChallengeToPlayer(cha, gameId, pId);
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

        Double v = Double.parseDouble(calcValue);

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