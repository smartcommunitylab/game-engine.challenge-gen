package eu.trentorise.game.challenges;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joda.time.LocalDate;

import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;

public class ChallengeInstanceFactory {

	public ChallengeDataDTO createChallenge(String chType,
			Map<String, Object> params) throws UndefinedChallengeException {
		switch (chType) {
		case "ZEROIMPACT":
			return buildZeroImpact(params);
		default:
			throw new UndefinedChallengeException("Unknown challenge type!"
					+ chType.toString());
		}
	}

	private ChallengeDataDTO buildZeroImpact(Map<String, Object> params) {
		LocalDate now = new LocalDate();

		ChallengeDataDTO cdd = new ChallengeDataDTO();
		cdd.setModelName("zeroImpact");
		cdd.setInstanceName("zeroImpact_" + UUID.randomUUID());
		cdd.setStart(now.dayOfMonth().addToCopy(-10).toDate());
		cdd.setEnd(now.dayOfMonth().addToCopy(5).toDate());
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("counter", 0);
		data.put("target", params.get("target"));
		data.put("bonusPointType", params.get("bonusPointType"));
		data.put("bonusScore",
				Double.valueOf(params.get("bonusScore").toString()));
		cdd.setData(data);
		return cdd;
	}

}
