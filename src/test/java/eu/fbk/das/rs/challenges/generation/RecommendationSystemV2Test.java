package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import it.smartcommunitylab.model.PlayerStateDTO;
import org.apache.commons.io.IOUtils;
import org.chocosolver.solver.constraints.nary.nvalue.amnv.differences.D;
import org.joda.time.DateTime;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import static eu.fbk.das.utils.Utils.f;
import static eu.fbk.das.utils.Utils.p;

// Test per la versione V2 - con gestione del detect di intervenire con i repetitive
public class RecommendationSystemV2Test extends ChallengesBaseTest {

    public RecommendationSystemV2Test() {
        prod = true;
    }

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
        p(rs.getRepetitiveQuery("1345", new DateTime().toDate()));
    }

    @Test
    public void testRepetitiveInterveneSingle() throws java.text.ParseException {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        conf.put("GAMEID", ferrara20_gameid);
        conf.put("execDate", "2020-12-08");

        Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
         // for (String pId: pIds) p(pId);

        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(conf.get("execDate"));

        PlayerStateDTO state = facade.getPlayerState(ferrara20_gameid, "29774");
        rs.repetitiveIntervene(state, date);
    }

    @Test
    public void testRepetitiveIntervene() throws java.text.ParseException {
        String ferrara20_gameid = conf.get("FERRARA20_GAMEID");
        conf.put("GAMEID", ferrara20_gameid);
        conf.put("execDate", "2021-01-20");

        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(conf.get("execDate"));

        Set<String> pIds = facade.getGamePlayers(ferrara20_gameid);
        for (String pId: pIds) {
            p(pId);
            PlayerStateDTO state = facade.getPlayerState(ferrara20_gameid, pId);
            rs.repetitiveIntervene(state, date);
        }

    }
}
