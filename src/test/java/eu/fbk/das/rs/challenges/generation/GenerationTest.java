package eu.fbk.das.rs.challenges.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Sets;
import eu.fbk.das.rs.challenges.ChallengesBaseTest;
import eu.trentorise.game.challenges.ChallengeFactory;
import eu.trentorise.game.challenges.ChallengeInstanceFactory;
import eu.trentorise.game.challenges.ChallengesRulesGenerator;
import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.model.ChallengeDataDTO;
import eu.trentorise.game.challenges.model.ChallengeDataInternalDto;
import eu.trentorise.game.challenges.model.ChallengeModel;
import eu.trentorise.game.challenges.model.ChallengeType;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static eu.fbk.das.rs.Utils.dbg;
import static org.junit.Assert.assertTrue;

public class GenerationTest extends ChallengesBaseTest {

    private static final Logger logger = LogManager.getLogger(GenerationTest.class);

    private GamificationEngineRestFacade facade;
    private GamificationEngineRestFacade insertFacade;
    private GamificationEngineRestFacade challengeModelFacade;
    private GamificationEngineRestFacade challengeAssignFacade;
    private GamificationEngineRestFacade challengeModelReadFacade;

    @Before
    public void setup() {
        facade = new GamificationEngineRestFacade(HOST + CONTEXT, USERNAME,
                PASSWORD);
        insertFacade = new GamificationEngineRestFacade(HOST + INSERT_CONTEXT,
                USERNAME, PASSWORD);
        challengeModelFacade =
                new GamificationEngineRestFacade(HOST, USERNAME, PASSWORD);
        challengeAssignFacade = new GamificationEngineRestFacade(HOST + "data/game/",
                USERNAME, PASSWORD);
        challengeModelReadFacade = new GamificationEngineRestFacade(HOST + "model/game/",
                USERNAME, PASSWORD);
    }

