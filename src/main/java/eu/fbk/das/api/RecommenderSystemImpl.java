package eu.fbk.das.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.rs.GroupChallengesAssigner;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import it.smartcommunitylab.model.ext.GroupChallengeDTO;
import it.smartcommunitylab.model.ext.GroupChallengeDTO.PointConceptDTO;
import it.smartcommunitylab.model.ext.GroupChallengeDTO.RewardDTO;

import static eu.fbk.das.utils.Utils.*;

public class RecommenderSystemImpl implements RecommenderSystemAPI {

    private static RecommendationSystem rs;
    private Set<String> players;

    private void prepare(String playerSet) {
    	System.out.println("playerSet -> " + playerSet);
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
        String host = conf.get("HOST");
        String user = conf.get("API_USER");
        String pass = conf.get("API_PASS");
        String gameId = conf.get("GAMEID");

        boolean create = rs == null;
        if (!create) {
            create = !(rs.host.equals(host) && rs.user.equals(user) && rs.pass.equals(pass) && rs.gameId.equals(gameId));
        }

        if (create)
            this.rs = new RecommendationSystem(conf);
    }

    @Override
    public List<ChallengeExpandedDTO> createSpecialSingleChallenges(Map<String, String> conf, String modelType, Map<String, Object> config,
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
    public List<ChallengeExpandedDTO> createStandardSingleChallenges(Map<String, String> conf, Set<String> modelTypes, Map<String, String> creationRules, Map<String, Object> config, String playerSet, Map<String, String> rewards) {

       
		System.out.println("\n##################### configMap #####################");
		config.entrySet().forEach(entry -> {
			System.out.println(entry.getKey() + " " + entry.getValue());
		});
		System.out.println("#########################################################\n");

    	if (playerSet == null || "".equals(playerSet))
            playerSet = "all";

        checkUpdateRs(conf);
        prepare(playerSet);

        List<ChallengeExpandedDTO> chas = new ArrayList<>();

        StringBuilder errors = new StringBuilder();
        
        for (String pId: players) {

            try {

                List<ChallengeExpandedDTO> challenges = rs.recommend(pId, modelTypes, creationRules, config);

                for (ChallengeExpandedDTO cha : challenges) {
                    // set data
                    dataCha(cha, config);
                    // set reward;
                    reward(cha, rewards);

                    cha.setInfo("gameId", rs.gameId);
                    cha.setInfo("pId", pId);
                    chas.add(cha);

                    pf("playerId: %s, instanceName: %s, model: %s, s: %s, e: %s, f: %s\n", pId,
                            cha.getInstanceName(), cha.getModelName(), cha.getStart(), cha.getEnd(), cha.printData());
                }
            } catch (Exception e) {
                pfs(errors, "WARNING: error in generation for playerId: %s\n", pId);
                logFirstStackTrace(e, 5, errors);
                pfs(errors, "players: %s\n", players);
                logMap("conf", conf, errors);
                logSet("modelTypes", modelTypes, errors);
                logMap("creationRules", creationRules, errors);
                logMap("config", config, errors);
                logMap("rewards", rewards, errors);
            }

        }

        // Email both the list of challenges, and the error list

        return chas;
    }

    @Override
    public List<GroupExpandedDTO> createStandardGroupChallenges(Map<String, String> conf, Set<String> modelTypes, String assignmentType, Map<String, Object> config, String playerSet, Map<String, String> rewards) {

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
        cha.setGameId(gameId);
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

        cha.setDates(config.get("start"),config.get("duration"));
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

    private void dataGroup(GroupExpandedDTO gcd, Map<String, Object> config) {
        for (String k : config.keySet()) {
            Object v = config.get(k);
            if ("start".equals(k) || "duration".equals(k)) continue;
            // else String v = challengeValues.get(k);
            // else cha.setData(k, v);
        }

        gcd.setDates(config.get("start"), config.get("duration"));
    }

    private void rewardGroup(GroupChallengeDTO gcd, Map<String, String> rewards) {
        String scoreType = rewards.get("scoreType");
        String calcType = rewards.get("calcType");
        String calcValue = rewards.get("calcValue");
        String maxValue = rewards.get("maxValue");

        RewardDTO rew = gcd.getReward();
        Map<String, Double> bs = rew.getBonusScore();
        final PointConceptDTO calculationPointConcept = new PointConceptDTO();
        calculationPointConcept.setName(scoreType);
        // FIXME valid only for play&go
        calculationPointConcept.setPeriod("weekly");
        rew.setCalculationPointConcept(calculationPointConcept);
        final PointConceptDTO targetPointConcept = new PointConceptDTO();
        targetPointConcept.setName(scoreType);
        rew.setTargetPointConcept(targetPointConcept);

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
