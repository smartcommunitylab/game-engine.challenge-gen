package eu.trentorise.game.challenges;

import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.ChallengeType;

public class BikeSharePercentMobilityChallenge extends PercentMobilityChallenge {

	public BikeSharePercentMobilityChallenge(String templateDir) {
		super(templateDir);
		this.templateName = "BikeSharePercentImproveTemplate.drt";
		this.type = ChallengeType.BSPERCENT;
	}

	@Override
    public void compileChallenge(String playerId)
	    throws UndefinedChallengeException {
		if (mode == null || ! mode.equals("bikesharing"))
			throw new UndefinedChallengeException("undefined challenge!");
		super.compileChallenge(playerId);
	}

}
