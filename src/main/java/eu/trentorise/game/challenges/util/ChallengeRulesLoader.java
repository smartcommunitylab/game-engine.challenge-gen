package eu.trentorise.game.challenges.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Load challenges rule from and csv file
 *
 * {@link IOUtils}
 */
public final class ChallengeRulesLoader {

	private static final Logger logger = LogManager
			.getLogger(ChallengeRulesLoader.class);

	private static final String[] COLUMNS = { "NAME", "TYPE", "GOAL_TYPE",
			"TARGET", "BONUS", "POINT_TYPE", "DIFFICULTY", "BASELINE_VARIABLE",
			"SELECTION_CRITERIA_CUSTOM_DATA", "SELECTION_CRITERIA_POINTS",
			"SELECTION_CRITERIA_BADGES" };

	private ChallengeRulesLoader() {
	}

	public static ChallengeRules load(String ref) throws IOException,
			NullPointerException, IllegalArgumentException {
		if (ref == null) {
			logger.error("Input file must be not null");
			throw new NullPointerException("Input file must be not null");
		}
		if (!ref.endsWith(".csv")) {
			logger.error("challenges rules file must be a csv file");
			throw new IllegalArgumentException(
					"challenges rules file must be a csv file");
		}
		BufferedReader rdr = null;
		try {

			try {
				// open csv file
				rdr = new BufferedReader(new StringReader(
						IOUtils.toString(Thread.currentThread()
								.getContextClassLoader()
								.getResourceAsStream(ref))));
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				return null;
			} catch (NumberFormatException e) {
				logger.error(e.getMessage(), e);
				return null;
			} catch (NullPointerException npe) {
				rdr = new BufferedReader(new FileReader(ref));
			}
			ChallengeRules response = new ChallengeRules();
			boolean first = true;
			for (String line = rdr.readLine(); line != null; line = rdr
					.readLine()) {
				if (first) {
					first = false;
					continue;
				}
				String[] elements = line.split(";");
				ChallengeRuleRow crr = new ChallengeRuleRow();
				crr.setName(elements[0]);
				crr.setType(elements[1]);
				crr.setGoalType(elements[2]);
				if (elements[3] != null && !elements[3].isEmpty()) {
					crr.setTarget(Double.valueOf(elements[3]));
				}
				crr.setBonus(Integer.valueOf(elements[4]));
				crr.setPointType(elements[5]);
				crr.setBaselineVar(elements[7]);
				crr.setSelectionCriteriaCustomData(elements[8]);
				if (elements.length > 9) {
					crr.setSelectionCriteriaPoints(elements[9]);
				}
				if (elements.length > 10) {
					crr.setSelectionCriteriaBadges(elements[10]);
				}
				response.getChallenges().add(crr);
			}
			logger.debug("Rows in file " + response.getChallenges().size());
			return response;
		} finally {
			if (rdr != null) {
				rdr.close();
			}
		}
	}

	public static void write(File f, ChallengeRules rules) throws IOException,
			IllegalArgumentException {
		if (f == null) {
			logger.error("Target file must be not null");
			throw new IllegalArgumentException("Target file must be not null");
		}
		if (rules == null) {
			logger.error("Rules must be not null");
			throw new IllegalArgumentException("Rules must be not null");
		}
		FileOutputStream fos = null;
		try {
			StringBuffer toWrite = new StringBuffer();
			toWrite.append(StringUtils.join(COLUMNS, ";") + "\n");
			for (ChallengeRuleRow row : rules.getChallenges()) {
				toWrite.append(row.getName() + ";");
				toWrite.append(row.getType() + ";");
				toWrite.append(row.getGoalType() + ";");
				toWrite.append(row.getTarget() + ";");
				toWrite.append(row.getBonus() + ";");
				toWrite.append(row.getPointType() + ";");
				toWrite.append(";");
				toWrite.append(row.getBaselineVar() + ";");
				toWrite.append(row.getSelectionCriteriaCustomData() + ";");
				toWrite.append(row.getSelectionCriteriaPoints() + ";");
				toWrite.append(row.getSelectionCriteriaBadges() + ";");
				toWrite.append("\n");
			}
			fos = new FileOutputStream(f);
			IOUtils.write(toWrite.toString(), fos);
		} catch (IOException e) {
			logger.error(e);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}
}
