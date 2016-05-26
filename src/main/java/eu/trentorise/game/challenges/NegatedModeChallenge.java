package eu.trentorise.game.challenges;

import eu.trentorise.game.challenges.model.ChallengeType;

// import eu.trentorise.game.model.Player;

public class NegatedModeChallenge extends TripNumberChallenge {

    public NegatedModeChallenge(String templateDir) {
    	super(templateDir);
    	this.templateName = "NegateModeTemplate.drt";
    	this.type = ChallengeType.NEGATEDMODE;
    }

}