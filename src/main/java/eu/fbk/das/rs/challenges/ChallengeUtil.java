package eu.fbk.das.rs.challenges;

import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import eu.fbk.das.rs.challenges.generation.RecommendationSystemConfig;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.Player;
import eu.trentorise.game.challenges.rest.PlayerLevel;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static eu.fbk.das.rs.utils.Utils.*;

public class ChallengeUtil {

    protected GamificationEngineRestFacade facade;
    protected RecommendationSystemConfig cfg;
    protected String gameId;
    protected DateTime execDate;

    protected DateTime endDate;
    protected DateTime startDate;
    protected DateTime lastMonday;
    protected String prefix;

    protected int playerLimit = 50;
    protected int minLvl = -1;

    public ChallengeUtil(RecommendationSystemConfig cfg) {
        this.cfg = cfg;
        gameId = cfg.get("GAME_ID");
    }

    protected void setFacade( GamificationEngineRestFacade facade) {
        this.facade = facade;
    }

    protected void prepare(DateTime date) {

        // Set next monday as start, and next sunday as end
        int week_day = execDate.getDayOfWeek();
        int d = (7 - week_day) + 1;

        this.execDate = date
                .withHourOfDay(0)
                .withMinuteOfHour(0)
                .withSecondOfMinute(0);

        lastMonday = execDate.minusDays(week_day-1).minusDays(7);

        startDate = execDate.plusDays(d);
        startDate = startDate.minusDays(2);
        endDate = startDate.plusDays(7);
    }

    protected List<String> getPlayers() {


        Set<String> players = facade.getGamePlayers(gameId);
        List<String> playersToConsider = new ArrayList<String>(playerLimit);
        int ix = 0;
        for (String pId: players) {
            Player p = facade.getPlayerState(gameId, pId);
            int lvl = getLevel(p);
            if (minLvl < 0 || lvl >=  minLvl) {
                playersToConsider.add(pId);
                if (playerLimit != 0 && playersToConsider.size() >= playerLimit)
                    break;
            }
        }
        return playersToConsider;
    }

    public static int getLevel(Player state) {

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


            pf("Could not decode value %s of player level %s \n", lvl.getLevelValue(), lvl);

            return -1;

            */

        }

        pf("Could not find level based on green leaves! %s - Assuming level 0 \n", lvls);

        return 0;
    }




}
