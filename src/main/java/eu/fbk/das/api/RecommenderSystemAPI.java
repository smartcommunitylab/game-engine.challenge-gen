package eu.fbk.das.api;

import eu.fbk.das.model.GroupExpandedDTO;
import eu.fbk.das.rs.challenges.Challenge;
import eu.fbk.das.model.ChallengeExpandedDTO;
import it.smartcommunitylab.model.ext.GroupChallengeDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RecommenderSystemAPI {

    /**
     *
     * @param conf keys: host / user / pass / gameId
     * @param modelType challenge model to be created
     * @param config keys: start / end (required), hide (optional)
     * @param playerSet values: all / list of ids - comma separated
     * @param rewards keys: scoreType / calcType (fixed, bonus, booster) / calcValue / maxValue
     * @return success of operation
     */
    public List<ChallengeExpandedDTO> createSpecialSingleChallenges(Map<String, String> conf, String modelType, Map<String, Object> config, String playerSet, Map<String, String> rewards);

    /**
     *
     * @param conf keys: host / user / pass / gameId
     * @param modelTypes : transportation modes to be evaluated for the challenge
     * @param creationRules assigns to each level (key) a creation rule (value): empty / fixedOne / choiceTwo / choiceThree
     * @param config keys: start / end / challengeWeek / execDate (required), hide (optional)
     * @param playerSet values: all / list of ids - comma separated
     * @param rewards keys: scoreType / calcType (fixed, bonus, booster) / calcValue / maxValue
     * @return success of operation
     */
    public List<ChallengeExpandedDTO> createStandardSingleChallenges(Map<String, String> conf, Set<String> modelTypes, Map<String, String> creationRules, Map<String, Object> config, String playerSet, Map<String, String> rewards);

    /**
     *
     * @param conf keys: host / user / pass / gameId
     * @param modelTypes types of challenge model taken into consideration
     * @param assignmentType type of assignment: groupCooperative / groupCompetitiveTime / groupCompetitivePerformance
     * @param config keys: start / end (required)
     * @param playerSet values: all / list of ids - comma separated
     * @param rewards keys: scoreType / calcType (fixed, bonus, booster) / calcValue / maxValue
     * @return success of operation
     */
    public List<GroupExpandedDTO> createStandardGroupChallenges(Map<String, String> conf, Set<String> modelTypes, String assignmentType, Map<String, Object> config, String playerSet, Map<String, String> rewards);

    /**
     * 
     * @param conf keys: host / user / pass / gameId
     * @param creationRules assigns to each week (key) a list of challenges
     * @param config keys: start / end / challengeWeek / execDate (required), hide (optional)
     * @return
     */
	public List<ChallengeExpandedDTO> createHSCChallenges(Map<String, String> conf, Map<String, List<Challenge>> creationRules, Map<String, Object> config);
    
    public boolean assignSingleChallenge(Map<String, String> conf, ChallengeExpandedDTO cha);

    public boolean assignGroupChallenge(Map<String, String> conf, GroupExpandedDTO cha);
}
