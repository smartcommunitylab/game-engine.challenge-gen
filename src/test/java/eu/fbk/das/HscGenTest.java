package eu.fbk.das;

import eu.fbk.das.api.RecommenderSystemImpl;
import eu.fbk.das.model.ChallengeExpandedDTO;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.fbk.das.rs.challenges.calculator.ChallengesConfig;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import java.util.*;

import static eu.fbk.das.rs.challenges.generation.RecommendationSystem.getChallengeWeek;
import static eu.fbk.das.utils.Utils.p;

public class HscGenTest extends ChallengesBaseTest {

    @Test
    public void checkHSCGeneration() {

        conf.put("GAMEID", "63eb47e2f3ffe74ae078d250");

        DateTime execDate = new DateTime("2023-03-28");

        DateTime startDate = new DateTime("2023-03-27");

        DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();

        RecommenderSystemImpl api = new RecommenderSystemImpl();

        Set<String> modelTypes = new HashSet<>();
        modelTypes.add(ChallengesConfig.BIKE_KM);
        modelTypes.add(ChallengesConfig.WALK_KM);
        modelTypes.add(ChallengesConfig.GREEN_LEAVES);

        Map<String, String> creationRules = new HashMap<>();

        //creationRules.put("1", "mobilityRepetitive");
        creationRules.put("1", "mobilityAbsolute");

        Map<String, Object> config = cloneMap(conf);
        config.put("start", fmt.print(startDate));
        config.put("duration", "7d");
        config.put("exec", execDate.toDate());
        config.put("periodName", "weekly");
        config.put("challengeWeek", getChallengeWeek(execDate, startDate));

        Map<String, String> reward = new HashMap<>();
        reward.put("scoreType", "green leaves");

        List<ChallengeExpandedDTO> chas = api.createStandardSingleChallenges(conf, modelTypes, creationRules, false, config, "all", reward);

        for (ChallengeExpandedDTO cha: chas)
            p(cha);
    }

    private Map<String, Object> cloneMap(Map<String, String> conf) {
        Map<String, Object> new_map = new HashMap<>();
        for (Map.Entry<String, String> entry : conf.entrySet()) {
            new_map.put(entry.getKey(),
                    entry.getValue());
        }
        return new_map;
    }
}
