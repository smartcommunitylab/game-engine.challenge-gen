package eu.fbk.das.rs.valuator;

import java.util.Map;

public class DifficultyCalculator {

	public final static Integer EASY = 1;
	public final static Integer MEDIUM = 2;
	public final static Integer HARD = 3;
	public final static Integer VERY_HARD = 4;

	public static Integer computeDifficulty(Map<Integer, Double> quartiles, Double baseline, Double target) {
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
		System.out.println("targetZone=" + targetZone);

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

		// if (zone == 1) {
		// if (targetZone == zone) {
		// return EASY;
		// } else if (targetZone - zone == 1) {
		// return MEDIUM;
		// } else if (targetZone - zone == 2) {
		// return HARD;
		// } else if (targetZone - zone == 3) {
		// return VERY_HARD;
		// }
		// } else if (zone == 2) {
		// if (targetZone == zone) {
		// return EASY;
		// } else if (targetZone - zone == 1) {
		// return MEDIUM;
		// } else if (targetZone - zone == 2) {
		// return HARD;
		// } else if (target >= virtualValue * 2) {
		// return VERY_HARD;
		// } else {
		// System.out.println("error during compute diffficulty! for 2");
		// }
		// }
		//
		// else if (zone == 3) {
		// if (targetZone == zone) {
		// return EASY;
		// } else if (targetZone - zone == 1) {
		// return MEDIUM;
		// } else if (target >= virtualValue * 2 && target < virtualValue * 3) {
		// return HARD;
		// } else if (target >= virtualValue * 3) {
		// return VERY_HARD;
		// }
		//
		// else {
		// System.out.println("error during compute diffficulty! for 3");
		// }
		//
		// } else if (zone == 4) {
		// if (target < virtualValue) {
		// return EASY;
		// } else if (target >= virtualValue * 2 && target < virtualValue * 3) {
		// return MEDIUM;
		// } else if (target >= virtualValue * 3 && target < virtualValue * 4) {
		// return HARD;
		// } else if (target >= virtualValue * 4) {
		// return VERY_HARD;
		// } else {
		// System.out.println("error during compute diffficulty! for 4");
		// }
		// }
		// return EASY;
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
