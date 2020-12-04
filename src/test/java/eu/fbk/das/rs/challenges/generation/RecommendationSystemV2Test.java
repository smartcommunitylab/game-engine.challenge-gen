package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import org.apache.commons.io.IOUtils;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static eu.fbk.das.utils.Utils.p;

// Test per la versione V2 - con gestione del detect di intervenire con i repetitive
public class RecommendationSystemV2Test extends ChallengesBaseTest {

    @Test
    public void testRepetitiveInterveneAnalyze() throws IOException, ParseException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("query/past-performances/response-1.json");
        String response = IOUtils.toString(is, StandardCharsets.UTF_8.name());
        p(response);
        rs.repetitiveInterveneAnalyze(response);
    }
}
