package eu.fbk.das.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import eu.fbk.das.model.ChallengeExpandedDTO;

public class SingleSpecialChallengeApiTest {

    @Test
    public void survey_challenge() {
        RecommenderSystemAPI recommenderApi = new RecommenderSystemImpl();
        final String  model = "survey";
        final Map<String, Object> config = new HashMap<>();
        config.put("link", "survey_link");
        config.put("surveyType", "final");
        config.put("bonusScore", 40.0);
        config.put("bonusPointType", "green leaves");
        final Map<String, String> rewards = new HashMap<>();
        rewards.put("scoreType", "green leaves");
        rewards.put("calcType", "fixed");
        rewards.put("calcValue", "300.0");
        List<ChallengeExpandedDTO> generated = recommenderApi.createSingleChallengeUnaTantum(
                gamificationEngineConf(), model, config,
                playerSet(), rewards);
        assertThat(generated).hasSize(1);
        final ChallengeExpandedDTO challenge = generated.get(0);
        System.out.println(String.format("model: %s, name: %s, fields: %s",
                challenge.getModelName(), challenge.getInstanceName(), challenge.getData()));
        Map<String, Object> expectedValues = new HashMap<>();
        expectedValues.put("surveyType", "final");
        expectedValues.put("link", "survey_link");
        expectedValues.put("bonusScore", 300.0); // taken from rewards
        expectedValues.put("bonusPointType", "green leaves"); // taken from rewards
        assertThat(challenge.getData()).containsExactlyInAnyOrderEntriesOf(expectedValues);
    }

    @Test
    public void repetitiveBehaviour_challenge() {
        RecommenderSystemAPI recommenderApi = new RecommenderSystemImpl();
        final String model = "repetitiveBehaviour";
        final Map<String, Object> config = new HashMap<>();
        config.put("bonusScore", 50.0);
        config.put("bonusPointType", "green leaves");
        config.put("periodName", "daily");
        config.put("target", 5.0);
        config.put("periodTarget", 2.0);
        config.put("counterName", "Walk_Km");
        final Map<String, String> rewards = new HashMap<>();
        rewards.put("scoreType", "green leaves");
        rewards.put("calcType", "fixed");
        rewards.put("calcValue", "300.0");
        List<ChallengeExpandedDTO> generated = recommenderApi.createSingleChallengeUnaTantum(
                gamificationEngineConf(), model, config, playerSet(), rewards);
        final ChallengeExpandedDTO challenge = generated.get(0);
        System.out.println(String.format("model: %s, name: %s, fields: %s",
                challenge.getModelName(), challenge.getInstanceName(), challenge.getData()));
        final Map<String, Object> expectedValues = new HashMap<>();
        expectedValues.put("bonusScore", 300.0); // taken from rewards
        expectedValues.put("bonusPointType", "green leaves"); // taken from rewards
        expectedValues.put("periodName", "daily");
        expectedValues.put("target", 5.0);
        expectedValues.put("periodTarget", 2.0);
        expectedValues.put("counterName", "Walk_Km");
        assertThat(challenge.getData()).containsExactlyInAnyOrderEntriesOf(expectedValues);
    }

    @Test
    public void absoluteIncrement_challenge() {
        RecommenderSystemAPI recommenderApi = new RecommenderSystemImpl();
        final String model = "absoluteIncrement";
        final Map<String, Object> config = new HashMap<>();
        config.put("difficulty", 5.0);
        config.put("wi", "wi_value");
        config.put("bonusScore", 20.0);
        config.put("bonusPointType", "green leaves");
        config.put("periodName", "weekly");
        config.put("target", 5.0);
        config.put("counterName", "Walk_Km");
        final Map<String, String> rewards = new HashMap<>();
        rewards.put("scoreType", "green leaves");
        rewards.put("calcType", "fixed");
        rewards.put("calcValue", "110.0");
        List<ChallengeExpandedDTO> generated = recommenderApi.createSingleChallengeUnaTantum(
                gamificationEngineConf(), model, config, playerSet(), rewards);
        final ChallengeExpandedDTO challenge = generated.get(0);
        System.out.println(String.format("model: %s, name: %s, fields: %s",
                challenge.getModelName(), challenge.getInstanceName(), challenge.getData()));
        final Map<String, Object> expectedValues = new HashMap<>();
        expectedValues.put("difficulty", 5.0);
        expectedValues.put("wi", "wi_value");
        expectedValues.put("bonusScore", 110.0); // taken from rewards
        expectedValues.put("bonusPointType", "green leaves"); // taken from rewards
        expectedValues.put("periodName", "weekly");
        expectedValues.put("target", 5.0);
        expectedValues.put("counterName", "Walk_Km");
        assertThat(challenge.getData()).containsExactlyInAnyOrderEntriesOf(expectedValues);
    }

    @Test
    public void percentageIncrement_challenge() {
        RecommenderSystemAPI recommenderApi = new RecommenderSystemImpl();
        final String model = "percentageIncrement";
        final Map<String, Object> config = new HashMap<>();
        config.put("difficulty", 5.0);
        config.put("wi", "wi_value");
        config.put("bonusScore", 20.0);
        config.put("bonusPointType", "green leaves");
        config.put("percentage", 20.0);
        config.put("periodName", "weekly");
        config.put("baseline", 100.0);
        config.put("target", 5.0);
        config.put("counterName", "Walk_Km");
        final Map<String, String> rewards = new HashMap<>();
        rewards.put("scoreType", "green leaves");
        rewards.put("calcType", "fixed");
        rewards.put("calcValue", "120.0");
        List<ChallengeExpandedDTO> generated = recommenderApi.createSingleChallengeUnaTantum(
                gamificationEngineConf(), model, config, playerSet(), rewards);
        final ChallengeExpandedDTO challenge = generated.get(0);
        System.out.println(String.format("model: %s, name: %s, fields: %s",
                challenge.getModelName(), challenge.getInstanceName(), challenge.getData()));
        final Map<String, Object> expectedValues = new HashMap<>();
        expectedValues.put("difficulty", 5.0);
        expectedValues.put("wi", "wi_value");
        expectedValues.put("bonusScore", 120.0); // taken from rewards
        expectedValues.put("bonusPointType", "green leaves"); // taken from rewards
        expectedValues.put("percentage", 20.0);
        expectedValues.put("periodName", "weekly");
        expectedValues.put("baseline", 100.0);
        expectedValues.put("target", 5.0);
        expectedValues.put("counterName", "Walk_Km");
        assertThat(challenge.getData()).containsExactlyInAnyOrderEntriesOf(expectedValues);
    }

    // fake gamification engine settings. Setting a valid playerSet
    // it is possible to skip gamification engine connection (to retrieve all playerIds of a game)
    private Map<String, String> gamificationEngineConf() {
        Map<String, String> settings = new HashMap<>();
        settings.put("GAMEID", "");
        settings.put("HOST", "");
        settings.put("USER", "");
        settings.put("PASS", "");
        return settings;
    }

    // settings a playerSet permits to avoid gameEngine invocation
    // to get the game player list
    private String playerSet() {
        return "player";
    }
}
