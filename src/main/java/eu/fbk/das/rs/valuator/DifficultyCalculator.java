package eu.fbk.das.rs.valuator;

import java.util.Map;

public class DifficultyCalculator {

    public final static Integer EASY = 1;
    public final static Integer MEDIUM = 2;
    public final static Integer HARD = 3;
    public final static Integer VERY_HARD = 4;

    /**
     * @param quartiles
     * @param baseline
     * @param target
     * @return computed difficulty valye for given input
     * @throws if inputs are null
     */
    public static Integer computeDifficulty(Map<Integer, Double> quartiles,
                                            Double baseline, Double target) {
        if (quartiles == null || baseline == null || target == null) {
            throw new IllegalArgumentException("All input must be not null");
        }
        if (quartiles.get(4) == null || quartiles.get(7) == null
                || quartiles.get(9) == null) {
            throw new IllegalArgumentException(
                    "Quartiles that must be defined: 4,7,9");
        }
        double virtualValue = quartiles.get(9) - quartiles.get(7);

        double values[] = new double[6];
        values[0] = quartiles.get(4);
        values[1] = quartiles.get(7);
        values[2] = quartiles.get(9);
        values[3] = quartiles.get(9) + virtualValue;
        values[4] = quartiles.get(9) + virtualValue * 2;
        values[5] = quartiles.get(9) + virtualValue * 3;

        Integer zone = computeZone(values, baseline);
        Integer targetZone = computeZone(values, target);

        Integer diffZone = targetZone - zone;
        if (diffZone == 0) {
            return EASY;
        }
        if (diffZone == 1) {
            return MEDIUM;
        }
        if (diffZone == 2) {
            return HARD;
        }
        return VERY_HARD;
    }

    private static Integer computeZone(double[] values, Double baseline) {
        if (baseline <= values[0]) {
            return 1;
        } else if (baseline > values[0] && baseline <= values[1]) {
            return 2;
        } else if (baseline > values[1] && baseline <= values[2]) {
            return 3;
        } else if (baseline > values[2] && baseline <= values[3]) {
            return 4;
        } else if (baseline > values[3] && baseline <= values[4]) {
            return 5;
        } else if (baseline > values[4] && baseline <= values[5]) {
            return 6;
        } else if (baseline > values[5]) {
            return 7;
        }
        return null;
    }

}
