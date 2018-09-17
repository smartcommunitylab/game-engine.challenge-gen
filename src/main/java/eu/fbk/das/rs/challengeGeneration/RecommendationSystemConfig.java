package eu.fbk.das.rs.challengeGeneration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static eu.fbk.das.rs.Utils.logExp;

/**
 * RecommendationSystem configuration main class
 */
public class RecommendationSystemConfig {


    /**
     * --------------------------------------
     * main configuration
     * --------------------------------------
     */

    private static final Logger logger = LogManager.getLogger(RecommendationSystemConfig.class);

    public static final String RS_PROPERTIES = "rs.properties";

    private static Properties prop;

    /**
     * --------------------------------------
     * default configuration
     * --------------------------------------
     */

    public static String gLeaves = "green leaves";

    // default modes
    private static final String NO_CAR_TRIPS = "NoCar_Trips";
    private static final String BIKE_KM = "Bike_Km";
    private static final String BUS_KM = "Bus_Km";
    private static final String BIKE_SHARING_TRIPS = "BikeSharing_Trips";
    private static final String WALK_TRIPS = "Walk_Trips";
    private static final String BIKE_TRIPS = "Bike_Trips";
    private static final String TRAIN_KM = "Train_Km";
    private static final String BUS_TRIPS = "Bus_Trips";
    private static final String TRAIN_TRIPS = "Train_Trips";
    private static final String ZERO_IMPACT_TRIPS = "ZeroImpact_Trips";
    private static final String BIKE_SHARING_KM = "BikeSharing_Km";
    private static final String WALK_KM = "Walk_Km";

    // Default prize matrix dimension, number of rows
    public static final int PRIZE_MATRIX_NROW = 4;

    // Default prize matrix dimension, number of columns
    public static final int PRIZE_MATRIX_NCOL = 10;

    // Default prize matrix coordinate for try once, number of row
    public static final int PRIZE_MATRIX_TRY_ONCE_ROW_INDEX = 1;

    // Default prize matrix coordinate for try once, number of column
    public static final int PRIZE_MATRIX_TRY_ONCE_COL_INDEX = 9;

    // Prize matrix approximator value, @see {@link PlanePointFunction}
    public static final Double PRIZE_MATRIX_APPROXIMATOR = 10.0;

    /**
     * --------------------------------------
     * dynamic configuration
     * --------------------------------------
     */

    // Enable default users filtering
    private boolean userFiltering = false;

    // Enable select top 2 challenges
    private static final boolean selectTopTwo = true;

    // Transportation mode configuration
    // First, declare call supported modes. Order matters!
    private final String[] defaultMode = {BIKE_KM, WALK_KM, BUS_TRIPS, TRAIN_TRIPS};
    // Second, declare corresponding *_Trips of *_Km modes (i.e. Walk_km =>
    // Walk_Trips), used for try once challenges
    private final String[] defaultModetrip = {BIKE_TRIPS, WALK_TRIPS,
            BUS_TRIPS, TRAIN_TRIPS, BIKE_SHARING_TRIPS};
    // "Walk_Trips", "Bike_Trips", "BikeSharing_Trips"
    // defining different improvement percentage 10%,20%, etc.
    private final Double[] percentage = {0.1, 0.2, 0.3, 0.5, 1.0};

    // default user player id
    private List<String> playerIds;

    // recommendation system configuration
    private RecommendationSystemModeConfiguration modeConfiguration;
    private String challengeNamePrefix = "w12_rs_";

    public RecommendationSystemConfig() {

        prop = new Properties();
        try {
            prop.load(getClass().getClassLoader().getResourceAsStream(RS_PROPERTIES));
        } catch (IOException e) {
            logExp(logger, e);
        }

        init();
    }

    // init recommendation system configuration
    private void init() {

        modeConfiguration = new RecommendationSystemModeConfiguration();

        modeConfiguration.put(BIKE_KM, new SingleModeConfig(BIKE_KM, 8, 220.0,
                380.0, 300.0));
        modeConfiguration.put(WALK_KM, new SingleModeConfig(WALK_KM, 10, 200.0,
                380.0, 280.0));

        modeConfiguration.put(BUS_TRIPS, new SingleModeConfig(BUS_TRIPS, 10,
                200.0, 380.0, 280.0));
        modeConfiguration.put(TRAIN_TRIPS, new SingleModeConfig(TRAIN_TRIPS,
                10, 200.0, 380.0, 280.0));

        modeConfiguration.put(BIKE_SHARING_TRIPS, new SingleModeConfig(
                BIKE_SHARING_TRIPS, 10, 220.0, 380.0, 300.0));

        modeConfiguration.put(TRAIN_KM, new SingleModeConfig(TRAIN_KM, 0,
                150.0, 350.0, 240.0));
        modeConfiguration.put(BUS_KM, new SingleModeConfig(BUS_KM, 0, 150.0,
                350.0, 240.0));
        modeConfiguration.put(NO_CAR_TRIPS, new SingleModeConfig(NO_CAR_TRIPS,
                0, 100.0, 250.0, 150.0));
        modeConfiguration.put(BIKE_SHARING_KM, new SingleModeConfig(
                BIKE_SHARING_KM, 0, 200.0, 300.0, 250.0));
        modeConfiguration.put(WALK_TRIPS, new SingleModeConfig(WALK_TRIPS, 0,
                150.0, 250.0, 200.0));
        modeConfiguration.put(BIKE_TRIPS, new SingleModeConfig(BIKE_TRIPS, 0,
                220.0, 380.0, 300.0));
        modeConfiguration.put(ZERO_IMPACT_TRIPS, new SingleModeConfig(
                ZERO_IMPACT_TRIPS, 0, 200.0, 380.0, 280.0));

        Arrays.sort(defaultMode);

    }


    public String get(String key) {
        if (prop != null)
            return prop.getProperty(key, "");

        return "";
    }

    public Integer getWeight(String key) {
        // TODO weight currently disabled
        // return modeConfiguration.get(key).getWeight();
        return 10;
    }

    public List<String> getPlayerIds() {
        return playerIds;
    }

    public Set<String> getModeKeySet() {
        return modeConfiguration.getModeKeySet();
    }

    public SingleModeConfig getModeConfig(String mode) {
        return modeConfiguration.get(mode);
    }

    public boolean isUserfiltering() {
        return userFiltering;
    }

    public boolean isSelecttoptwo() {
        return selectTopTwo;
    }

    public RecommendationSystemModeConfiguration getModeConfiguration() {
        return modeConfiguration;
    }

    public String[] getDefaultMode() {
        return defaultMode;
    }

    public String[] getDefaultModetrip() {
        return defaultModetrip;
    }

    public Double[] getPercentage() {
        return percentage;
    }

    /**
     * @param mode
     * @return true if input mode is a default mode trip
     */
    public boolean isDefaultMode(String mode) {
        if (mode == null) {
            return false;
        }
        for (int i = 0; i < getDefaultModetrip().length; i++) {
            String m = getDefaultModetrip()[i];
            if (m.toLowerCase().equals(mode.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public String getChallengeNamePrefix() {
        return challengeNamePrefix;
    }

}
