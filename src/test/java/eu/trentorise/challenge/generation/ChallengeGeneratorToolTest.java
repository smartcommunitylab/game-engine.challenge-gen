package eu.trentorise.challenge.generation;

import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import eu.trentorise.game.challenges.ChallengeGeneratorTool;
import eu.trentorise.game.challenges.util.ChallengeRules;

public class ChallengeGeneratorToolTest {

	@Test(expected = IllegalArgumentException.class)
	public void nullTest() {
		ChallengeGeneratorTool.generate(null, null, null, null, null, null,
				null, null, null, null);
	}

	@Test
	public void voidTest() {
		String log = ChallengeGeneratorTool.generate("", "",
				new ChallengeRules(), "", "", "", new Date(), new Date(), "",
				Boolean.FALSE);
		assertTrue(log.contains("Error in reading game state from host"));
	}

}
