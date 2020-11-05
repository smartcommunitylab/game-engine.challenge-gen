package eu.fbk.das.rs;

import eu.fbk.das.rs.challenges.calculator.PlanePointFunction;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PlaneFunctionTest {

    private int nrow;
    private int ncol;
    private Double min;
    private Double max;
    private Double intermediate;
    private PlanePointFunction ppf;
    private Double approximator;

    @Before
    public void setup() {
        this.nrow = 4;
        this.ncol = 10;
        this.min = 150.0;
        this.max = 300.0;
        this.intermediate = 200.0;
        this.approximator = 10.0;

        this.ppf = new PlanePointFunction(nrow, ncol, min, max, intermediate,
                approximator);
    }

    @Test
    public void valuesAreDouble() {
        assertTrue(ppf.get(0, 0) instanceof Double);
    }

    @Test
    public void minPlaneFunctionTest() {
        assertTrue(ppf.get(0, 0).equals(ppf.getMin()));
    }

    @Test
    public void maxPlaneFunctionTest() {
        assertTrue(ppf.get(ppf.getNrow() - 1, ppf.getNcol() - 1).equals(
                ppf.getMax()));
    }

    @Test
    public void intermediatePlaneFunctionTest() {
        assertTrue(ppf.get(0, ppf.getNcol() - 1).equals(ppf.getIntermediate()));
    }

    @Test
    public void approximatorPlaneFunctionTest() {
        assertTrue(ppf.get(1, ppf.getNcol() - 1).equals(230.0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void approximatorMustBeGreaterThanZeroTest() {
        new PlanePointFunction(nrow, ncol, min, max, intermediate, -1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void approximatorMustBeLessThanMinPlaneFunctionTest() {
        new PlanePointFunction(nrow, ncol, min, max, intermediate, min + 1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void minMustBeLessthanMaxPlaneFunctionTest() {
        new PlanePointFunction(nrow, ncol, max, min, intermediate, min + 1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void minAndMaxMustBeDifferentFromZeroFunctionTest() {
        new PlanePointFunction(nrow, ncol, 0.0, 0.0, intermediate, min + 1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void maxMustBeMorethanMinPlaneFunctionTest() {
        new PlanePointFunction(nrow, ncol, min, min + 1, intermediate, min + 1);
    }



}
