package eu.trentorise.game.challenges.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.drools.template.ObjectDataCompiler;

import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.util.CalendarUtil;

public abstract class Challenge {

	protected String drlName;

	protected int difficulty = 0;
	protected UUID chId;
	protected ChallengeType type;
	protected String templateName;
	protected Collection<Object> players = null;

	protected HashMap<String, Object> templateParams = null;
	protected HashMap<String, Object> customData = null;
	protected String generatedRules = "";

	private String templateDir;

	/**
	 * Create a new challenge using given template
	 * 
	 * @param templateName
	 */
	public Challenge(String templateDir, String templateName) {
		this.templateDir = templateDir;
		this.templateName = templateName;
	}

	public abstract void compileChallenge(String playerId)
			throws UndefinedChallengeException;

	public abstract void setTemplateParams(Map<String, Object> tp)
			throws UndefinedChallengeException;

	protected void setCustomData(Map<String, Object> tp)
			throws UndefinedChallengeException {
		customData = new HashMap<String, Object>();
		customData.put(Constants.CH + this.chId + Constants.TYPE, this.type);
		// add beginning and end of challenge
		Calendar calendar = (CalendarUtil.getStart());
		customData.put(Constants.CH + this.chId + Constants.START_CHTS,
				calendar.getTimeInMillis());
		// add challenge duration
		customData.put(Constants.CH + this.chId + Constants.END_CHTS,
				CalendarUtil.getEnd().getTimeInMillis());
		customData.put(Constants.CH + this.chId + Constants.SUCCESS, false);
	}

	protected final void generateChId() {
		this.chId = UUID.randomUUID();
	}

	protected String generateRules() throws IOException {
		ObjectDataCompiler compiler = new ObjectDataCompiler();
		InputStream templateStream = Thread
				.currentThread()
				.getContextClassLoader()
				.getResourceAsStream(
						templateDir + File.separator + templateName);
		if (templateStream == null) {
			templateStream = new FileInputStream(templateDir + File.separator
					+ templateName);
		}
		return compiler.compile(Arrays.asList(templateParams), templateStream);
	}

	public UUID getChId() {
		return chId;
	}

	public ChallengeType getType() {
		return type;
	}

	public String getGeneratedRules() {
		return generatedRules;
	}

	public HashMap<String, Object> getTemplateParams() {
		return templateParams;
	}

	public HashMap<String, Object> getCustomData() {
		return customData;
	}

}