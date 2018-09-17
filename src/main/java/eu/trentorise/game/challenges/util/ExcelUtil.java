package eu.trentorise.game.challenges.util;

import eu.fbk.das.rs.challengeGeneration.RecommendationSystemConfig;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public final class ExcelUtil {

    private static final String PERCENTAGE = "percentage";
    private static final String TARGET = "target";
    private static final String BASELINE = "baseline";
    private static final String WI = "wi";
    private static final String BONUS_SCORE = "bonusScore";
    private static final String DIFFICULTY = "difficulty";
    private static final String COUNTER_NAME = "counterName";

    private ExcelUtil() {
    }

    /**
     * Convert {@link ChallengeDataDTO} to row format for excel writing
     *
     * @param sheet
     * @param row
     * @param playerId
     * @param challenge
     * @return sheet with added rows
     */
    public static Sheet buildRow(RecommendationSystemConfig configuration,
                                 Sheet sheet, Row row, String playerId, ChallengeDataDTO challenge) {
        if (configuration == null || sheet == null || row == null
                || playerId == null || challenge == null) {
            throw new IllegalArgumentException("Illegal arguments: null");
        }
        row.createCell(0).setCellValue(playerId);
        row.createCell(1).setCellValue(challenge.getModelName());
        row.createCell(2).setCellValue(challenge.getInstanceName());
        row.createCell(3).setCellValue(
                (String) challenge.getData().get(COUNTER_NAME));
        row.createCell(4).setCellValue(
                (Integer) configuration.getWeight((String) challenge.getData()
                        .get(COUNTER_NAME)));
        row.createCell(5).setCellValue(
                (Integer) challenge.getData().get(DIFFICULTY));
        row.createCell(6).setCellValue((Double) challenge.getData().get(WI));
        row.createCell(7).setCellValue(
                (Double) challenge.getData().get(BONUS_SCORE));
        if (challenge.getData().get(BASELINE) != null) {
            row.createCell(8).setCellValue(
                    (Double) challenge.getData().get(BASELINE));
        } else {
            row.createCell(8).setCellValue(0d);
        }
        if (challenge.getData().get(TARGET) instanceof Integer) {
            row.createCell(9).setCellValue(
                    (Integer) challenge.getData().get(TARGET));
        } else {
            row.createCell(9).setCellValue(
                    (Double) challenge.getData().get(TARGET));
        }
        if (challenge.getData().get(PERCENTAGE) != null) {
            row.createCell(10).setCellValue(
                    (Double) challenge.getData().get(PERCENTAGE));
        } else {
            row.createCell(10).setCellValue(0);
        }
        return sheet;
    }

    public static Sheet buildRow(String playerId, String customData,
                                 Sheet sheet, Row row) {
        if (playerId == null || customData == null || sheet == null
                || row == null) {
            throw new IllegalArgumentException("Illegal arguments: null");
        }
        row.createCell(0).setCellValue(playerId);
        String[] values = customData.split(";");
        int index = 1;
        for (int i = 0; i < values.length; i++) {
            row.createCell(index).setCellValue(values[i]);
            index++;
        }
        return sheet;
    }

}
