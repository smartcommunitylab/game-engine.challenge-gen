package eu.fbk.das.rs.challenges;

import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystem;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.PlayerLevel;
import it.smartcommunitylab.model.ext.PointConcept;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static eu.fbk.das.rs.challenges.generation.RecommendationSystem.getChallengeWeek;
import static eu.fbk.das.rs.utils.Utils.*;

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

    public void prepare(DateTime d) {
        prepare(getChallengeWeek(d));
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
        return pc.getPeriodScore(w, dt.getMillis());
    }
}
