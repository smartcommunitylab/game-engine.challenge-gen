package eu.fbk.das.rs.challengeGeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class RecommendationSystemConfig {

	public static final int PRIZE_MATRIX_NROW = 4;
	public static final int PRIZE_MATRIX_NCOL = 10;

	public static final int PRIZE_MATRIX_TRY_ONCE_ROW_INDEX = 1;
	public static final int PRIZE_MATRIX_TRY_ONCE_COL_INDEX = 9;

	// we need to put all the configuration in different modes
	public static final String[] defaultMode = { "Bike_Km", "BikeSharing_Km",
			"Bike_Trips", "BikeSharing_Trips" };
	public static final String[] defaultModetrip = { "Bike_Trips",
			"BikeSharing_Trips" };

	public static final long PRIZE_MATRIX_APPROXIMATOR = 5;

	// public static final String[] defaultMode = { "Walk_Km", "Bike_Km",
	// "BikeSharing_Km", "Walk_Trips", "Bike_Trips", "BikeSharing_Trips",
	// "ZeroImpact_Trips" };

	// , "Bike_Trips", "BikeSharing_Trips", "Bus_Trips",
	// "Train_Trips" };

	// , "Walk_Trips", "Bike_Trips", "BikeSharing_Trips", "Bus_Trips",
	// "Train_Trips"

	private static List<String> playerIds;

	private static boolean init = false;
	private static RecommendationSystemModeConfiguration modeConfiguration;

	public static final boolean userFiltering = false;
	public static final boolean selectTopTwo = false;

	public RecommendationSystemConfig() {
		init();
	}

	private static void init() {
		init = true;
		// init recommendation system configuration
		modeConfiguration = new RecommendationSystemModeConfiguration();
		modeConfiguration.put("Walk_Km", new SingleModeConfig("Walk_Km", 1,
				200, 300, 250));
		modeConfiguration.put("Walk_Trips", new SingleModeConfig("Walk_Trips",
				1, 150, 250, 200));
		modeConfiguration.put("Bike_Trips", new SingleModeConfig("Bike_Trips",
				10, 200, 300, 250));
		modeConfiguration.put("Bike_Km", new SingleModeConfig("Bike_Km", 9,
				200, 300, 250));
		modeConfiguration.put("BikeSharing_Trips", new SingleModeConfig(
				"BikeSharing_Trips", 8, 200, 300, 250));
		modeConfiguration.put("BikeSharing_Km", new SingleModeConfig(
				"BikeSharing_Km", 7, 200, 300, 250));
		modeConfiguration.put("ZeroImpact_Trips", new SingleModeConfig(
				"ZeroImpact_Trips", 1, 100, 250, 150));

		modeConfiguration.put("Bus_Km", new SingleModeConfig("Bus_Km", 1, 100,
				250, 150));
		modeConfiguration.put("Bus_Trips", new SingleModeConfig("Bus_Trips", 1,
				100, 250, 150));
		modeConfiguration.put("Train_Km", new SingleModeConfig("Train_Km", 1,
				100, 250, 150));
		modeConfiguration.put("Train_Trips", new SingleModeConfig(
				"Train_Trips", 1, 100, 250, 150));
		modeConfiguration.put("NoCar_Trips", new SingleModeConfig(
				"NoCar_Trips", 1, 100, 250, 150));

		// RecommendationSystemConfig.modeWeights = new HashMap<String,
		// Integer>();
		// // just for test
		// modeWeights.put("Walk", 0);
		// modeWeights.put("Bike", 10);
		// modeWeights.put("BikeSharing", 9);
		// modeWeights.put("Bus", 0);
		// modeWeights.put("Train", 0);
		// modeWeights.put("ZeroImpact", 0);
		// modeWeights.put("NoCar", 1);

		// list of default player ids
		RecommendationSystemConfig.playerIds = new ArrayList<String>();
		// Collections.addAll(RecommendationSystemConfig.playerIds, "23501",
		// "1658", "23692", "23897", "19092", "24502", "4458");

		Collections.addAll(RecommendationSystemConfig.playerIds, "7", "17741",
				"11125", "23897", "23515", "19092", "2795", "24502", "23513");

	}

	public static Integer getWeight(String key) {
		if (!init) {
			init();
		}
		System.out.println("key>" + key);
		return modeConfiguration.get(key).getWeight();
	}

	public static List<String> getPlayerIds() {
		if (!init) {
			init();
		}
		return playerIds;
	}

	public static Set<String> getModeKeySet() {
		if (!init) {
			init();
		}
		return modeConfiguration.getModeKeySet();
	}

	public static SingleModeConfig getModeConfig(String mode) {
		if (!init) {
			init();
		}
		return modeConfiguration.get(mode);
	}

}
