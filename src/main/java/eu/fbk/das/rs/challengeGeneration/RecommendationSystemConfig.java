package eu.fbk.das.rs.challengeGeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import eu.trentorise.challenge.PropertiesUtil;

/**
 * RecommendationSystem configuration main class
 */
public class RecommendationSystemConfig {

	// public constants
	public static final int PRIZE_MATRIX_NROW = 4;
	public static final int PRIZE_MATRIX_NCOL = 10;

	public static final int PRIZE_MATRIX_TRY_ONCE_ROW_INDEX = 1;
	public static final int PRIZE_MATRIX_TRY_ONCE_COL_INDEX = 9;

	public static final long PRIZE_MATRIX_APPROXIMATOR = 10;

	// Enable default users filtering
	private static final boolean userFiltering = true;
	// Enable select top 2 challenges
	private static final boolean selectTopTwo = true;

	// Transportation mode configuration
	// First, declare call supported modes. Order matters!
	private final String[] defaultMode = { "ZeroImpact_Trips", "Bus_Trips", "Train_Trips" };
	// Second, declare corresponding *_Trips of *_Km modes (i.e. Walk_km =>
	// Walk_Trips), used for try once challenges
	private final String[] defaultModetrip = { "ZeroImpact_Trips", "Bus_Trips", "Train_Trips" };
	// "Walk_Trips", "Bike_Trips", "BikeSharing_Trips"
	// defining different improvement percentage 10%,20%, etc.
	private final Double[] percentage = { 0.1, 0.2, 0.3, 0.5, 1.0 };

	// default user player id
	private List<String> playerIds;

	// recommendation system configuration
	private RecommendationSystemModeConfiguration modeConfiguration;
	private String challengeNamePrefix = "w10_rs_";

	public RecommendationSystemConfig() {
		init();
	}

	// init recommendation system configuration
	private void init() {
		modeConfiguration = new RecommendationSystemModeConfiguration();
		modeConfiguration.put("Walk_Km", new SingleModeConfig("Walk_Km", 0, 200, 300, 250));
		modeConfiguration.put("BikeSharing_Km", new SingleModeConfig("BikeSharing_Km", 0, 200, 300, 250));
		modeConfiguration.put("ZeroImpact_Trips", new SingleModeConfig("ZeroImpact_Trips", 10, 150, 300, 200));
		modeConfiguration.put("Train_Trips", new SingleModeConfig("Train_Trips", 8, 150, 350, 220));
		modeConfiguration.put("Bus_Trips", new SingleModeConfig("Bus_Trips", 10, 150, 350, 220));
		modeConfiguration.put("Train_Km", new SingleModeConfig("Train_Km", 0, 150, 350, 240));
		modeConfiguration.put("Bike_Trips", new SingleModeConfig("Bike_Trips", 0, 200, 300, 250));
		modeConfiguration.put("Walk_Trips", new SingleModeConfig("Walk_Trips", 0, 150, 250, 200));
		modeConfiguration.put("BikeSharing_Trips", new SingleModeConfig("BikeSharing_Trips", 0, 200, 300, 250));
		modeConfiguration.put("Bus_Km", new SingleModeConfig("Bus_Km", 0, 150, 350, 240));
		modeConfiguration.put("Bike_Km", new SingleModeConfig("Bike_Km", 0, 200, 300, 250));
		modeConfiguration.put("NoCar_Trips", new SingleModeConfig("NoCar_Trips", 0, 100, 250, 150));

		// list of default player ids
		playerIds = new ArrayList<String>();
		String filteringIds = PropertiesUtil.get(PropertiesUtil.FILTERING);
		if (filteringIds != null && !filteringIds.isEmpty() && filteringIds.contains(",")) {
			Collections.addAll(playerIds, filteringIds.split(","));
		}
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
			if (m == mode) {
				return true;
			}
		}
		return false;
	}

	public String getChallengeNamePrefix() {
		return challengeNamePrefix;
	}

}
