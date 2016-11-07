package eu.fbk.das.rs.challengeGeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * RecommendationSystem configuration main class
 */
public class RecommendationSystemConfig {

	// public constants
	public static final int PRIZE_MATRIX_NROW = 4;
	public static final int PRIZE_MATRIX_NCOL = 10;

	public static final int PRIZE_MATRIX_TRY_ONCE_ROW_INDEX = 1;
	public static final int PRIZE_MATRIX_TRY_ONCE_COL_INDEX = 9;

	public static final long PRIZE_MATRIX_APPROXIMATOR = 5;

	// Enable default users filtering
	private static final boolean userFiltering = true;
	// Enable select top 2 challenges
	private static final boolean selectTopTwo = true;

	// Transportation mode configuration
	// First, declare call supported modes. Order matters!
	private final String[] defaultMode = { "Walk_Km", "Bike_Km",
			"BikeSharing_Km", "Walk_Trips", "Bike_Trips", "BikeSharing_Trips",
			"ZeroImpact_Trips" };
	// Second, declare corresponding *_Trips of *_Km modes (i.e. Walk_km =>
	// Walk_Trips), used for try once challenges
	private final String[] defaultModetrip = { "Walk_Trips", "Bike_Trips",
			"BikeSharing_Trips" };

	// defining different improvement percentage 10%,20%, etc.
	private final Double[] percentage = { 0.1, 0.2, 0.3, 0.5, 1.0 };

	// default user player id
	private List<String> playerIds;

	// recommendation system configuration
	private RecommendationSystemModeConfiguration modeConfiguration;

	public RecommendationSystemConfig() {
		init();
	}

	// init recommendation system configuration
	private void init() {
		modeConfiguration = new RecommendationSystemModeConfiguration();
		modeConfiguration.put("Walk_Km", new SingleModeConfig("Walk_Km", 10,
				200, 300, 250));
		modeConfiguration.put("BikeSharing_Km", new SingleModeConfig(
				"BikeSharing_Km", 10, 200, 300, 250));
		modeConfiguration.put("ZeroImpact_Trips", new SingleModeConfig(
				"ZeroImpact_Trips", 10, 100, 250, 150));
		modeConfiguration.put("Train_Trips", new SingleModeConfig(
				"Train_Trips", 1, 100, 250, 150));
		modeConfiguration.put("Bus_Trips", new SingleModeConfig("Bus_Trips", 1,
				100, 250, 150));
		modeConfiguration.put("Train_Km", new SingleModeConfig("Train_Km", 1,
				100, 250, 150));
		modeConfiguration.put("Bike_Trips", new SingleModeConfig("Bike_Trips",
				10, 200, 300, 250));
		modeConfiguration.put("Walk_Trips", new SingleModeConfig("Walk_Trips",
				10, 150, 250, 200));
		modeConfiguration.put("BikeSharing_Trips", new SingleModeConfig(
				"BikeSharing_Trips", 10, 200, 300, 250));
		modeConfiguration.put("Bus_Km", new SingleModeConfig("Bus_Km", 1, 100,
				250, 150));
		modeConfiguration.put("Bike_Km", new SingleModeConfig("Bike_Km", 10,
				200, 300, 250));
		modeConfiguration.put("NoCar_Trips", new SingleModeConfig(
				"NoCar_Trips", 1, 100, 250, 150));

		// list of default player ids
		playerIds = new ArrayList<String>();
		Collections.addAll(playerIds, "7", "17741", "11125", "23897", "23515",
				"19092", "2795", "24502", "23513");
	}

	public Integer getWeight(String key) {
		return modeConfiguration.get(key).getWeight();
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

}
