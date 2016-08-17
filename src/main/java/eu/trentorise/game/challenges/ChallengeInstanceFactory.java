package eu.trentorise.game.challenges;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.BadgeCollectionConcept;
import eu.trentorise.game.challenges.rest.Content;

public class ChallengeInstanceFactory {

	public ChallengeDataDTO createChallenge(String chType,
			Map<String, Object> params, Content user)
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

		default:
			throw new UndefinedChallengeException("Unknown challenge type!"
					+ chType.toString());
		}
	}

	private ChallengeDataDTO buildCompleteBadgeCollection(
			Map<String, Object> params, Content user) {
		ChallengeDataDTO cdd = new ChallengeDataDTO();
		cdd.setModelName(Constants.COMPLETEBADGECOLLECTION);
		cdd.setInstanceName(params.get(Constants.NAME) + "_"
				+ UUID.randomUUID());
		cdd.setStart((Date) params.get(Constants.START_DATE));
		cdd.setEnd((Date) params.get(Constants.END_DATE));
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(Constants.TARGET, params.get(Constants.TARGET));
		data.put(Constants.BONUS_POINT_TYPE,
				params.get(Constants.BONUS_POINT_TYPE));
		data.put(Constants.BONUS_SCORE,
				Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
		data.put(Constants.BADGECOLLECTIONNAME, params.get(Constants.GOAL_TYPE));
		cdd.setData(data);
		return cdd;
	}

	private ChallengeDataDTO buildNextBadge(Map<String, Object> params,
			Content user) {
		ChallengeDataDTO cdd = new ChallengeDataDTO();
		cdd.setModelName(Constants.NEXTBADGE);
		cdd.setInstanceName(params.get(Constants.NAME) + "_"
				+ UUID.randomUUID());
		cdd.setStart((Date) params.get(Constants.START_DATE));
		cdd.setEnd((Date) params.get(Constants.END_DATE));
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(Constants.TARGET, params.get(Constants.TARGET));
		data.put(Constants.BONUS_POINT_TYPE,
				params.get(Constants.BONUS_POINT_TYPE));
		data.put(Constants.BONUS_SCORE,
				Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
		data.put(Constants.BADGECOLLECTIONNAME, params.get(Constants.GOAL_TYPE));
		data.put(
				Constants.INITIAL_BADGE_NUMBER,
				getCurrentBadgeCollectionSize(user,
						(String) params.get(Constants.GOAL_TYPE)));

		cdd.setData(data);
		return cdd;
	}

	private ChallengeDataDTO buildPercentageIncrement(Map<String, Object> params) {
		ChallengeDataDTO cdd = new ChallengeDataDTO();
		cdd.setModelName(Constants.PERCENTAGEINCREMENT);
		cdd.setInstanceName(params.get(Constants.NAME) + "_"
				+ UUID.randomUUID());
		cdd.setStart((Date) params.get(Constants.START_DATE));
		cdd.setEnd((Date) params.get(Constants.END_DATE));
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(Constants.COUNTER_NAME, params.get(Constants.COUNTER_NAME));
		data.put(Constants.PERIOD_NAME, params.get(Constants.GOAL_TYPE));
		data.put(Constants.TARGET, params.get(Constants.TARGET));
		data.put(Constants.BASELINE, params.get(Constants.BASELINE));
		data.put(Constants.BONUS_POINT_TYPE,
				params.get(Constants.BONUS_POINT_TYPE));
		data.put(Constants.BONUS_SCORE,
				Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
		cdd.setData(data);
		return cdd;
	}

	private ChallengeDataDTO buildAbsoluteIncrement(Map<String, Object> params) {
		ChallengeDataDTO cdd = new ChallengeDataDTO();
		cdd.setModelName(Constants.ABSOLUTEINCREMENT);
		cdd.setInstanceName(params.get(Constants.NAME) + "_"
				+ UUID.randomUUID());
		cdd.setStart((Date) params.get(Constants.START_DATE));
		cdd.setEnd((Date) params.get(Constants.END_DATE));
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(Constants.COUNTER_NAME, params.get(Constants.COUNTER_NAME));
		data.put(Constants.PERIOD_NAME, params.get(Constants.GOAL_TYPE));
		data.put(Constants.TARGET, params.get(Constants.TARGET));
		data.put(Constants.BONUS_POINT_TYPE,
				params.get(Constants.BONUS_POINT_TYPE));
		data.put(Constants.BONUS_SCORE,
				Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
		cdd.setData(data);
		return cdd;
	}

	private Integer getCurrentBadgeCollectionSize(Content user, String name) {
		for (BadgeCollectionConcept bc : user.getState()
				.getBadgeCollectionConcept()) {
			if (bc.getName().equals(name)) {
				return bc.getBadgeEarned().size();
			}
		}
		return 0;
	}

}