    @Test
    public void loadChallengeRuleGenerate()
            throws NullPointerException, IllegalArgumentException, IOException {
        // load
        ChallengeRules result = ChallengeRulesLoader.load("w1_challenges.csv");

        assertTrue(result != null && !result.getChallenges().isEmpty());

        Map<String, Content> m_users = facade.readGameState(GAMEID);
        List<Content> users = new ArrayList<Content>();
        for (String pId: m_users.keySet())
            users.add(m_users.get(pId));


        System.out.println("Users found: " + users.size());

        // generate challenges
        for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
            Matcher matcher = new Matcher(challengeSpec);
            List<Content> r = matcher.match(users);
            assertTrue(r != null);
            System.out.println("Users match: " + r.size());
        }
    }

    @Test
    public void loadTestGeneration() throws NullPointerException, IllegalArgumentException,
            IOException, UndefinedChallengeException {
        // load
        ChallengeRules result = ChallengeRulesLoader.load("w1_challenges.csv");

        assertTrue(result != null && !result.getChallenges().isEmpty());

        // get users from gamification engine
        Map<String, Content> m_users = facade.readGameState(GAMEID);
        List<Content> users = new ArrayList<Content>();
        for (String pId: m_users.keySet())
            users.add(m_users.get(pId));

        ChallengesRulesGenerator crg = new ChallengesRulesGenerator(new ChallengeInstanceFactory(),
                "generated-rules-report.csv", "output.json");

        // generate challenges
        for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
            dbg(logger, "rules generation for challenge: " + challengeSpec.getName());
            Matcher matcher = new Matcher(challengeSpec);
            List<Content> filteredUsers = matcher.match(users);
            dbg(logger, "found users: " + filteredUsers.size());
            // generate rule
            if (!filteredUsers.isEmpty()) {
                crg.generateChallenges(challengeSpec, filteredUsers,
                        CalendarUtil.getStart().getTime(), CalendarUtil.getEnd().getTime());
            }
        }
        crg.writeChallengesToFile();
    }

    @Test
    public void loadAndUploadTest() throws NullPointerException, IllegalArgumentException,
            IOException, UndefinedChallengeException {
        // load
        ChallengeRules result = ChallengeRulesLoader.load("w1_challenges.csv");

        assertTrue(result != null && !result.getChallenges().isEmpty());

        // get users from gamification engine
        Map<String, Content> m_users = facade.readGameState(GAMEID);
        List<Content> users = new ArrayList<Content>();
        for (String pId: m_users.keySet())
            users.add(m_users.get(pId));

        ChallengesRulesGenerator crg = new ChallengesRulesGenerator(new ChallengeInstanceFactory(),
                "generated-rules-report.csv", "output.json");

        // generate challenges
        for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
            dbg(logger, "rules generation for challenge: " + challengeSpec.getName());
            Matcher matcher = new Matcher(challengeSpec);
            List<Content> filteredUsers = matcher.match(users);
            dbg(logger, "found users: " + filteredUsers.size());
            // generate rule
            if (!filteredUsers.isEmpty()) {
                crg.generateChallenges(challengeSpec, filteredUsers,
                        CalendarUtil.getStart().getTime(), CalendarUtil.getEnd().getTime());
            }
        }
        crg.writeChallengesToFile();

        // upload
        String input = "output.json";
        System.out.println("Uploading for file " + input);
        // read input file
        ObjectMapper mapper = new ObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        String jsonString = IOUtils.toString(new FileInputStream(input));
        List<ChallengeDataInternalDto> challenges = mapper.readValue(jsonString,
                typeFactory.constructCollectionType(List.class, ChallengeDataInternalDto.class));
        System.out.println("Read challenges " + challenges.size());

        int tot = 0;
        StringBuffer buffer = new StringBuffer();
        buffer.append("CHALLENGE_NAME;CHALLENGE_MODEL;CHALLENGE_UUID;PLAYER_ID\n");

        boolean r = false;
        for (ChallengeDataInternalDto ch : challenges) {
            // upload and assign challenge
            tot++;
            r = challengeAssignFacade.assignChallengeToPlayer(ch.getDto(), ch.getGameId(),
                    ch.getPlayerId());
            if (!r) {
                System.out.println(
                        "Error in uploading challenge instance " + ch.getDto().getInstanceName());
                return;
            } else {
                System.out.println("Inserted challenge with Id " + ch.getDto().getInstanceName());
                buffer.append(ch.getDto().getInstanceName() + ";");
                buffer.append(ch.getDto().getModelName() + ";");
                buffer.append(ch.getDto().getInstanceName() + ";");
                buffer.append(ch.getPlayerId() + ";\n");
            }
        }
        try {
            IOUtils.write(buffer.toString(), new FileOutputStream("report.csv"));
        } catch (IOException e) {
            System.err.println("Error in writing report.csv file");
            return;
        }
        System.out.println("Inserted challenge: " + tot + "\n" + "Challenge upload completed");
        assertTrue(tot != 0);
    }

    @Test
    public void createAbsoluteIncrementZeroImpactTripChallenge() {
        LocalDate now = new LocalDate();

        ChallengeDataDTO cdd = new ChallengeDataDTO();
        cdd.setModelName("absoluteIncrement");
        cdd.setInstanceName("tripNumber_" + UUID.randomUUID());
        cdd.setStart(now.dayOfMonth().addToCopy(-10).toDate());
        cdd.setEnd(now.dayOfMonth().addToCopy(5).toDate());
        
        cdd.setData("target", 2d);
        cdd.setData("bonusPointType", "green leaves");
        cdd.setData("bonusScore", 100d);
        cdd.setData("periodName", "weekly");
        cdd.setData("counterName", "ZeroImpact_Trips");

        
        assertTrue(challengeAssignFacade.assignChallengeToPlayer(cdd, GAMEID, "1"));
    }

    @Test
    public void createLeaderboardPositionChallengeInstance() {
        // Test related to rule
        // https://github.com/smartcommunitylab/smartcampus.gamification/blob/r2.0.0/game-engine.test/src/test/resources/rules/challengeTest/zeroimpactChallenge.drl

        LocalDate now = new LocalDate();

        ChallengeDataDTO cdd = new ChallengeDataDTO();
        cdd.setModelName("leaderboardPosition");
        cdd.setInstanceName("InstanceName" + UUID.randomUUID());
        cdd.setStart(now.dayOfMonth().addToCopy(-2).toDate());
        cdd.setEnd(now.dayOfMonth().addToCopy(6).toDate());
        
        cdd.setData("posMax", 3);
        cdd.setData("posMin", 2);
        cdd.setData("bonusPointType", "green leaves");
        cdd.setData("bonusScore", 500d);
        cdd.setData("weekClassificationName", "week classification test");

        
        assertTrue(challengeAssignFacade.assignChallengeToPlayer(cdd, GAMEID, "4"));
    }

    @Test
    public void createEventChallengeTest() {
        // Test related to rule
        // https://github.com/smartcommunitylab/smartcampus.gamification/blob/r2.0.0/game-engine.test/src/test/resources/rules/challengeTest/zeroimpactChallenge.drl

        LocalDate now = new LocalDate();

        ChallengeDataDTO cdd = new ChallengeDataDTO();
        cdd.setModelName("leaderboardPosition");
        cdd.setInstanceName("InstanceName" + UUID.randomUUID());
        cdd.setStart(now.dayOfMonth().addToCopy(-2).toDate());
        cdd.setEnd(now.dayOfMonth().addToCopy(6).toDate());
        
        cdd.setData("posMax", 3);
        cdd.setData("posMin", 2);
        cdd.setData("bonusPointType", "green leaves");
        cdd.setData("bonusScore", 500d);
        cdd.setData("weekClassificationName", "week classification test");

        
        assertTrue(challengeAssignFacade.assignChallengeToPlayer(cdd, GAMEID, "23897"));
    }

    @Test
    public void readAllChallengeModelTest() {
        HashSet result = (HashSet) challengeModelReadFacade.readChallengesModel(GAMEID);
        assertTrue(result != null);
        List<ChallengeModel> models = new ArrayList<ChallengeModel>();
        Iterator iter = result.iterator();
        while (iter.hasNext()) {
            LinkedHashMap<String, Object> m = (LinkedHashMap<String, Object>) iter.next();
            String id = (String) m.get("id");
            String name = (String) m.get("name");
            List<String> variables = (List<String>) m.get("variables");
            String gameId = (String) m.get("gameId");
            ChallengeModel model = new ChallengeModel();
            model.setId(id);
            model.setName(name);
            model.setVariables(Sets.newHashSet(variables));
            model.setGameId(gameId);
            models.add(model);
            System.out.println(name);
        }
        System.out.println("read models " + models.size());
        assertTrue(!models.isEmpty());
    }

    @Test
    public void createPoiCheckinChallengeTest() {
        LocalDate now = new LocalDate();

        ChallengeDataDTO cdd = new ChallengeDataDTO();
        cdd.setModelName("poiCheckin");
        cdd.setInstanceName("InstanceName" + UUID.randomUUID());
        cdd.setStart(now.dayOfMonth().addToCopy(-2).toDate());
        cdd.setEnd(now.dayOfMonth().addToCopy(6).toDate());
        
        cdd.setData("poiName", "Trento Fiera");
        cdd.setData("eventName", "Fai la cosa giusta");
        cdd.setData("poiState", Boolean.FALSE);
        cdd.setData("eventState", Boolean.FALSE);
        cdd.setData("bonusScore", 200d);
        cdd.setData("bonusPointType", "green leaves");

        
        assertTrue(challengeAssignFacade.assignChallengeToPlayer(cdd, GAMEID, "24607"));
    }

    @Test(expected = UndefinedChallengeException.class)
    public void challengeFactoryTest() throws UndefinedChallengeException {
        ChallengeFactory cf = new ChallengeFactory();
        cf.createChallenge(ChallengeType.NOOP, "");
    }

}
