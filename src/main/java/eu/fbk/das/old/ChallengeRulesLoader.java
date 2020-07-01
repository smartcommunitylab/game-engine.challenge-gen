package eu.fbk.das.old;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static eu.fbk.das.rs.utils.Utils.dbg;

/**
 * Load challenges rule from and csv file
 * <p>
 * {@link IOUtils}
 */
public final class ChallengeRulesLoader {

    private static final Logger logger = Logger.getLogger(ChallengeRulesLoader.class);

    private static final String[] COLUMNS = {"NAME", "TYPE", "GOAL_TYPE", "TARGET", "BONUS",
            "POINT_TYPE", "PERIOD_NAME", "PERIOD_TARGET", "DIFFICULTY", "BASELINE_VARIABLE",
            "SELECTION_CRITERIA_POINTS", "SELECTION_CRITERIA_BADGES"};


    private ChallengeRulesLoader() {
    }


    private static Map<String, Integer> mapTheColums(String values) {
        Map<String, Integer> map = new HashMap<>();
        if (values == null) {
            values = "";
        }
        String[] columns = values.split(";");
        for (int position = 0; position < columns.length; position++) {
            map.put(columns[position], position);
        }

        return map;
    }

    public static ChallengeRules load(String ref)
            throws IOException, NullPointerException, IllegalArgumentException {
        if (ref == null) {
            logger.error("Input file must be not null");
            throw new NullPointerException("Input file must be not null");
        }
        if (!ref.endsWith(".csv")) {
            logger.error("challenges rules file must be a csv file");
            throw new IllegalArgumentException("challenges rules file must be a csv file");
        }
        BufferedReader rdr = null;
        try {

            try {
                // open csv file
                rdr = new BufferedReader(new StringReader(IOUtils.toString(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream(ref))));
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
            Map<String, Integer> columnsMapping = null;
            for (String line = rdr.readLine(); line != null; line = rdr.readLine()) {
                if (first) {
                    columnsMapping = mapTheColums(line);
                    first = false;
                    continue;
                }
                String[] elements = line.split(";");
                ChallengeRuleRow crr = new ChallengeRuleRow();
                crr.setName(elements[columnsMapping.get("NAME")]);
                crr.setModelName(elements[columnsMapping.get("TYPE")]);
                crr.setGoalType(elements[columnsMapping.get("GOAL_TYPE")]);
                if (elements[columnsMapping.get("TARGET")] != null
                        && !elements[columnsMapping.get("TARGET")].isEmpty()) {
                    try {
                        crr.setTarget(Double.valueOf(elements[columnsMapping.get("TARGET")]));
                    } catch (NumberFormatException nfe) {
                        dbg(logger,
                                "Target value is not a number, current challenge is a LeaderboardPosition?");
                        crr.setTarget(elements[columnsMapping.get("TARGET")]);
                    }
                }

                // for compatibility with old version of csv
                if (columnsMapping.get("PERIOD_NAME") != null) {
                    crr.setPeriodName(elements[columnsMapping.get("PERIOD_NAME")]);
                }
                // for compatibility with old version of csv
                if (columnsMapping.get("PERIOD_TARGET") != null) {
                    crr.setPeriodTarget(elements[columnsMapping.get("PERIOD_TARGET")]);
                }
                crr.setBonus(Double.valueOf(elements[columnsMapping.get("BONUS")]));
                crr.setPointType(elements[columnsMapping.get("POINT_TYPE")]);
                crr.setBaselineVar(elements[columnsMapping.get("BASELINE_VARIABLE")]);
                // maybe if can be removed
                if (elements.length > columnsMapping.get("SELECTION_CRITERIA_POINTS")) {
                    crr.setSelectionCriteriaPoints(
                            elements[columnsMapping.get("SELECTION_CRITERIA_POINTS")]);
                }
                if (elements.length > columnsMapping.get("SELECTION_CRITERIA_BADGES")) {
                    crr.setSelectionCriteriaBadges(
                            elements[columnsMapping.get("SELECTION_CRITERIA_BADGES")]);
                }
                response.getChallenges().add(crr);
            }
            dbg(logger, "Rows in file: %d", response.getChallenges().size());
            return response;
        } finally {
            if (rdr != null) {
                rdr.close();
            }
        }
    }

    public static void write(File f, ChallengeRules rules)
            throws IOException, IllegalArgumentException {
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
            toWrite.append(StringUtils.join(COLUMNS, ";") + Constants.LINE_SEPARATOR);
            for (ChallengeRuleRow row : rules.getChallenges()) {
                toWrite.append(row.getName() + ";");
                toWrite.append(row.getModelName() + ";");
                toWrite.append(row.getGoalType() + ";");
                toWrite.append(row.getTarget() + ";");
                toWrite.append(row.getBonus() + ";");
                toWrite.append(row.getPointType() + ";");
                toWrite.append(row.getPeriodName() + ";");
                toWrite.append(row.getPeriodTarget() + ";");
                toWrite.append(";");
                toWrite.append(row.getBaselineVar() + ";");
                toWrite.append(row.getSelectionCriteriaPoints() + ";");
                toWrite.append(row.getSelectionCriteriaBadges() + ";");
                toWrite.append(Constants.LINE_SEPARATOR);
            }
            fos = new FileOutputStream(f);
            IOUtils.write(toWrite.toString(), fos);
        } catch (IOException e) {
            logger.error(e);
        } finally {
            if (fos != null) {
                fos.flush();
                fos.close();
            }
        }
    }
}
