package eu.trentorise.game.challenges;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import eu.trentorise.game.challenges.util.CalendarUtil;
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
	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"dd/MM/YYYY HH:mm:ss , zzz ZZ");

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

	/**
	 * Generate challenges starting from input file
	 * 
	 * @param host
	 * @param gameId
	 * @param input
	 * @param templateDir
	 * @param output
	 * @param username
	 * @param password
	 */
	public static void generate(String host, String gameId, String input,
			String templateDir, String output, String username, String password) {
		// load
		ChallengeRules result;
		try {
			result = ChallengeRulesLoader.load(input);
		} catch (NullPointerException | IllegalArgumentException | IOException e1) {
			String msg = "Error in challenge definition loading for " + input
					+ ": " + e1.getMessage();
			System.err.println(msg);
			return;
		}
		if (result == null) {
			String msg = "Error in loading : " + input;
			System.out.println(msg);
			return;
		}
		System.out.println("Challenge definition file: " + input);
		generate(host, gameId, result, templateDir, output, username, password);
	}

	/**
	 * Generate challenges starting from a {@link ChallengeRules}
	 * 
	 * @param host
	 * @param gameId
	 * @param result
	 * @param templateDir
	 * @param output
	 * @param username
	 * @param password
	 * 
	 * @see ChallengeRulesLoader
	 */
	public static String generate(String host, String gameId,
			ChallengeRules result, String templateDir, String output,
			String username, String password) {
		String log = "";
		// get users from gamification engine
		GamificationEngineRestFacade facade;
		if (username != null && password != null && !username.isEmpty()
				&& !password.isEmpty()) {
			facade = new GamificationEngineRestFacade(host + "gengine/",
					username, password);
		} else {
			facade = new GamificationEngineRestFacade(host + "gengine/");

		}
		String msg = "Contacting gamification engine on host " + host;
		System.out.println(msg);
		log += msg + "\n";
		List<Content> users = null;

		try {
			users = facade.readGameState(gameId);
		} catch (Exception e) {
			msg = "Error in reading game state from host " + host
					+ " for gameId " + gameId;
			System.err.println(msg);
			log += msg + "\n";
			return log;
		}
		if (users == null || users.isEmpty()) {
			msg = "Warning: no users for game " + gameId;
			System.err.println(msg);
			log += msg + "\n";
			return log;
		}
		msg = "Start date "
				+ sdf.format(CalendarUtil.getStart().getTime())
				+ "\n"
				+ "End date "
				+ sdf.format(CalendarUtil.getEnd().getTime())
				+ "\n"
				+ "Reading game from gamification engine game state for gameId: "
				+ gameId + "\n" + "Users in game: " + users.size();
		System.out.println(msg);
		log += msg + "\n";
		ChallengesRulesGenerator crg;
		try {
			crg = new ChallengesRulesGenerator(new ChallengeFactory(),
					"generated-rules-report.csv");
		} catch (IOException e2) {
			msg = "Error in creating " + "generated-rules-report.csv";
			System.err.println(msg);
			log += msg + "\n";
			return log;
		}
		FileOutputStream fout;
		try {
			fout = new FileOutputStream(output);
		} catch (FileNotFoundException e1) {
			msg = "Errore in writing output file " + output;
			System.err.println(msg);
			log += msg + "\n";
			return log;
		}

		// generate challenges
		int tot = 0;
		List<RuleDto> toWrite = new ArrayList<RuleDto>();
		for (ChallengeRuleRow challengeSpec : result.getChallenges()) {
			Matcher matcher = new Matcher(challengeSpec);
			List<Content> filteredUsers = matcher.match(users);
			if (filteredUsers.isEmpty()) {
				msg = "Warning: no users for challenge : "
						+ challengeSpec.getName();
				System.out.println(msg);
				log += msg + "\n";
				continue;
			}
			String res;
			try {
				res = crg.generateRules(challengeSpec, filteredUsers,
						templateDir);
			} catch (UndefinedChallengeException | IOException e) {
				msg = "Error in challenge generation : " + e.getMessage();
				System.err.println(msg);
				log += msg + "\n";
				try {
					crg.closeStream();
					fout.close();
				} catch (IOException e1) {
				}
				return log;
			}
			if (res == null || res.isEmpty()) {
				continue;
			}
			tot++;
			// define rule
			RuleDto rule = new RuleDto();
			rule.setContent(res);
			rule.setName(challengeSpec.getName());
			rule.setCustomData(new HashMap<String, Map<String, Object>>(crg
					.getPlayerIdCustomData()));
			toWrite.add(rule);
		}
		// write result
		// write json
		ObjectMapper mapper = new ObjectMapper();
		try {
			IOUtils.write(mapper.writeValueAsString(toWrite), fout);
		} catch (IOException e) {
			msg = "Error in writing result " + e.getMessage();
			System.err.println(msg);
			log += msg + "\n";
		}
		// close stream
		if (fout != null) {
			try {
				fout.close();
			} catch (IOException e) {
				msg = "Error in closing output file " + output + " "
						+ e.getMessage();
				System.err.println(msg);
				log += msg + "\n";
				return log;
			}
		}
		try {
			crg.closeStream();
		} catch (IOException e) {
			msg = "Error in closing stream file";
			System.err.println(msg);
			log += msg + "\n";
		}
		msg = "Generated rules " + tot + "\n" + "Written output file " + output
				+ "\n" + "Written report file generated-rules-report.csv";
		System.out.println(msg);
		log += msg + "\n";
		return log;
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
