package eu.trentorise.game.challenges;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.Challenge;
import eu.trentorise.game.challenges.model.ChallengeType;

public class BadgeCollectionCompletionChallenge extends Challenge {
    private String badgeCollection = null;
    private String pointType = null;
    private Integer prize = null;
    private int badgeCollectionMax = 0; // will hold the max number of badges for the given Badge Collection

    public BadgeCollectionCompletionChallenge(String templateDir) {
    	super(templateDir, "BadgeCollectionCompletionTemplate.drt");
    	generateChId();
    	type = ChallengeType.BADGECOLLECTION;
    }

    @Override
    public void setTemplateParams(Map<String, Object> tp)
	    throws UndefinedChallengeException {
    	templateParams = new HashMap<String, Object>();
    	templateParams.put("ch_ID", this.chId);

		if (!tp.containsKey("badge_collection"))
		    throw new UndefinedChallengeException("undefined challenge!");
		this.badgeCollection = (String) tp.get("badge_collection");
		templateParams.put("ch_badge_collection", this.badgeCollection);
	
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
	
		customData.put("ch_" + this.chId + "_point_type", this.pointType);
		customData.put("ch_" + this.chId + "_badge_collection", this.badgeCollection);
	
		if (!tp.containsKey("bonus"))
		    throw new UndefinedChallengeException("undefined challenge!");
		this.prize = ((Integer) tp.get("bonus")).intValue();
		customData.put("ch_" + this.chId + "_bonus", this.prize);
		
		if (!tp.containsKey("target"))
		    throw new UndefinedChallengeException(
			    "undefined target for challenge!");
		this.badgeCollectionMax = ((Double) tp.get("target")).intValue();
		customData.put("ch_" + this.chId + "_target", this.badgeCollectionMax);
    }

    @Override
    public void compileChallenge(String playerId)
	    throws UndefinedChallengeException {
		if (badgeCollection == null || prize == null || badgeCollectionMax <= 0)
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
		sb.append(";");
		sb.append(this.badgeCollection + ";");
		sb.append(this.badgeCollectionMax + ";");
		sb.append(this.prize + ";");
		sb.append(this.pointType + ";");
		sb.append(this.chId);
		return sb.toString();
	}

}
