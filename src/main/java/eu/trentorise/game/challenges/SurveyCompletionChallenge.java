package eu.trentorise.game.challenges;

import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.Challenge;
import eu.trentorise.game.challenges.model.ChallengeType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SurveyCompletionChallenge extends Challenge {
    private Integer prize = null;
    private String pointType = null;

    public SurveyCompletionChallenge(String templateDir) {
        super(templateDir, "SurveyCompleteTemplate.drt");
        generateChId();
        type = ChallengeType.SURVEYDATA;
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

        if (!tp.containsKey("bonus"))
            throw new UndefinedChallengeException("undefined challenge!");
        this.prize = (Integer) tp.get("bonus");
        customData.put("ch_" + this.chId + "_bonus", this.prize);

    }

    @Override
    public void compileChallenge(String playerId)
            throws UndefinedChallengeException {
        if (prize == null)
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
        sb.append(";");
        sb.append(this.prize + ";");
        sb.append(this.pointType + ";");
        sb.append(this.chId);
        return sb.toString();
    }

}
