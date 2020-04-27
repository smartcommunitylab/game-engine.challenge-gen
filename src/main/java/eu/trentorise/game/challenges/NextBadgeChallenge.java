package eu.trentorise.game.challenges;

import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.Challenge;
import eu.trentorise.game.challenges.model.ChallengeType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NextBadgeChallenge extends Challenge {
    private int nextBadgesNum = 0;
    private String badgeCollection = null;
    private Integer prize = null;
    private String pointType = null;

    public NextBadgeChallenge(String templateDir) {
        super(templateDir, "NextBadgeTemplate.drt");
        generateChId();
        type = ChallengeType.NEXTBADGE;
    }

    @Override
    public void compileChallenge(String playerId)
            throws UndefinedChallengeException {
        if (badgeCollection == null || prize == null)
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
    public void setTemplateParams(Map<String, Object> tp)
            throws UndefinedChallengeException {
        templateParams = new HashMap<String, Object>();
        templateParams.put("ch_ID", this.chId);

        if (!tp.containsKey("point_type"))
            throw new UndefinedChallengeException("undefined challenge!");
        this.pointType = (String) tp.get("point_type");
        templateParams.put("ch_point_type", this.pointType);

        if (!tp.containsKey("badge_collection"))
            throw new UndefinedChallengeException("undefined challenge!");
        this.badgeCollection = (String) tp.get("badge_collection");
        templateParams.put("ch_badge_collection", this.badgeCollection);

        setCustomData(tp);

    }

    @Override
    protected void setCustomData(Map<String, Object> tp)
            throws UndefinedChallengeException {
        super.setCustomData(tp);

        customData.put("ch_" + this.chId + "_point_type", this.pointType);
        customData.put("ch_" + this.chId + "_badge_collection",
                this.badgeCollection);

        if (!tp.containsKey("target"))
            throw new UndefinedChallengeException(
                    "undefined target for challenge!");
        if (tp.get("target") instanceof Double) {
            this.nextBadgesNum = ((Double) tp.get("target")).intValue();
        } else {
            this.nextBadgesNum = ((Long) tp.get("target")).intValue();
        }
        customData.put(Constants.CH + this.chId + "_target", nextBadgesNum);

        if (!tp.containsKey("bonus"))
            throw new UndefinedChallengeException("undefined challenge!");
        this.prize = (Integer) tp.get("bonus");
        customData.put("ch_" + this.chId + "_bonus", this.prize);

        customData.put("ch_" + this.chId + "_counter", new Integer(0));
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.type + ";");
        sb.append(this.badgeCollection + ";");
        sb.append(";");
        sb.append(this.nextBadgesNum + ";");
        sb.append(this.prize + ";");
        sb.append(this.pointType + ";");
        sb.append(this.chId);
        return sb.toString();
    }

}
