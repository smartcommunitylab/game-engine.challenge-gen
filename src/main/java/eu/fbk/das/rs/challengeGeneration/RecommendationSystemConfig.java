package eu.fbk.das.rs.challengeGeneration;

import java.util.HashMap;
import java.util.Set;

public class RecommendationSystemConfig {

	// we need to put all the configuration in different modes
	public static final String[] defaultMode = { "Walk_Km", "Bike_Km", "BikeSharing_Km", "Bus_Km", "Train_Km" };
	public static final String[] defaultModetrip = { "Walk_Trips", "Bike_Trips", "BikeSharing_Trips", "Bus_Trips",
			"Train_Trips" };

	private static HashMap<String, Integer> modeWeights;

	public RecommendationSystemConfig() {
		init();
	}

	private static void init() {
		RecommendationSystemConfig.modeWeights = new HashMap<String, Integer>();
		// just for test
		modeWeights.put("Walk", 10);
		modeWeights.put("Bike", 10);
		modeWeights.put("BikeSharing", 10);
		modeWeights.put("Bus", 0);
		modeWeights.put("Train", 0);
		// modeWeights.put("ZeroImpact", 10);
	}

	public static Integer getWeight(String key) {
		if (modeWeights == null) {
			init();
		}
		return modeWeights.get(key);
	}

	public static Set<String> getWeightKeySet() {
		if (modeWeights == null) {
			init();
		}
		return modeWeights.keySet();
	}

}
