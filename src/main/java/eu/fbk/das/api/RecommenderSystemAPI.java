package eu.fbk.das.api;

import java.util.Map;
import java.util.Set;

public interface RecommenderSystemAPI {

    /**
     *
     * @param conf keys: host / user / pass / gameId
     * @param modelType challenge model to be created
     * @param challengeValues keys: start / end (required), hide (optional)
     * @param playerSet values: all / list of ids - comma separated
     * @param rewards keys: scoreType / calcType (fixed, bonus, booster) / calcValue / maxValue
     * @return success of operation
     */
    public boolean createSingleChallengeUnaTantum(Map<String, String> conf, String modelType, Map<String, Object> challengeValues, String playerSet, Map<String, String> rewards);

    /**
     *
     * @param conf keys: host / user / pass / gameId
     * @param creationRules assigns to each level (key) a creation rule (value): empty / fixedOne / choiceTwo / choiceThree
     * @param challengeValues keys: start / end / challengeWeek / execDate (required), hide (optional)
     * @param playerSet values: all / list of ids - comma separated
     * @param rewards keys: scoreType / calcType (fixed, bonus, booster) / calcValue / maxValue
     * @return success of operation
     */
    public boolean createSingleChallengeWeekly(Map<String, String> conf, Map<String, String> creationRules, Map<String, Object> challengeValues,  String playerSet, Map<String, String> rewards);

    /**
     *
     * @param conf keys: host / user / pass / gameId
     * @param modelTypes types of challenge model taken into consideration
     * @param assignmentType type of assignment: groupCooperative / groupCompetitiveTime / groupCompetitivePerformance
     * @param challengeValues keys: start / end (required)
     * @param playerSet values: all / list of ids - comma separated
     * @param rewards keys: scoreType / calcType (fixed, bonus, booster) / calcValue / maxValue
     * @return success of operation
     */
    public boolean createCoupleChallengeWeekly(Map<String, String> conf, Set<String> modelTypes, String assignmentType, Map<String, Object> challengeValues, String playerSet, Map<String, String> rewards);
}
