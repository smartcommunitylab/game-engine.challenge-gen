package eu.trentorise.challenge.rest;

import static eu.trentorise.challenge.PropertiesUtil.CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.GAMEID;
import static eu.trentorise.challenge.PropertiesUtil.HOST;
import static eu.trentorise.challenge.PropertiesUtil.INSERT_CONTEXT;
import static eu.trentorise.challenge.PropertiesUtil.PASSWORD;
import static eu.trentorise.challenge.PropertiesUtil.SAVE_ITINERARY;
import static eu.trentorise.challenge.PropertiesUtil.USERNAME;
import static eu.trentorise.challenge.PropertiesUtil.get;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.ExecutionDataDTO;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.InsertedRuleDto;
import eu.trentorise.game.challenges.util.ConverterUtil;
import eu.trentorise.game.challenges.util.JourneyData;

public class RestTest {

    private GamificationEngineRestFacade facade;
    private GamificationEngineRestFacade insertFacade;

    @Before
    public void setup() {
	facade = new GamificationEngineRestFacade(get(HOST) + get(CONTEXT),
		get(USERNAME), get(PASSWORD));
	insertFacade = new GamificationEngineRestFacade(get(HOST)
		+ get(INSERT_CONTEXT), get(USERNAME), get(PASSWORD));
    }

    @Test
    public void gameReadGameStateTest() {
	List<Content> result = facade.readGameState(get(GAMEID));
	assertTrue(!result.isEmpty());
    }

    @Test
    public void gameInsertRuleTest() {
	// define rule
	InsertedRuleDto rule = new InsertedRuleDto();
	rule.setContent("/* */");
	rule.setName("sampleRule");
	// insert rule
	InsertedRuleDto result = insertFacade.insertGameRule(get(GAMEID), rule);
	assertTrue(!result.getId().isEmpty());
	// delete inserted rule
	boolean res = insertFacade.deleteGameRule(get(GAMEID), result.getId());
	assertTrue(res);
    }

    @Test
    public void saveItineraryTest() {
	GamificationEngineRestFacade facade = new GamificationEngineRestFacade(
		get(HOST) + get(CONTEXT));
	ExecutionDataDTO input = new ExecutionDataDTO();
	input.setActionId(get(SAVE_ITINERARY));
	input.setPlayerId("1");
	input.setGameId(get(GAMEID));
	Map<String, Object> data = new HashMap<String, Object>();
	data.put("bikeDistance", Double.valueOf(1.0d));
	input.setData(data);
	boolean result = facade.saveItinerary(input);

	assertTrue(result);
    }

    @Test
    public void saveUsersItineraryLoadedFromFile() throws IOException {
	// create input
	String ref = "178-bus.json";

	// read all lines from file
	@SuppressWarnings("unchecked")
	List<String> lines = IOUtils.readLines(Thread.currentThread()
		.getContextClassLoader().getResourceAsStream(ref));

	for (String line : lines) {
	    JourneyData jd = ConverterUtil.extractJourneyData(line);
	    ExecutionDataDTO input = new ExecutionDataDTO();
	    input.setActionId(get(SAVE_ITINERARY));
	    input.setPlayerId(jd.getUserId());
	    input.setGameId(get(GAMEID));
	    input.setData(jd.getData());
	    boolean result = facade.saveItinerary(input);

	    assertTrue(result);
	}
    }

    @Test
    public void updateChallengeCustomData() {
	Map<String, Object> customData = new HashMap<String, Object>();
	customData.put("target", "20");
	boolean result = insertFacade.updateChallengeCustomData(get(GAMEID),
		"178", customData);
	assertTrue(result);
	// reset custom data
	// customData.put("target", "");
	// result = insertFacade.updateChallengeCustomData(get(GAMEID), "178",
	// customData);
	// assertTrue(result);
    }

    // For stress test, first configure perf4j in game-engine.core, then see
    // resulting log
    @Test
    public void stressTestRead() throws FileNotFoundException, IOException {
	StringBuffer toWrite = new StringBuffer();

	toWrite.append("N;TIME\n");
	int n = 1000;
	long start = 0;
	long end = 0;
	for (int i = 0; i < n; i++) {
	    toWrite.append(i + ";");
	    start = System.currentTimeMillis();
	    List<Content> result = facade.readGameState(get(GAMEID));
	    end = System.currentTimeMillis();
	    toWrite.append(Math.abs(end - start) + "\n");
	}

	IOUtils.write(toWrite.toString(), new FileOutputStream(
		"stressTestRead.csv"));
    }

    @Test
    public void stressTestInsertAndRead() throws FileNotFoundException,
	    IOException {
	StringBuffer toWrite = new StringBuffer();

	toWrite.append("N;TIME\n");
	int n = 1000;
	long start = 0;
	long end = 0;

	// insert 1000 rules

	// define rule
	List<String> inserted = new ArrayList<String>();
	for (int i = 0; i < n; i++) {
	    InsertedRuleDto rule = new InsertedRuleDto();
	    rule.setContent("/* */");
	    rule.setName("sampleRule" + i);
	    // insert rule
	    InsertedRuleDto result = insertFacade.insertGameRule(get(GAMEID),
		    rule);
	    inserted.add(result.getId());
	}

	// write stress test report
	for (int i = 0; i < n; i++) {
	    toWrite.append(i + ";");
	    start = System.currentTimeMillis();
	    List<Content> result = facade.readGameState(get(GAMEID));
	    end = System.currentTimeMillis();
	    toWrite.append(Math.abs(end - start) + "\n");
	}

	IOUtils.write(toWrite.toString(), new FileOutputStream(
		"stressTestInsertAndRead.csv"));

	// delete all inserted rules
	for (String ruleId : inserted) {
	    insertFacade.deleteGameRule(get(GAMEID), ruleId);
	}
    }

}
