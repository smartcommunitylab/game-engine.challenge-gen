package eu.fbk.das.rs;

import eu.fbk.das.GamificationEngineRestFacade;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static eu.fbk.das.rs.utils.Utils.p;
import static eu.fbk.das.rs.utils.Utils.pf;

public class CustomDataTest extends ChallengesBaseTest {

    @Test
    public void test() {
        conf.put("HOST", "https://dev.smartcommunitylab.it/gamification-v3/");
        // cfg.put("HOST", "https://tn.smartcommunitylab.it/gamification2/");
        facade = new GamificationEngineRestFacade(conf.get("HOST"),
                conf.get("USERNAME"), conf.get("PASSWORD"));

        String gameId = conf.get("GAMEID");

        Set<String> pIds =  facade.getGamePlayers(gameId);

        String pId = pIds.iterator().next();

        p(pId);

        int i = 0;

        Map<String, Object> cs = facade.getCustomDataPlayer(gameId, pId);
        for (String k : cs.keySet()) {
            pf("%s: %s \n", k, cs.get(k));
            if (k.equals("i")) {
                i = (int)  cs.get(k);
                i += 1;
            }
        }

        cs.put("i", i);
        facade.setCustomDataPlayer(gameId, pId, cs);


    }

}
