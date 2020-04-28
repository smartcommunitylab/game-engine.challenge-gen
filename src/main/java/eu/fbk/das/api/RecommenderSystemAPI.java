package eu.fbk.das.api;

import java.util.Map;
import java.util.Set;

public interface RecommenderSystemAPI {

    public boolean createSingleChallengeUnaTantum(Map<String, String> conf, String modelType, Map<String, String> challengeValues, String playerSet, Map<String, String> rewards);

    public boolean createSingleChallengeWeekly(Map<String, String> conf, Map<String, String> creationRules, String playerSet, Map<String, String> rewards);

    public boolean createCoupleChallengeWeekly(Map<String, String> conf, Set<String> modelTypes, String assignmentType, Map<String, String> challengeValues, String playerSet, Map<String, String> rewards);
}
