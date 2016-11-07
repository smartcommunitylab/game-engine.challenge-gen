package eu.fbk.das.rs.valuator;

/**
 * Plane point function
 */
public class PlanePointFunction {

	private int nrow;
	private int ncol;
	private long min;
	private long max;
	private long intermediate;
	private long matrix[][];
	private long approximator;

	/**
	 * Compute a plane point function (aka a matrix) of values with given size
	 * (nrow and ncol) using min, max and intermediate for values. Uses
	 * approximator to round matrix values to the closest value (i.e 183 => 180,
	 * 199 => 200)
	 */
	public PlanePointFunction(int nrow, int ncol, long min, long max,
			long intermediate, long approximator) {
		if (min == 0 || max == 0 || min > max || max < min) {
			throw new IllegalArgumentException(
					"Min and max must be not null and min more than max");
		}
		if (approximator <= 0) {
			throw new IllegalArgumentException(
					"Approximator must be greater than zero");
		}
		if (approximator > min) {
			throw new IllegalArgumentException(
					"Approximator must be greater than minimum: " + min);
		}
		this.nrow = nrow;
		this.ncol = ncol;
		this.min = min;
		this.max = max;
		this.intermediate = intermediate;
		this.approximator = approximator;
		// init
		this.matrix = new long[nrow][ncol];
		for (int i = 0; i < nrow; i++) {
			for (int j = 0; j < ncol; j++) {
				matrix[i][j] = 0;
			}
		}
		// build matrix
		calculate();
	}

	private void calculate() {
		double dh = (double) ((intermediate - min)) / (double) (ncol - 1);
		double dv = (double) (max - intermediate) / (double) (nrow - 1);

		for (int i = 0; i < nrow; i++) {
			for (int j = 0; j < ncol; j++) {
				matrix[i][j] = Math.round(Math.round(min + dh * j + i * dv)
						/ (double) approximator)
						* approximator;
			}
		}
		System.out.println();
	}

	public long get(int x, int y) {
		return matrix[x][y];
	}

	public long getMin() {
		return min;
	}

	public int getNrow() {
		return nrow;
	}

	public int getNcol() {
		return ncol;
	}

	public long getMax() {
		return max;
	}

	public long getIntermediate() {
		return intermediate;
	}

	public long getTryOncePrize(int x, int y) {
		return matrix[x][y];
	}
}
