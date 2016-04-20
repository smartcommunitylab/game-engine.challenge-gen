package eu.trentorise.game.challenges.api;

public final class Constants {

    // private constructor to avoid instantiations
    private Constants() {
    }

    // Challenge constants
    public static final String fileExt = ".drl";
    public static final String CH = "ch_";
    public static final String TYPE = "_type";
    public static final String START_CHTS = "_startChTs";
    public static final String END_CHTS = "_endChTs";
    public static final String SUCCESS = "_success";
    public static final String CH_ID = "ch_ID";
    public static final Object MODE = "mode";
    public static final String CH_MODE = "ch_mode";
    public static final Object POINT_TYPE = "point_type";
    public static final String CH_POINT_TYPE = "ch_point_type";
    public static final String RULE_PREFIX = "db://";

    // counters for rules
    public static final String[] COUNTERS = { "gp_current",
	    "zero_impact_trips_past", "walk_km_past", "walk_trips_past",
	    "bike_km_past", "bike_trips_past", "bikesharing_km_past",
	    "bikesharing_trips_past", "bus_km_past", "bus_trips_past",
	    "train_km_past", "train_trips_past", "car_km_past",
	    "car_trips_past" };

}
