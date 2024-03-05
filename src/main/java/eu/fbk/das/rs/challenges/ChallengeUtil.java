package eu.fbk.das.rs.challenges;

import static eu.fbk.das.rs.challenges.calculator.ChallengesConfig.getWeeklyContentMode;
import static eu.fbk.das.utils.Utils.equal;
import static eu.fbk.das.utils.Utils.f;
import static eu.fbk.das.utils.Utils.pf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;

import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.PlayerLevel;
import it.smartcommunitylab.model.ext.PointConcept;

public class ChallengeUtil {

    protected RecommendationSystem rs;
    
    protected String prefix;

    protected int playerLimit = 50;
    protected int minLvl = -1;

    protected String[] counters;

    public ChallengeUtil() {
        this(new RecommendationSystem());
    }

    public ChallengeUtil(RecommendationSystem rs) {
        this.rs = rs;
    }

    public void prepare() {
        prepare(1);
    }

    public void prepare(int challengeWeek) {
        counters = ChallengesConfig.getPerfomanceCounters();

        prefix = f(ChallengesConfig.getChallengeNamePrefix(), challengeWeek);
    }


    protected List<String> getPlayers() {

        Set<String> players = rs.facade.getGamePlayers(rs.gameId);
        List<String> playersToConsider = new ArrayList<>(playerLimit);
        for (String pId: players) {
            PlayerStateDTO p = rs.facade.getPlayerState(rs.gameId, pId);
            int lvl = getLevel(p);
            if (minLvl < 0 || lvl >=  minLvl) {
                playersToConsider.add(pId);
                if (playerLimit != 0 && playersToConsider.size() >= playerLimit)
                    break;
            }
        }
        return playersToConsider;
    }

    public static int getLevel(PlayerStateDTO state) {

        // check the level of the player
        List<PlayerLevel> lvls = state.getLevels();

        for (PlayerLevel lvl: lvls) {
            if (!equal(lvl.getPointConcept(), "green leaves"))
                continue;

            return lvl.getLevelIndex();

            /*

            String s = slug(lvl.getLevelValue());
            for (int i = 0; i < cfg.levelNames.length; i++)
                if (equal(s, slug(cfg.levelNames[i])))
                    return i;


            pf("Could not decode value %s of PlayerStateDTO level %s \n", lvl.getLevelValue(), lvl);

            return -1;

            */

        }

        pf("Could not find level based on green leaves! %s - Assuming level 0 \n", lvls);

        return 0;
    }

    public static double getPeriodScore(PointConcept pc, String w, DateTime dt) {
        try {
            return pc.getPeriodScore(w, dt.getMillis());
        } catch (Exception e) { // if ask for a date previous of period startDate return 0
            return 0;
        }

    }
    
    public static Double getWMABaseline(PlayerStateDTO state, String counter, DateTime lastMonday) {

        DateTime date = lastMonday;
        int v = 5;

        double den = 0;
        double num = 0;
        for (int ix = 0; ix < v; ix++) {
            // weight * value
            Double c = getWeeklyContentMode(state, counter, date);
            den += (v -ix) * c;
            num += (v -ix);

            date = date.minusDays(7);
        }

        double baseline;
        
        if (num == 0) {
        	baseline = 0;	
        } else {
        	baseline = den / num;	
        }         
        
        
        return baseline;
    }


    public static int getQuantile(Double c, Map<Integer, Double> quant) {
        for (int i = 0; i < 10; i++) {
            if (c < quant.get(i))
                return i;
        }

        return 9;
    }
    
}
