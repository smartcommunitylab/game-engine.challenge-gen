package eu.trentorise.game.challenges;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.Challenge;
import eu.trentorise.game.challenges.model.ChallengeType;

public class PointsEarnedChallenge extends Challenge {
    private String pointType = null;
    private Integer prize = null;
    private long pointsTarget = 0l;

    public PointsEarnedChallenge(String templateDir) {
	super(templateDir, "PointsEarnedTemplate.drt");
	generateChId();
	type = ChallengeType.POINTSEARNED;
    }

    @Override
    public void setTemplateParams(Map<String, Object> tp)
	    throws UndefinedChallengeException {
	templateParams = new HashMap<String, Object>();
	templateParams.put(Constants.CH_ID, this.chId);

	if (!tp.containsKey(Constants.POINT_TYPE))
	    throw new UndefinedChallengeException("undefined challenge!");
	this.pointType = (String) tp.get(Constants.POINT_TYPE);
	templateParams.put(Constants.CH_POINT_TYPE, this.pointType);

	setCustomData(tp);
    }

    @Override
    protected void setCustomData(Map<String, Object> tp)
	    throws UndefinedChallengeException {
	super.setCustomData(tp);

	customData
		.put(Constants.CH + this.chId + "_point_type", this.pointType);
	if (!tp.containsKey("bonus"))
	    throw new UndefinedChallengeException("undefined challenge!");
	this.prize = (Integer) tp.get("bonus");
	customData.put("ch_" + this.chId + "_bonus", this.prize);

	if (!tp.containsKey("target"))
	    throw new UndefinedChallengeException("undefined challenge!");
	if (tp.get("target") instanceof Double) {
	    this.pointsTarget = ((Double) tp.get("target")).longValue();
	} else {
	    this.pointsTarget = ((Long) tp.get("target")).longValue();
	}
	customData.put("ch_" + this.chId + "_target", this.pointsTarget);

	/*
	 * customData.put("ch_" + this.chId +
	 * "_points_earned_during_challenges", new Long(0));
	 */
    }

    @Override
    public void compileChallenge(String playerId)
	    throws UndefinedChallengeException {
	if (pointsTarget <= 0l || prize == null || pointType == null)
	    throw new UndefinedChallengeException("undefined challenge!");

	// here find the players affected by this one challenge
	templateParams.put("ch_player", playerId);
	try {
	    generatedRules += generateRules();
	} catch (IOException ioe) {
	    throw new UndefinedChallengeException(
		    "challenge cannot be compiled for user " + playerId);
	}
	return;
    }

    @Override
    public String toString() {
	StringBuffer sb = new StringBuffer();
	sb.append(this.type + ";");
	sb.append(";");
	sb.append(";");
	sb.append(this.pointsTarget + ";");
	sb.append(this.prize + ";");
	sb.append(this.pointType + ";");
	sb.append(this.chId);
	return sb.toString();
    }

}
