package eu.trentorise.game.challenges;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.game.challenges.exception.UndefinedChallengeException;
import eu.trentorise.game.challenges.rest.Content;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.RuleDto;
import eu.trentorise.game.challenges.util.ChallengeRuleRow;
import eu.trentorise.game.challenges.util.ChallengeRules;
import eu.trentorise.game.challenges.util.ChallengeRulesLoader;
import eu.trentorise.game.challenges.util.Matcher;

/**
 * Command line tool for challenge generation, requires in input</br> -
 * challenge definition in csv format</br> - host where gamification engine is
 * deployed</br> - gameid uuid for game in gamification engine </br> -
 * templateDir challenge templates</br></br> Output: generate a json file with
 * all generated rules</br>
 * 
 */
public class ChallengeGeneratorTool {

	private static Options options;
	private static HelpFormatter helpFormatter;

	public static void main(String[] args) throws ParseException {
		// parse options
		init();
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (MissingOptionException e) {
			printHelp();
			return;
		}
		if (cmd.getOptions() == null || cmd.getOptions().length == 0) {
			printHelp();
			return;
		}
		if (cmd.hasOption("help")) {
			helpFormatter.printHelp("challengeGeneratorTool", options);
			return;
		}
		String host = "";
		String gameId = "";
		String input = "";
		String templateDir = "";
		String output = "challenge.json";
		String username = "";
		String password = "";
		if (cmd.hasOption("host")) {
			host = cmd.getArgList().get(0);
		} else {
			printHelp();
			return;
		}
		if (cmd.hasOption("gameId")) {
			gameId = cmd.getArgList().get(1);
		} else {
			printHelp();
			return;
		}
		if (cmd.hasOption("input")) {
			input = cmd.getArgList().get(2);
		} else {
			printHelp();
			return;
		}
		if (cmd.hasOption("templateDir")) {
			templateDir = cmd.getArgList().get(3);
		} else {
			printHelp();
			return;
		}
		if (cmd.hasOption("output")) {
			output = cmd.getArgList().get(4);
		}
		if (cmd.hasOption("username")) {
			username = cmd.getArgList().get(5);
		}
		if (cmd.hasOption("password")) {
			password = cmd.getArgList().get(6);
		}
		// call generation
		generate(host, gameId, input, templateDir, output, username, password);
	}

	private static void printHelp() {
		helpFormatter
				.printHelp(
						"challengeGeneratorTool",
						"-host <host> -gameId <gameId> -input <input csv file> -template <template directory> [-output output file] [-username -password]",
						options, "");
	}

	private static void generate(String host, String gameId, String input,
			String templateDir, String output, String username, String password) {
		// load
		ChallengeRules result;
		try {
			result = ChallengeRulesLoader.load(input);
		} catch (NullPointerException | IllegalArgumentException | IOException e1) {
			System.err.println("Error in challenge definition loading for "
					+ input + ": " + e1.getMessage());
			return;
		}
		if (result == null) {
			System.out.println("Error in loading : " + input);
			return;
		}
		// get users from gamification engine
		GamificationEngineRestFacade facade;
		if (username != null && password != null && !username.isEmpty()
				&& !password.isEmpty()) {
			facade = new GamificationEngineRestFacade(host + "gengine/",
					username, password);
		} else {
			facade = new GamificationEngineRestFacade(host + "gengine/");

		}
		System.out.println("Contacting gamification engine on host " + host);
		List<Content> users = null;

		try {
			users = facade.readGameState(gameId);
		} catch (Exception e) {
			System.err.println("Error in reading game state from host " + host
					+ " for gameId " + gameId);
			return;
		}
		if (users == null || users.isEmpty()) {
			System.err.println("Warning: no users for game " + gameId);
			return;
		}

		System.out
				.println("Reading game from gamification engine game state for gameId: "
						+ gameId);

		ChallengesRulesGenerator crg;
		try {
			crg = new ChallengesRulesGenerator(new ChallengeFactory(),
					"generated-rules-report.csv");
		} catch (IOException e2) {
			System.err.println("Error in creating "
					+ "generated-rules-report.csv");
			return;
		}
		FileOutputStream fout;
		try {
			fout = new FileOutputStream(output);
		} catch (FileNotFoundException e1) {
			System.err.println("Errore in writing output file " + output);
			return;
		}

		// generate challenges
		int tot = 0;
		List<RuleDto> toWrite = new ArrayList<RuleDto>();
		for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
			Matcher matcher = new Matcher(challengeSpec);
			List<Content> filteredUsers = matcher.match(users);
			if (filteredUsers.isEmpty()) {
				System.out.println("Warning: no users for challenge : "
						+ challengeSpec.getName());
				continue;
			}
			String res;
			try {
				res = crg.generateRules(challengeSpec, filteredUsers,
						templateDir);
			} catch (UndefinedChallengeException | IOException e) {
				System.err.println("Error in challenge generation : "
						+ e.getMessage());
				try {
					crg.closeStream();
					fout.close();
				} catch (IOException e1) {
				}
				return;
			}
			tot++;
			// define rule
			RuleDto rule = new RuleDto();
			rule.setContent(res);
			rule.setName(challengeSpec.getName());
			rule.setCustomData(crg.getPlayerIdCustomData());
			toWrite.add(rule);
		}
		// write result
		// write json
		ObjectMapper mapper = new ObjectMapper();
		try {
			IOUtils.write(mapper.writeValueAsString(toWrite), fout);
		} catch (IOException e) {
			System.err.println("Error in writing result " + e.getMessage());
		}
		// close stream
		if (fout != null) {
			try {
				fout.close();
			} catch (IOException e) {
				System.err.println("Error in closing output file " + output
						+ " " + e.getMessage());
				return;
			}
		}
		try {
			crg.closeStream();
		} catch (IOException e) {
			System.err.println("Error in closing stream file");
		}
		System.out.println("Generated rules: " + tot);
		System.out.println("Written output file " + output);
		System.out.println("Written report file generated-rules-report.csv");

	}

	private static void init() {
		options = new Options();
		options.addOption(Option.builder("help").desc("display this help")
				.build());
		options.addOption(Option.builder("host")
				.desc("gamification engine host").build());
		options.addOption(Option.builder("gameId")
				.desc("uuid for gamification engine").build());
		options.addOption(Option.builder("input")
				.desc("challenge definition as csv file").required().build());
		options.addOption(Option.builder("templateDir")
				.desc("challenges templates").build());
		options.addOption(Option.builder("output")
				.desc("generated file name, default challenge.json").build());
		options.addOption(Option.builder("username")
				.desc("username for gamification engine").build());
		options.addOption(Option.builder("password")
				.desc("password for gamification engine").build());
		helpFormatter = new HelpFormatter();
	}

}
