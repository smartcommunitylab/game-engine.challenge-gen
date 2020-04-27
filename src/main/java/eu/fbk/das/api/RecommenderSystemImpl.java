package eu.fbk.das.api;

import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RecommenderSystemImpl implements RecommenderSystemAPI {

    private RecommendationSystem rs;
    private Set<String> players;

    private void prepare(Map<String, String> conf, String playerSet, String duration) {

        this.rs = new RecommendationSystem(conf.get("HOST"), conf.get("USER"), conf.get("PASS"), conf.get("GAME_ID"));

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
    public boolean createSingleChallengeUnaTantum(Map<String, String> conf, String modelType, Map<String, String> challengeValues,
        String playerSet, String duration, Map<String, String> rewards, Boolean visibility) {

        prepare(conf, playerSet, duration);

        for (String pId: players) {
            // prepare
            ChallengeDataDTO cha = rs.rscg.prepareChallange(modelType);
            // set challenge model
            cha.setModelName(modelType);
            // set data
            for (String k: challengeValues.keySet()) {
                String v = challengeValues.get(k);
                if ("start".equals(k)) cha.setStart(new DateTime(v));
                else if ("end".equals(k)) cha.setEnd(new DateTime(v));
                else cha.setData(k, v);
            }

            // TODO COME INTEGRARE DURATION / START / END

            // set visibility
            cha.setHide(!visibility);

            reward(cha, rewards);

            rs.facade.assignChallengeToPlayer(cha, rs.gameId, pId);
        }

        return false;
    }

    private void reward(ChallengeDataDTO cha, Map<String, String> rewards) {
        String scoreType = rewards.get("ScoreType");
        String calcType = rewards.get("CalcType");
        String calcValue = rewards.get("CalcValue");
        String maxValue = rewards.get("MaxValue");

        // TODO COME INSERIRE INFO SCORE TYPE

        // if fixed reward, simply set it
        if ("Fixed".equals(calcType)) {
            cha.getData().put("bonusScore", calcValue);
            return;
        }
        // otherwise use Evaluator
        rs.rscv.valuate(cha);
        Double r = (Double)cha.getData().get("bonusScore");
        // check if we have to increment reward
        if ("Bonus".equals(calcType)) {
            r += Double.parseDouble(calcValue);
        } else if ("Booster".equals(calcType)) {
            r *= Double.parseDouble(calcValue);
        }
        // check it there is a maximum reward
        if (maxValue != null) {
            r = Math.min(r, Double.parseDouble(maxValue));
        }

        cha.getData().put("bonusScore", r);
    }

    @Override
    public boolean createSingleChallengeWeekly(Map<String, String> conf, String modelType, Map<String, String> creationRules, String playerSet,
        String duration, Map<String, String> rewards, Boolean visibility) {

        prepare(conf, playerSet, duration);

        return false;
    }

    @Override
    public boolean createCoupleChallengeWeekly(Map<String, String> conf, String modelType, Map<String, String> challengeValues,
        String playerSet, String duration, Map<String, String> rewards, Boolean visibility) {

        prepare(conf, playerSet, duration);

        return false;
    }
}
