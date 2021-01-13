package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import org.apache.commons.io.IOUtils;
import org.chocosolver.solver.constraints.nary.nvalue.amnv.differences.D;
import org.joda.time.DateTime;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static eu.fbk.das.utils.Utils.f;
import static eu.fbk.das.utils.Utils.p;

// Test per la versione V2 - con gestione del detect di intervenire con i repetitive
public class RecommendationSystemV2Test extends ChallengesBaseTest {

    @Test
    public void testRepetitiveInterveneAnalyze() throws IOException, ParseException {
        for (int i = 1; i < 4; i++) {
            p(this.getClass().getResource("."));
            InputStream is = getClass().getClassLoader().getResourceAsStream(f("past-performances/response-%d.json", i));
            String response = IOUtils.toString(is, StandardCharsets.UTF_8.name());
            // p(response);
            Double ent = rs.repetitiveInterveneAnalyze(response);
            p(ent);
        }
    }

    @Test
    public void testRepetitiveQuery() throws IOException, ParseException {
        p(this.getClass().getResource("."));
        p(rs.getRepetitiveQuery("1", new DateTime()));
    }
}
