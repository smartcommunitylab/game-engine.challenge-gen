package eu.fbk.das.rs.challenges.evaluation;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import org.junit.Test;

import java.io.IOException;

public class ChallengeDataGuruTest extends ChallengesBaseTest {

    @Test
    public void testWrite() throws IOException {
        ChallengeDataGuru cdg = new ChallengeDataGuru(cfg);
        // cdg.generate("/home/loskana/Desktop/tensorflow/play-classifier/", challenges);
    }

    @Test
    public void testReadCompleted() throws IOException {
        ChallengeDataGuru cdg = new ChallengeDataGuru(cfg);
        cdg.readCompleted("src/test/resources/eu/fbk/das/rs/");
    }

    @Test
    public void testReadAll() throws IOException {
        ChallengeDataGuru cdg = new ChallengeDataGuru(cfg);
        // cdg.readAll("src/test/resources/eu/fbk/das/rs/");
    }

}