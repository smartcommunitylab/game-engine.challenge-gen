package eu.trentorise.game.challenges;

import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.BadgeCollectionConcept;
import eu.trentorise.game.challenges.rest.Player;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class ChallengeInstanceFactory {

    public ChallengeDataDTO createChallenge(String chType, Map<String, Object> params, Player user)
            throws UndefinedChallengeException {
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
                throw new UndefinedChallengeException(
                        "Challenge Generator doesn't recognize this challenge type! "
                                + chType.toString());
        }
    }

    private ChallengeDataDTO buildSurvey(Map<String, Object> params, Player user) {
        ChallengeDataDTO cdd = new ChallengeDataDTO();
        cdd.setModelName(Constants.SURVEY);
        cdd.setInstanceName(params.get(Constants.NAME) + "_" + UUID.randomUUID());
        cdd.setStart((Date) params.get(Constants.START_DATE));
        cdd.setEnd((Date) params.get(Constants.END_DATE));
        
        cdd.setData(Constants.BONUS_POINT_TYPE, params.get(Constants.BONUS_POINT_TYPE));
        cdd.setData(Constants.BONUS_SCORE,
                Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
        
        return cdd;
    }

    private ChallengeDataDTO buildCompleteBadgeCollection(Map<String, Object> params,
                                                          Player user) {
        ChallengeDataDTO cdd = new ChallengeDataDTO();
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

    private ChallengeDataDTO buildNextBadge(Map<String, Object> params, Player user) {
        ChallengeDataDTO cdd = new ChallengeDataDTO();
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

    private ChallengeDataDTO buildPercentageIncrement(Map<String, Object> params) {
        ChallengeDataDTO cdd = new ChallengeDataDTO();
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

    private ChallengeDataDTO buildAbsoluteIncrement(Map<String, Object> params) {
        ChallengeDataDTO cdd = new ChallengeDataDTO();
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

    private ChallengeDataDTO buildPoiCheckIn(Map<String, Object> params, Player user) {

        ChallengeDataDTO cdd = new ChallengeDataDTO();
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

    private ChallengeDataDTO buildCheckIn(Map<String, Object> params, Player user) {

        ChallengeDataDTO cdd = new ChallengeDataDTO();
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


    private Integer getCurrentBadgeCollectionSize(Player user, String name) {
        for (BadgeCollectionConcept bc : user.getState().getBadgeCollectionConcept()) {
            if (bc.getName().equals(name)) {
                return bc.getBadgeEarned().size();
            }
        }
        return 0;
    }


    private ChallengeDataDTO buildRepetitiveBehaviour(Map<String, Object> params, Player user) {
        ChallengeDataDTO cdd = new ChallengeDataDTO();
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

    private ChallengeDataDTO buildLeaderBoardPosition(Map<String, Object> params, Player user) {
        ChallengeDataDTO cdd = new ChallengeDataDTO();
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
