package eu.trentorise.game.challenges;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.Challenge;
import eu.trentorise.game.challenges.model.ChallengeType;

public class PercentMobilityChallenge extends Challenge {
	protected String mode = null;
	private Double percent = 0.0;
	private Double baseline = 0.0;
	private Integer prize = null;
	private String pointType = null;
	private DecimalFormat df = new DecimalFormat("#.00");

	public PercentMobilityChallenge(String templateDir) {
		super(templateDir, "MobilityPercentImproveTemplate.drt");
		generateChId();
		type = ChallengeType.PERCENT;
	}

	@Override
	public void setTemplateParams(Map<String, Object> tp)
			throws UndefinedChallengeException {
		templateParams = new HashMap<String, Object>();
		templateParams.put(Constants.CH_ID, this.chId);

		if (!tp.containsKey(Constants.MODE))
			throw new UndefinedChallengeException("undefined challenge!");
		this.mode = (String) tp.get(Constants.MODE);
		templateParams.put(Constants.CH_MODE, this.mode);

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

		customData.put(Constants.CH + this.chId + "_mode", this.mode);
		customData
				.put(Constants.CH + this.chId + "_point_type", this.pointType);

		if (!tp.containsKey("target"))
			throw new UndefinedChallengeException(
					"undefined value target for challenge!");
		this.percent = ((Double) tp.get("target")).doubleValue();
		if (!tp.containsKey("baseline"))
			throw new UndefinedChallengeException("undefined challenge!");
		this.baseline = ((Double) tp.get("baseline")).doubleValue();
		// round double
		Double value = this.baseline * (1.0 + this.percent);
		value = Double.valueOf(Math.round(value));
		customData.put(Constants.CH + this.chId + "_target", value);

		if (!tp.containsKey("bonus"))
			throw new UndefinedChallengeException("undefined challenge!");
		this.prize = ((Integer) tp.get("bonus")).intValue();
		customData.put(Constants.CH + this.chId + "_bonus", this.prize);

		customData.put(Constants.CH + this.chId
				+ "_Km_traveled_during_challenge", new Double(0.0));
	}

	@Override
	public void compileChallenge(String playerId)
			throws UndefinedChallengeException {
		if (mode == null || prize == null || percent <= 0 || baseline <= 0)
			throw new UndefinedChallengeException("undefined challenge!");

		templateParams.put("ch_player", playerId);
		try {
			generatedRules += generateRules();
		} catch (IOException ioe) {
			throw new UndefinedChallengeException(
					"challenge cannot be compiled for user " + playerId, ioe);
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.type + ";");
		sb.append(this.mode + ";");
		String baseline = df.format(this.baseline);
		sb.append(baseline + ";");
		Double value = this.baseline * (1.0 + this.percent);
		value = Double.valueOf(Math.round(value));
		sb.append(value + ";");
		sb.append(this.prize + ";");
		sb.append(this.pointType + ";");
		sb.append(this.chId);
		return sb.toString();
	}
}
