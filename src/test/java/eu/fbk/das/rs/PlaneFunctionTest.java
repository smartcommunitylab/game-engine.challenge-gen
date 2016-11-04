package eu.fbk.das.rs;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import eu.fbk.das.rs.valuator.PlanePointFunction;

public class PlaneFunctionTest {

	private int nrow;
	private int ncol;
	private int min;
	private int max;
	private int intermediate;
	private PlanePointFunction ppf;
	private long approximator;

	@Before
	public void setup() {
		this.nrow = 4;
		this.ncol = 10;
		this.min = 100;
		this.max = 250;
		this.intermediate = 150;
		this.approximator = 5;

		this.ppf = new PlanePointFunction(nrow, ncol, min, max, intermediate,
				approximator);
	}

	@Test
	public void minPlaneFunctionTest() {
		assertTrue(ppf.get(0, 0) == ppf.getMin());
	}

	@Test
	public void maxPlaneFunctionTest() {
		assertTrue(ppf.get(ppf.getNrow() - 1, ppf.getNcol() - 1) == ppf
				.getMax());
	}

	@Test
	public void intermediatePlaneFunctionTest() {
		assertTrue(ppf.get(0, ppf.getNcol() - 1) == ppf.getIntermediate());
	}

	@Test
	public void approximatorPlaneFunctionTest() {
		assertTrue(ppf.get(1, ppf.getNcol() - 1) == 185);
	}

	@Test(expected = IllegalArgumentException.class)
	public void approximatorMustBeGreaterThanZeroTest() {
		new PlanePointFunction(nrow, ncol, min, max, intermediate, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void approximatorMustBeLessThanMinPlaneFunctionTest() {
		new PlanePointFunction(nrow, ncol, min, max, intermediate, min + 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void minMustBeLessthanMaxPlaneFunctionTest() {
		new PlanePointFunction(nrow, ncol, max, min, intermediate, min + 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void minAndMaxMustBeDifferentFromZeroFunctionTest() {
		new PlanePointFunction(nrow, ncol, 0, 0, intermediate, min + 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void maxMustBeMorethanMinPlaneFunctionTest() {
		new PlanePointFunction(nrow, ncol, min, min + 1, intermediate, min + 1);
	}

}
