import eu.fbk.das.old.ChallengeGeneratorTool;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AnalysisCmd {

    private static final Logger logger = Logger.getLogger(AnalysisCmd.class);

    private static Options options;
    private static HelpFormatter helpFormatter;
    private static final String[] COLUMNS = new String[]{"PLAYER_ID",
            "SCORE_GREEN_LEAVES", "bike_km", "bike_trips", "bikesharing_km",
            "bikesharing_trips", "bus_km", "bus_trips", "car_km", "car_trips",
            "train_km", "train_trips", "walk_km", "walk_trips",
            "zero_impact_trips", "CHALLENGES_SUCCESS", "CHALLENGES_TOTAL"};

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
        String first = "";
        String second = "";
        if (cmd.hasOption("first")) {
            first = cmd.getArgList().get(0);
        } else {
            printHelp();
            return;
        }

        if (cmd.hasOption("second")) {
            second = cmd.getArgList().get(1);
        } else {
            printHelp();
            return;
        }
        buildDifferences(first, second);
    }

    private static void buildDifferences(String first, String second) {
        Map<String, Properties> firstData = new HashMap<String, Properties>();
        Map<String, Properties> secondData = new HashMap<String, Properties>();
        Map<String, Properties> resultData = new HashMap<String, Properties>();

        try {
            firstData = load(first);
            secondData = load(second);
        } catch (NullPointerException | IllegalArgumentException | IOException e) {
            logger.error(e);
        }

        for (String secondKey : secondData.keySet()) {
            if (firstData.containsKey(secondKey)) {
                resultData.put(
                        secondKey,
                        propDiff(firstData.get(secondKey),
                                secondData.get(secondKey)));
            } else {
                resultData.put(secondKey, new Properties());
            }
        }

        // write result to csv

        FileOutputStream fout;
        try {
            fout = new FileOutputStream("diff.csv");
            IOUtils.write(StringUtils.join(COLUMNS, ";") + "\n", fout);

            StringBuffer sb = new StringBuffer();
            for (String key : resultData.keySet()) {
                Properties data = resultData.get(key);
                boolean start = true;
                for (String column : COLUMNS) {
                    if (start) {
                        start = false;
                        sb.append(key + ";");
                        continue;
                    }
                    if (data.containsKey(column)) {
                        sb.append(data.get(column) + ";");
                    } else {
                        sb.append(secondData.get(key).get(column) + ";");
                    }
                }
                sb.append("\n");
            }
            IOUtils.write(sb.toString(), fout);

        } catch (IOException e) {
            logger.error(e);
        }

    }

    private static Properties propDiff(Properties first, Properties second) {
        Properties result = new Properties();
        boolean start = true;
        for (String column : COLUMNS) {
            if (start) {
                start = false;
                continue;
            }
            Double s = 0.0;
            Double f = 0.0;
            if (second.get(column) != null
                    && !second.get(column).equals("null")) {
                s = Double.valueOf((String) second.get(column));
            }
            if (first.get(column) != null && !first.get(column).equals("null")) {
                f = Double.valueOf((String) first.get(column));
            }
            result.put(column, s - f);
        }
        return result;
    }

    private static void printHelp() {
        helpFormatter.printHelp("AnalysisCmd",
                "-first <first csv> -second <second csv>", options, "");
    }

    private static void init() {
        options = new Options();
        options.addOption(Option.builder("help").desc("display this help")
                .build());
        options.addOption(Option.builder("first").desc("first file to compare")
                .required().build());
        options.addOption(Option.builder("second")
                .desc("second file to compare").required().build());
        helpFormatter = new HelpFormatter();
    }

    public static Map<String, Properties> load(String ref) throws IOException,
            NullPointerException, IllegalArgumentException {
        if (ref == null) {
            System.err.println("Input file must be not null");
            throw new NullPointerException("Input file must be not null");
        }
        if (!ref.endsWith(".csv")) {
            System.err.println("challenges rules file must be a csv file");
            throw new IllegalArgumentException(
                    "challenges rules file must be a csv file");
        }
        BufferedReader rdr = null;
        try {

            try {
                // open csv file
                rdr = new BufferedReader(new StringReader(
                        IOUtils.toString(Thread.currentThread()
                                .getContextClassLoader()
                                .getResourceAsStream(ref))));
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return null;
            } catch (NumberFormatException e) {
                System.err.println(e.getMessage());
                return null;
            } catch (NullPointerException npe) {
                rdr = new BufferedReader(new FileReader(ref));
            }
            Map<String, Properties> response = new HashMap<String, Properties>();
            boolean first = true;
            for (String line = rdr.readLine(); line != null; line = rdr
                    .readLine()) {
                if (first) {
                    first = false;
                    continue;
                }
                String[] elements = line.split(";");
                int i = 0;
                Properties prop = new Properties();
                for (String column : COLUMNS) {
                    prop.put(column, elements[i]);
                    i++;
                }
                response.put(elements[0], prop);

            }
            return response;
        } finally {
            if (rdr != null) {
                rdr.close();
            }
        }
    }

}
