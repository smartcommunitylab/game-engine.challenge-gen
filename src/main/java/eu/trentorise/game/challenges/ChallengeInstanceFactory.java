package eu.trentorise.game.challenges;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;

public class ChallengeInstanceFactory {

	public ChallengeDataDTO createChallenge(String chType,
			Map<String, Object> params) throws UndefinedChallengeException {
		switch (chType) {
		case Constants.ZEROIMPACT:
			return buildZeroImpact(params);
		default:
			throw new UndefinedChallengeException("Unknown challenge type!"
					+ chType.toString());
		}
	}

	private ChallengeDataDTO buildZeroImpact(Map<String, Object> params) {
		ChallengeDataDTO cdd = new ChallengeDataDTO();
		cdd.setModelName(Constants.ZEROIMPACT);
		cdd.setInstanceName(Constants.ZEROIMPACT + "_" + UUID.randomUUID());
		cdd.setStart((Date) params.get(Constants.START_DATE));
		cdd.setEnd((Date) params.get(Constants.END_DATE));
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(Constants.COUNTER, 0);
		data.put(Constants.TARGET, params.get(Constants.TARGET));
		data.put(Constants.BONUS_POINT_TYPE,
				params.get(Constants.BONUS_POINT_TYPE));
		data.put(Constants.BONUS_SCORE,
				Double.valueOf(params.get(Constants.BONUS_SCORE).toString()));
		cdd.setData(data);
		return cdd;
	}

}
