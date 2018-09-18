package eu.trentorise.game.challenges;

import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.Challenge;
import eu.trentorise.game.challenges.model.ChallengeType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ZeroImpactChallenge extends Challenge {
    private long nTrips = 0;
    private Integer prize = null;
    private String pointType = null;

    public ZeroImpactChallenge(String templateDir) {
        super(templateDir, "ZeroImpactTemplate.drt");
        generateChId();
        type = ChallengeType.ZEROIMPACT;
    }

    @Override
    public void compileChallenge(String playerId)
            throws UndefinedChallengeException {
        if (nTrips <= 0 || prize == null)
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

        setCustomData(tp);
    }

    @Override
    protected void setCustomData(Map<String, Object> tp)
            throws UndefinedChallengeException {
        super.setCustomData(tp);

        customData.put("ch_" + this.chId + "_point_type", this.pointType);

        if (!tp.containsKey("target"))
            throw new UndefinedChallengeException(
                    "undefined target for challenge!");
        if (tp.get("target") instanceof Double) {
            this.nTrips = ((Double) tp.get("target")).longValue();
        } else {
            this.nTrips = ((Long) tp.get("target")).longValue();
        }

        customData.put("ch_" + this.chId + "_target", this.nTrips);

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
        sb.append(";");
        sb.append(";");
        sb.append(this.nTrips + ";");
        sb.append(this.prize + ";");
        sb.append(this.pointType + ";");
        sb.append(this.chId);
        return sb.toString();
    }

}
