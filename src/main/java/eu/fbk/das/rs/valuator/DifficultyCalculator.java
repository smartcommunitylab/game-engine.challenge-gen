package eu.fbk.das.rs.valuator;

import java.util.Map;

public class DifficultyCalculator {

	public final static Integer EASY = 1;
	public final static Integer MEDIUM = 2;
	public final static Integer HARD = 3;
	public final static Integer VERY_HARD = 4;

	public static Integer computeDifficulty(Map<Integer, Double> quartiles, Integer zone, Double baseline,
			Double target) {
		Integer targetZone = computeZone(quartiles, target);
		System.out.println("targetZone=" + targetZone);
		double virtualValue = quartiles.get(9) - quartiles.get(7);

		if (zone == 1) {
			if (targetZone == zone) {
				return EASY;
			} else if (targetZone - zone == 1) {
				return MEDIUM;
			} else if (targetZone - zone == 2) {
				return HARD;
			} else if (targetZone - zone == 3) {
				return VERY_HARD;
			}
		} else if (zone == 2) {
			if (targetZone == zone) {
				return EASY;
			} else if (targetZone - zone == 1) {
				return MEDIUM;
			} else if (targetZone - zone == 2) {
				return HARD;
			} else if (target >= virtualValue * 2) {
				return VERY_HARD;
			} else {
				System.out.println("error during compute diffficulty! for 2");
			}
		}

		else if (zone == 3) {
			if (targetZone == zone) {
				return EASY;
			} else if (targetZone - zone == 1) {
				return MEDIUM;
			} else if (target >= virtualValue * 2 && target < virtualValue * 3) {
				return HARD;
			} else if (target >= virtualValue * 3) {
				return VERY_HARD;
			}

			else {
				System.out.println("error during compute diffficulty! for 3");
			}

		} else if (zone == 4) {
			if (targetZone == zone) {
				return EASY;
			} else if (target >= virtualValue * 2 && target < virtualValue * 3) {
				return MEDIUM;
			} else if (target >= virtualValue * 3 && target < virtualValue * 4) {
				return HARD;
			} else if (target >= virtualValue * 4) {
				return VERY_HARD;
			} else {
				System.out.println("error during compute diffficulty! for 4");
			}
		}
		return EASY;
	}

	public static Integer computeZone(Map<Integer, Double> quartiles, Double baseline) {
		if (baseline <= quartiles.get(4)) {
			return 1;
		} else if (baseline > quartiles.get(4) && baseline <= quartiles.get(7)) {
			return 2;
		} else if (baseline > quartiles.get(7) && baseline <= quartiles.get(9)) {
			return 3;
		} else if (baseline > quartiles.get(9)) {
			return 4;
		}
		return null;
	}

}
