package eu.fbk.das.old;


import eu.fbk.das.model.ChallengeExpandedDTO;
import it.smartcommunitylab.model.PlayerStateDTO;
import it.smartcommunitylab.model.ext.BadgeCollectionConcept;
import it.smartcommunitylab.model.ext.GameConcept;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChallengeInstanceFactory {

    public ChallengeExpandedDTO createChallenge(String chType, Map<String, Object> params, PlayerStateDTO user) throws Exception {
        switch (chType) {
            case Constants.ABSOLUTEINCREMENT:
                return buildAbsoluteIncrement(params);
            case Constants.PERCENTAGEINCREMENT:
                return buildPercentageIncrement(params);
            case Constants.NEXTBADGE:
                return buildNextBadge(params, user);
            case Constants.COMPLETEBADGECOLLECTION:
                return buildCompleteBadgeCollection(params, user);
            case Constants.SURVEY:
                return buildSurvey(params, user);
            case Constants.POICHECKIN:
                return buildPoiCheckIn(params, user);
            case Constants.CHECKIN:
                return buildCheckIn(params, user);
            case Constants.LEADERBOARDPOSITION:
                return buildLeaderBoardPosition(params, user);
            case Constants.REPETITIVE_BEHAVIOUR:
                return buildRepetitiveBehaviour(params, user);
            default:
                throw new Exception(
                        "Challenge Generator doesn't recognize this challenge type! "
                                + chType.toString());
        }
    }

    private ChallengeExpandedDTO buildSurvey(Map<String, Object> params, PlayerStateDTO user) {
        ChallengeExpandedDTO cdd = new ChallengeExpandedDTO();
        cdd.setModelName(Constants.SURVEY);
        cdd.setInstanceName(params.get(Constants.NAME) + "_" + UUID.randomUUID());
        cdd.setStart((Date) params.get(Constants.START_DATE));
        cdd.setEnd((Date) params.get(Constants.END_DATE));
        
        cdd.setData(Constants.BONUS_POINT_TYPE, params.get(Constants.BONUS_POINT_TYPE));
        cdd.setData(Constants.BONUS_SCORE,
                Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
        
        return cdd;
    }

    private ChallengeExpandedDTO buildCompleteBadgeCollection(Map<String, Object> params,
                                                          PlayerStateDTO user) {
        ChallengeExpandedDTO cdd = new ChallengeExpandedDTO();
        cdd.setModelName(Constants.COMPLETEBADGECOLLECTION);
        cdd.setInstanceName(params.get(Constants.NAME) + "_" + UUID.randomUUID());
        cdd.setStart((Date) params.get(Constants.START_DATE));
        cdd.setEnd((Date) params.get(Constants.END_DATE));
        
        cdd.setData(Constants.TARGET, params.get(Constants.TARGET));
        cdd.setData(Constants.BONUS_POINT_TYPE, params.get(Constants.BONUS_POINT_TYPE));
        cdd.setData(Constants.BONUS_SCORE,
                Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
        cdd.setData(Constants.BADGECOLLECTIONNAME, params.get(Constants.GOAL_TYPE));
        
        return cdd;
    }

    private ChallengeExpandedDTO buildNextBadge(Map<String, Object> params, PlayerStateDTO user) {
        ChallengeExpandedDTO cdd = new ChallengeExpandedDTO();
        cdd.setModelName(Constants.NEXTBADGE);
        cdd.setInstanceName(params.get(Constants.NAME) + "_" + UUID.randomUUID());
        cdd.setStart((Date) params.get(Constants.START_DATE));
        cdd.setEnd((Date) params.get(Constants.END_DATE));
        
        cdd.setData(Constants.TARGET, params.get(Constants.TARGET));
        cdd.setData(Constants.BONUS_POINT_TYPE, params.get(Constants.BONUS_POINT_TYPE));
        cdd.setData(Constants.BONUS_SCORE,
                Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
        cdd.setData(Constants.BADGECOLLECTIONNAME, params.get(Constants.GOAL_TYPE));
        cdd.setData(Constants.INITIAL_BADGE_NUMBER,
                getCurrentBadgeCollectionSize(user, (String) params.get(Constants.GOAL_TYPE)));

        
        return cdd;
    }

    private ChallengeExpandedDTO buildPercentageIncrement(Map<String, Object> params) {
        ChallengeExpandedDTO cdd = new ChallengeExpandedDTO();
        cdd.setModelName(Constants.PERCENTAGEINCREMENT);
        cdd.setInstanceName(params.get(Constants.NAME) + "_" + UUID.randomUUID());
        cdd.setStart((Date) params.get(Constants.START_DATE));
        cdd.setEnd((Date) params.get(Constants.END_DATE));
        
        cdd.setData(Constants.COUNTER_NAME, params.get(Constants.GOAL_TYPE));
        cdd.setData(Constants.PERIOD_NAME, params.get(Constants.PERIOD_NAME));
        cdd.setData(Constants.TARGET, params.get(Constants.TARGET));
        cdd.setData(Constants.BASELINE, params.get(Constants.BASELINE));
        cdd.setData(Constants.BONUS_POINT_TYPE, params.get(Constants.BONUS_POINT_TYPE));
        cdd.setData(Constants.BONUS_SCORE,
                Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
        
        return cdd;
    }

    private ChallengeExpandedDTO buildAbsoluteIncrement(Map<String, Object> params) {
        ChallengeExpandedDTO cdd = new ChallengeExpandedDTO();
        cdd.setModelName(Constants.ABSOLUTEINCREMENT);
        cdd.setInstanceName(params.get(Constants.NAME) + "_" + UUID.randomUUID());
        cdd.setStart((Date) params.get(Constants.START_DATE));
        cdd.setEnd((Date) params.get(Constants.END_DATE));
        
        cdd.setData(Constants.COUNTER_NAME, params.get(Constants.GOAL_TYPE));
        cdd.setData(Constants.PERIOD_NAME, params.get(Constants.PERIOD_NAME));
        cdd.setData(Constants.TARGET, params.get(Constants.TARGET));
        cdd.setData(Constants.BONUS_POINT_TYPE, params.get(Constants.BONUS_POINT_TYPE));
        cdd.setData(Constants.BONUS_SCORE,
                Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
        
        return cdd;
    }

    private ChallengeExpandedDTO buildPoiCheckIn(Map<String, Object> params, PlayerStateDTO user) {

        ChallengeExpandedDTO cdd = new ChallengeExpandedDTO();
        cdd.setModelName(Constants.POICHECKIN);
        cdd.setInstanceName(params.get(Constants.NAME) + "_" + UUID.randomUUID());
        cdd.setStart((Date) params.get(Constants.START_DATE));
        cdd.setEnd((Date) params.get(Constants.END_DATE));
        
        cdd.setData(Constants.POI_NAME, "Trento Fiera");
        cdd.setData(Constants.EVENT_NAME, "Fai la cosa giusta");
        cdd.setData(Constants.POI_STATE, Boolean.FALSE);
        cdd.setData(Constants.EVENT_STATE, Boolean.FALSE);
        cdd.setData(Constants.BONUS_POINT_TYPE, params.get(Constants.BONUS_POINT_TYPE));
        cdd.setData(Constants.BONUS_SCORE,
                Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));

        
        return cdd;
    }

    private ChallengeExpandedDTO buildCheckIn(Map<String, Object> params, PlayerStateDTO user) {

        ChallengeExpandedDTO cdd = new ChallengeExpandedDTO();
        cdd.setModelName(Constants.CHECKIN);
        cdd.setInstanceName(params.get(Constants.NAME) + "_" + UUID.randomUUID());
        cdd.setStart((Date) params.get(Constants.START_DATE));
        cdd.setEnd((Date) params.get(Constants.END_DATE));
        
        cdd.setData("checkinType", params.get(Constants.GOAL_TYPE)); // reused an excel existing field
        // for this
        // type of challenge
        cdd.setData(Constants.BONUS_POINT_TYPE, params.get(Constants.BONUS_POINT_TYPE));
        cdd.setData(Constants.BONUS_SCORE,
                Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));

        
        return cdd;
    }


    private Integer getCurrentBadgeCollectionSize(PlayerStateDTO user, String name) {
        Set<GameConcept> bcc = user.getState().get("BadgeCollectionConcept");
        for (GameConcept gc : bcc) {
            BadgeCollectionConcept bc = (BadgeCollectionConcept) gc;
            if (bc.getName().equals(name)) {
                return bc.getBadgeEarned().size();
            }
        }
        return 0;
    }


    private ChallengeExpandedDTO buildRepetitiveBehaviour(Map<String, Object> params, PlayerStateDTO user) {
        ChallengeExpandedDTO cdd = new ChallengeExpandedDTO();
        cdd.setModelName(Constants.REPETITIVE_BEHAVIOUR);
        cdd.setInstanceName(params.get(Constants.NAME) + "_" + UUID.randomUUID());
        cdd.setStart((Date) params.get(Constants.START_DATE));
        cdd.setEnd((Date) params.get(Constants.END_DATE));
        
        cdd.setData(Constants.COUNTER_NAME, params.get(Constants.GOAL_TYPE));
        cdd.setData(Constants.PERIOD_NAME, params.get(Constants.PERIOD_NAME));
        cdd.setData(Constants.PERIOD_TARGET,
                Double.valueOf(params.get(Constants.PERIOD_TARGET).toString()));
        cdd.setData(Constants.TARGET, params.get(Constants.TARGET));
        cdd.setData(Constants.BONUS_POINT_TYPE, params.get(Constants.BONUS_POINT_TYPE));
        cdd.setData(Constants.BONUS_SCORE,
                Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));

        
        return cdd;
    }

    private ChallengeExpandedDTO buildLeaderBoardPosition(Map<String, Object> params, PlayerStateDTO user) {
        ChallengeExpandedDTO cdd = new ChallengeExpandedDTO();
        cdd.setModelName(Constants.LEADERBOARDPOSITION);
        cdd.setInstanceName(params.get(Constants.NAME) + "_" + UUID.randomUUID());
        cdd.setStart((Date) params.get(Constants.START_DATE));
        cdd.setEnd((Date) params.get(Constants.END_DATE));
        
        String[] values =
                ((String) params.get(Constants.TARGET)).split(Constants.MIN_MAX_SEPARATOR);
        cdd.setData("posMin", Double.valueOf(values[0]));
        cdd.setData("posMax", Double.valueOf(values[1]));
        cdd.setData("bonusPointType", params.get(Constants.BONUS_POINT_TYPE));
        cdd.setData("bonusScore", Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
        cdd.setData("weekClassificationName", params.get(Constants.GOAL_TYPE));

        
        return cdd;
    }

}
