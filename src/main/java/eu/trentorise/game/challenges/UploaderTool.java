package eu.trentorise.game.challenges;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.trentorise.game.challenges.api.Constants;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;
import eu.trentorise.game.challenges.rest.InsertedRuleDto;
import eu.trentorise.game.challenges.rest.RuleDto;

/**
 * Uploader tool get a json file created from {@link ChallengeGeneratorTool} and
 * upload into Gamification engine using {@link GamificationEngineRestFacade}.
 * Options: </br></br> - input json input file </br> - host host for
 * gamification engine</br> - gameId unique indentifier for game into
 * gamification engine
 */
public class UploaderTool {

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
		if (cmd.hasOption("username")) {
			username = cmd.getArgList().get(3);
		}
		if (cmd.hasOption("password")) {
			password = cmd.getArgList().get(4);
		}
		// call generation
		upload(host, gameId, input, username, password);
	}

	private static void printHelp() {
		helpFormatter
				.printHelp(
						"challengeUploader",
						"-host <host> -gameId <gameId> -input <input json file> [-username -password ] ",
						options, "");
	}

	public static String upload(String host, String gameId, String input,
			String username, String password) {
		String log = "";
		String msg = "";
		if (input == null) {
			msg = "Input file cannot be null";
			log += msg + "\n";
			System.err.println(msg);
			return log;
		}
		GamificationEngineRestFacade insertFacade;
		if (username != null && password != null && !username.isEmpty()
				&& !password.isEmpty()) {
			insertFacade = new GamificationEngineRestFacade(host + "console/",
					username, password);
		} else {
			insertFacade = new GamificationEngineRestFacade(host + "console/");
		}
		msg = "Uploading on host " + host + " for gameId " + gameId
				+ " for file " + input;
		System.out.println(msg);
		log += msg + "\n";
		// read input file
		ObjectMapper mapper = new ObjectMapper();
		List<RuleDto> rules = null;
		try {
			String jsonString = IOUtils.toString(new FileInputStream(input));
			rules = mapper.readValue(jsonString,
					new TypeReference<List<RuleDto>>() {
					});
		} catch (IOException e) {
			msg = "Error in reading input file " + input + " " + e.getMessage();
			System.err.println(msg);
			log += msg + "\n";
			return log;
		}
		int tot = 0;
		StringBuffer buffer = new StringBuffer();
		buffer.append("CHALLENGE_NAME;CHALLENGE_UUID;RULE_TEXT\n");
		msg = "Read rules " + rules.size();
		System.out.println(msg);
		log += msg + "\n";
		for (RuleDto rule : rules) {
			// update custom data for every user related to genrate rule
			for (String userId : rule.getCustomData().keySet()) {
				insertFacade.updateChallengeCustomData(gameId, userId, rule
						.getCustomData().get(userId));
			}

			// insert rule
			InsertedRuleDto toInsert = new InsertedRuleDto(rule);
			InsertedRuleDto insertedRule = insertFacade.insertGameRule(gameId,
					toInsert);
			if (insertedRule != null) {
				String ruleId = StringUtils.removeStart(insertedRule.getId(),
						Constants.RULE_PREFIX);
				msg = "Uploaded rule " + insertedRule.getName() + " with id="
						+ ruleId;
				System.out.println(msg);
				log += msg + "\n";
				buffer.append(insertedRule.getName() + ";");
				buffer.append(ruleId + ";");
				buffer.append("test;\n");
				tot++;
			} else {
				msg = "Error in uploaded rule " + rule.getName();
				System.err.println(msg);
				log += msg + "\n";
			}
			System.out.println();
		}
		try {
			IOUtils.write(buffer, new FileOutputStream("report.csv"));
		} catch (IOException e) {
			msg = "Error in writing report file";
			System.err.println(msg);
			log += msg + "\n";
			return log;
		}
		msg = "Inserted rules " + tot + "\n" + "Rule upload completed";
		System.out.println(msg);
		log += msg + "\n";
		return log;
	}

	private static void init() {
		options = new Options();
		options.addOption(Option.builder("help").desc("display this help")
				.build());
		options.addOption(Option.builder("host")
				.desc("gamification engine host").required().build());
		options.addOption(Option.builder("gameId")
				.desc("uuid for gamification engine").required().build());
		options.addOption(Option.builder("input")
				.desc("rules to upload in json format").required().build());
		options.addOption(Option.builder("username")
				.desc("username for gamification engine").build());
		options.addOption(Option.builder("password")
				.desc("password for gamification engine").build());
		helpFormatter = new HelpFormatter();
	}

}
