package eu.fbk.das.old;

import eu.fbk.das.GamificationEngineRestFacade;
import it.smartcommunitylab.model.PlayerStateDTO;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Command line tool for challenge generation, requires in input</br> -
 * challenge definition in csv format</br> - host where gamification engine is
 * deployed</br> - gameid uuid for game in gamification engine </br> -
 * templateDir challenge templates</br></br> Output: generate a json file with
 * all generated rules</br>
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
        String output = "challenge.json";
        String username = "";
        String password = "";
        String filterIds = "";
        Boolean useRecommendationSystem = Boolean.FALSE;
        Boolean useFiltering = Boolean.FALSE;
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
        if (cmd.hasOption("output")) {
            output = cmd.getArgList().get(3);
        }
        if (cmd.hasOption("username")) {
            username = cmd.getArgList().get(4);
        }
        if (cmd.hasOption("password")) {
            password = cmd.getArgList().get(5);
        }
        if (cmd.hasOption("useRecommendationSystem")) {
            useRecommendationSystem = Boolean.valueOf(cmd.getArgList().get(6));
        }
        if (cmd.hasOption("enableFiltering")) {
            useFiltering = Boolean.valueOf(cmd.getArgList().get(7));
        }
        if (cmd.hasOption("filterIds")) {
            filterIds = cmd.getArgList().get(8);
        }
        // call generation
        generate(host, gameId, input, output, username, password, filterIds, useFiltering);
    }

    private static void printHelp() {
        helpFormatter
                .printHelp(
                        "challengeGeneratorTool",
                        "-host <host> -gameId <gameId> -input <input csv file> -template <template directory> [-output output file] [-username -password] -useRecommendationSystem <true/false> -filterIds <list of filter ids comma separated>",
                        options, "");
    }

    /**
     * Generate challenges starting from input file
     */
    public static void generate(String host, String gameId, String input,
                                String output, String username, String password, String filterIds, Boolean useFiltering) {
        // load
        ChallengeRules challengeDefinition;
        try {
            challengeDefinition = ChallengeRulesLoader.load(input);
        } catch (NullPointerException | IllegalArgumentException | IOException e1) {
            String msg = "Error in challenge definition loading for " + input
                    + ": " + e1.getMessage();
            System.err.println(msg);
            return;
        }
        if (challengeDefinition == null) {
            String msg = "Error in loading : " + input;
            System.out.println(msg);
            return;
        }
        System.out.println("Challenge definition file: " + input);
        generate(host, gameId, challengeDefinition, username, password, output,
                filterIds, useFiltering);
    }

    /**
     * Generate challenges starting from a {@link ChallengeRules}
     *
     * @see ChallengeRulesLoader
     */
    public static String generate(String host, String gameId,
                                  ChallengeRules challengeDefinitions, String username,
                                  String password, String output, String filterIds, Boolean useFiltering) {
        String log = "";
        // get users from gamification engine
        GamificationEngineRestFacade facade;

            facade = new GamificationEngineRestFacade(host + "gengine/",
                    username, password);

        String msg = "Contacting gamification engine on host " + host;
        System.out.println(msg);
        log += msg + Constants.LINE_SEPARATOR;
        Map<String, PlayerStateDTO> m_users = null;

        List<PlayerStateDTO> users = null;

        try {
            m_users = facade.readGameState(gameId);
            for (String pId: m_users.keySet())
                users.add(m_users.get(pId));
        } catch (Exception e) {
            msg = "Error in reading game state from host " + host
                    + " for gameId " + gameId + " error: " + e.getMessage();
            System.err.println(msg);
            log += msg + Constants.LINE_SEPARATOR;
            return log;
        }
        if (users == null || users.isEmpty()) {
            msg = "Warning: no users for game " + gameId;
            System.err.println(msg);
            log += msg + Constants.LINE_SEPARATOR;
            return log;
        }
        msg = "Start date "
                + sdf.format(CalendarUtil.getStart().getTime())
                + Constants.LINE_SEPARATOR
                + "End date "
                + sdf.format(CalendarUtil.getEnd().getTime())
                + Constants.LINE_SEPARATOR
                + "Reading game from gamification engine game state for gameId: "
                + gameId + Constants.LINE_SEPARATOR + "Users in game: "
                + users.size();
        System.out.println(msg);
        log += msg + Constants.LINE_SEPARATOR;
        ChallengesRulesGenerator crg;
        try {
            crg = new ChallengesRulesGenerator(new ChallengeInstanceFactory(),
                    "generated-rules-report.csv", output);
        } catch (IOException e2) {
            msg = "Error in creating " + "generated-rules-report.csv";
            System.err.println(msg);
            log += msg + Constants.LINE_SEPARATOR;
            return log;
        }

        // generate challenges
        for (ChallengeRuleRow challengeSpec : challengeDefinitions
                .getChallenges()) {
            Matcher matcher = new Matcher(challengeSpec);
            List<PlayerStateDTO> filteredUsers = matcher.match(users);
            if (filteredUsers.isEmpty()) {
                msg = "Warning: no users for challenge : "
                        + challengeSpec.getName();
                System.out.println(msg);
                log += msg + Constants.LINE_SEPARATOR;
                continue;
            }
            try {
                crg.generateChallenges(challengeSpec, filteredUsers,
                        CalendarUtil.getStart().getTime(), CalendarUtil
                                .getEnd().getTime());
            } catch (IOException e) {
                msg = "Error in challenge generation : " + e.getMessage();
                System.err.println(msg);
                log += msg + Constants.LINE_SEPARATOR;
                return log;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            crg.writeChallengesToFile();
            msg = "Challenges generated and written report file generated-rules-report.csv , "
                    + output + " ready to be uploaded";
            System.out.println(msg);
            log += msg + Constants.LINE_SEPARATOR;
            return log;
        } catch (IOException e) {
            msg = "Error in writing challenges to file";
            log += msg + Constants.LINE_SEPARATOR;
            return log;
        }
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

    public static String generate(String host, String gameId,
                                  ChallengeRules challenges, String username, String password,
                                  String output, Date startDate, Date endDate, String filterIds,
                                  Boolean useFiltering) {
        if (host == null || gameId == null || challenges == null
                || username == null || password == null || output == null
                || startDate == null || endDate == null || filterIds == null
        ) {
            throw new IllegalArgumentException("inputs must be not null");
        }
        CalendarUtil.setStart(startDate);
        CalendarUtil.setEnd(endDate);
        return generate(host, gameId, challenges, username, password, output,
                filterIds, useFiltering);
    }
}
