package eu.trentorise.game.challenges;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;

public class ChallengeInstanceFactoryTest {


    @Test(expected = UndefinedChallengeException.class)
    public void not_existing_challenge() throws UndefinedChallengeException {
        ChallengeInstanceFactory factory = new ChallengeInstanceFactory();
        String challengeType = "dummie";
        Map<String, Object> params = new HashMap<>();
        Content user = new Content();
        factory.createChallenge(challengeType, params, user);
    }


    @Test
    public void repetitive_behaviour_main_case() throws UndefinedChallengeException {
        ChallengeInstanceFactory factory = new ChallengeInstanceFactory();
        String challengeType = "repetitiveBehaviour";
        Map<String, Object> params = new HashMap<>();
        params.put("goalType", "goalType");
        params.put("periodName", "myPeriod");
        params.put("periodTarget", 2.0);
        params.put("bonusPointType", "bonus_type");
        params.put("bonusScore", "25");
        params.put("target", "target");
        Content user = new Content();
        ChallengeDataDTO challengeDTO = factory.createChallenge(challengeType, params, user);

        ChallengeDataDTO result = new ChallengeDataDTO();
        result.setData(new HashMap<String, Object>());
        result.getData().put("counterName", "goalType");
        result.getData().put("periodName", "myPeriod");
        result.getData().put("periodTarget", 2d);
        result.getData().put("bonusPointType", "bonus_type");
        result.getData().put("bonusScore", 25d);
        result.getData().put("target", "target");
        Assert.assertEquals(result.getData(), challengeDTO.getData());

    }
}
