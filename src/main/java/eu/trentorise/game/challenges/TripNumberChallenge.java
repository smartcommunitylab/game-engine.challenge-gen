package eu.trentorise.game.challenges;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.Challenge;
import eu.trentorise.game.challenges.model.ChallengeType;

// import eu.trentorise.game.model.Player;

public class TripNumberChallenge extends Challenge {
    private int nTrips = 0;
    private String mode = null;
    private Integer prize = null;
    private String pointType = null;

    public TripNumberChallenge(String templateDir) {
	super(templateDir, "TravelModeTemplate.drt");
	generateChId();
	type = ChallengeType.TRIPNUMBER;

    }

    @Override
    public void setTemplateParams(Map<String, Object> tp)
	    throws UndefinedChallengeException {
	templateParams = new HashMap<String, Object>();
	templateParams.put("ch_ID", this.chId);

	if (!tp.containsKey("mode"))
	    throw new UndefinedChallengeException("undefined challenge!");
	this.mode = (String) tp.get("mode");
	templateParams.put("ch_mode", this.mode);

	if (!tp.containsKey("point_type"))
	    throw new UndefinedChallengeException("undefined challenge!");
	this.pointType = (String) tp.get("point_type");
	templateParams.put("ch_point_type", this.pointType);

	setCustomData(tp);
    }

    @Override
    protected void setCustomData(Map<String, Object> tp)
	    throws UndefinedChallengeException {
	super.setCustomData(tp);

	customData.put("ch_" + this.chId + "_mode", this.mode);
	customData.put("ch_" + this.chId + "_point_type", this.pointType);

	if (!tp.containsKey("target"))
	    throw new UndefinedChallengeException(
		    "undefined target for challenge!");
	this.nTrips = ((Double) tp.get("target")).intValue();
	customData.put("ch_" + this.chId + "_target", this.nTrips);

	if (!tp.containsKey("bonus"))
	    throw new UndefinedChallengeException("undefined challenge!");
	this.prize = (Integer) tp.get("bonus");
	customData.put("ch_" + this.chId + "_bonus", this.prize);

	customData.put("ch_" + this.chId + "_counter", new Integer(0));
    }

    @Override
    public void compileChallenge(String playerId)
	    throws UndefinedChallengeException {
	if (nTrips <= 0 || mode == null || prize == null)
	    throw new UndefinedChallengeException("undefined challenge!");

	templateParams.put("ch_player", playerId);
	try {
	    generatedRules += generateRules();
	} catch (IOException ioe) {
	    throw new UndefinedChallengeException(
		    "challenge cannot be compiled for user " + playerId);
	}
    }

    @Override
    public String toString() {
	StringBuffer sb = new StringBuffer();
	sb.append(this.type + ";");
	sb.append(this.mode + ";");
	sb.append(";");
	sb.append(this.nTrips + ";");
	sb.append(this.prize + ";");
	sb.append(this.pointType + ";");
	sb.append(this.chId);
	return sb.toString();
    }

}