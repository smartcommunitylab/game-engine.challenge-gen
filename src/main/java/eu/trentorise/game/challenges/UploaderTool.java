package eu.trentorise.game.challenges;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.ProcessingException;

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
import com.fasterxml.jackson.databind.type.TypeFactory;

import eu.trentorise.game.challenges.model.ChallengeDataInternalDto;
import eu.trentorise.game.challenges.rest.GamificationEngineRestFacade;

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
		GamificationEngineRestFacade challengeAssignFacade;
		if (username != null && password != null && !username.isEmpty()
				&& !password.isEmpty()) {
			challengeAssignFacade = new GamificationEngineRestFacade(host
					+ "data/game/", username, password);
		} else {
			challengeAssignFacade = new GamificationEngineRestFacade(host
					+ "data/game/");
		}
		msg = "Uploading on host " + host + " for gameId " + gameId
				+ " for file " + input;
		System.out.println(msg);
		log += msg + "\n";
		// read input file
		ObjectMapper mapper = new ObjectMapper();
		TypeFactory typeFactory = mapper.getTypeFactory();
		String jsonString;
		List<ChallengeDataInternalDto> challenges = null;
		try {
			jsonString = IOUtils.toString(new FileInputStream(input));
			challenges = mapper.readValue(jsonString, typeFactory
					.constructCollectionType(List.class,
							ChallengeDataInternalDto.class));

		} catch (IOException e1) {
			msg = "Error in reading input file for uploader " + input;
			log += msg + "\n";
			return log;
		}
		msg = "Read challenges " + challenges.size();

		int tot = 0;
		StringBuffer buffer = new StringBuffer();
		buffer.append("CHALLENGE_NAME;CHALLENGE_UUID;RULE_TEXT\n");
		msg = "Read challenges " + challenges.size();
		System.out.println(msg);
		log += msg + "\n";
		boolean r = false;
		for (ChallengeDataInternalDto ch : challenges) {
			// upload and assign challenge
			tot++;
			try {
				r = challengeAssignFacade.assignChallengeToPlayer(ch.getDto(),
						ch.getGameId(), ch.getPlayerId());
			} catch (ProcessingException e) {
				msg = "Error on uploading challenge on gamification engine "
						+ e.getMessage();
				System.out.println(msg);
				log += msg + "\n";
				return null;
			}
			if (!r) {
				msg = "Error in uploading challenge instance "
						+ ch.getDto().getInstanceName();
				System.out.println(msg);
				log += msg + "\n";
				return null;
			} else {
				System.out.println("Inserted challenge with Id "
						+ ch.getDto().getInstanceName());
				buffer.append(ch.getDto().getInstanceName() + ";");
				buffer.append(ch.getDto().getModelName() + ";");
				buffer.append(ch.getDto().getInstanceName() + ";");
				buffer.append(ch.getPlayerId() + ";");
			}
		}
		try {
			IOUtils.write(buffer, new FileOutputStream("report.csv"));
		} catch (IOException e) {
			msg = "Error in writing report.csv file";
			System.out.println(msg);
			log += msg + "\n";
			return null;
		}
		msg = "Inserted challenges " + tot + "\n"
				+ "Challenges upload completed";
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
