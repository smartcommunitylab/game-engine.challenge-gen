package eu.fbk.das.rs;

import eu.fbk.das.rs.sortfilter.DifficultyPrizeComparator;
import eu.fbk.das.rs.valuator.DifficultyCalculator;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertTrue;

public class DifficultyCalculatorTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullQuartiles() {
        Double baseline = 1.2;
        Double target = 60.43;
        DifficultyCalculator.computeDifficulty(null, baseline, target);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullBaseline() {
        Map<Integer, Double> quartiles = new HashMap<Integer, Double>();
        quartiles.put(4, 3.99);
        quartiles.put(7, 12.516551);
        quartiles.put(9, 30.51);
        Double target = 60.43;
        DifficultyCalculator.computeDifficulty(quartiles, null, target);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullTarget() {
        Map<Integer, Double> quartiles = new HashMap<Integer, Double>();
        quartiles.put(4, 3.99);
        quartiles.put(7, 12.516551);
        quartiles.put(9, 30.51);
        Double baseline = 1.2;
        DifficultyCalculator.computeDifficulty(quartiles, baseline, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQuartilesLevelsTarget() {
        Map<Integer, Double> quartiles = new HashMap<Integer, Double>();
        quartiles.put(1, 3.99);
        Double baseline = 1.2;
        Double target = 60.43;
        DifficultyCalculator.computeDifficulty(quartiles, baseline, target);
    }

    @Test
    public void testDifficultyEasy() {
        Map<Integer, Double> quartiles = new HashMap<Integer, Double>();
        quartiles.put(4, 3.99);
        quartiles.put(7, 12.516551);
        quartiles.put(9, 30.51);
        Double baseline = 1.2;
        Double target = 2.4;
        Integer difficulty = DifficultyCalculator.computeDifficulty(quartiles,
                baseline, target);
        System.out.println("Difficulty: " + difficulty);
        assertTrue(difficulty == DifficultyCalculator.EASY);
    }

    @Test
    public void testDifficultyMedium() {
        Map<Integer, Double> quartiles = new HashMap<Integer, Double>();
        quartiles.put(4, 3.99);
        quartiles.put(7, 12.516551);
        quartiles.put(9, 30.51);
        Double baseline = 1.2;
        Double target = 4.0;
        Integer difficulty = DifficultyCalculator.computeDifficulty(quartiles,
                baseline, target);
        System.out.println("Difficulty: " + difficulty);
        assertTrue(difficulty == DifficultyCalculator.MEDIUM);
    }

    @Test
    public void testDifficultyHard() {
        Map<Integer, Double> quartiles = new HashMap<Integer, Double>();
        quartiles.put(4, 3.99);
        quartiles.put(7, 12.516551);
        quartiles.put(9, 30.51);
        Double baseline = 1.2;
        Double target = 24.0;
        Integer difficulty = DifficultyCalculator.computeDifficulty(quartiles,
                baseline, target);
        System.out.println("Difficulty: " + difficulty);
        assertTrue(difficulty == DifficultyCalculator.HARD);
    }

    @Test
    public void testDifficultyVeryHard() {
        Map<Integer, Double> quartiles = new HashMap<Integer, Double>();
        quartiles.put(4, 3.99);
        quartiles.put(7, 12.516551);
        quartiles.put(9, 30.51);
        Double baseline = 1.2;
        Double target = 60.43;
        Integer difficulty = DifficultyCalculator.computeDifficulty(quartiles,
                baseline, target);
        System.out.println("Difficulty: " + difficulty);
        assertTrue(difficulty == DifficultyCalculator.VERY_HARD);
    }

    @Test
    public void difficultyPrizeComparatorTest() {
        List<ChallengeDataDTO> test = new ArrayList<ChallengeDataDTO>();
        ChallengeDataDTO first = new ChallengeDataDTO();

        first.setInstanceName("Instance1");
        first.setData("difficulty", 3);
        first.setData("bonusScore", 100.0);
        first.setData("wi", 100.0);
        test.add(first);

        ChallengeDataDTO second = new ChallengeDataDTO();
        second.setInstanceName("Instance2");
        second.setData("difficulty", 1);
        second.setData("bonusScore", 200.0);
        second.setData("wi", 200.0);
        test.add(second);

        Collections.sort(test, new DifficultyPrizeComparator());

        assertTrue(test.get(0).getInstanceName().equals("Instance2"));
    }

}
