package eu.fbk.das.rs.challenges.evaluation;

import eu.fbk.das.rs.GroupChallengesAssigner;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import org.junit.Test;

import static org.junit.Assert.*;

public class ImprovementCheckerTest extends ChallengesBaseTest {

    @Test
    public void test() {
        cfg.put("HOST", "https://dev.smartcommunitylab.it/gamification/");
        facade = new GamificationEngineRestFacade(cfg.get("HOST"),
                cfg.get("USERNAME"), cfg.get("PASSWORD"));
        GroupChallengesAssigner gca = new GroupChallengesAssigner(cfg, facade);
        gca.execute();
    }

    @Test
    public void execute() {
        ImprovementChecker gca = new ImprovementChecker(cfg, facade);
        gca.execute();
    }
}