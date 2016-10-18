package eu.fbk.das.rs.valuator;

import java.util.List;
import java.util.Map;

import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.rest.Content;

public class RecommendationSystemChallengeValuator {
	// defining a table for points/prize
	private int points[][] = { { 100, 106, 111, 122, 155 }, { 133, 139, 144, 155, 183 }, { 166, 172, 177, 189, 217 },
			{ 197, 205, 211, 222, 250 } };

	public Map<String, List<ChallengeDataDTO>> valuate(Map<String, List<ChallengeDataDTO>> combinations,
			List<Content> result) {
		return null;
	}

}
