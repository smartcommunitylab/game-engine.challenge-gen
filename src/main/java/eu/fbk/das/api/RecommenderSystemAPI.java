package eu.fbk.das.api;

import java.util.Map;

public interface RecommenderSystemAPI {

    public boolean createSingleChallengeUnaTantum(Map<String, String> conf, String modelType, Map<String, String> challengeValues, String playerSet, String duration, Map<String, String> rewards, Boolean visibility);

    public boolean createSingleChallengeWeekly(Map<String, String> conf, String modelType, Map<String, String> creationRules, String playerSet, String duration, Map<String, String> rewards, Boolean visibility);

    public boolean createCoupleChallengeWeekly(Map<String, String> conf, String modelType, Map<String, String> challengeValues, String playerSet, String duration, Map<String, String> rewards, Boolean visibility);
}
