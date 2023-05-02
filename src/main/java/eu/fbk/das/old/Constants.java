package eu.fbk.das.old;

public final class Constants {

    // private constructor to avoid instantiations
    private Constants() {
    }

    // General constants
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

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

    // challenges type constants (v2.1.0)
    public static final String ABSOLUTEINCREMENT = "absoluteIncrement";
    public static final String PERCENTAGEINCREMENT = "percentageIncrement";
    public static final String NEXTBADGE = "nextBadge";
    public static final String COMPLETEBADGECOLLECTION = "completeBadgeCollection";
    public static final String POICHECKIN = "poiCheckin";
    public static final String LEADERBOARDPOSITION = "leaderboardPosition";

    public static final String CHECKIN = "checkin";
    public static final String REPETITIVE_BEHAVIOUR = "repetitiveBehaviour";

    // challenges various constants
    public static final String NAME = "name";
    public static final String BONUS_SCORE = "bonusScore";
    public static final String BONUS_POINT_TYPE = "bonusPointType";
    public static final String TARGET = "target";
    public static final String COUNTER = "counter";
    public static final String END_DATE = "endDate";
    public static final String START_DATE = "startDate";
    public static final String ZEROIMPACT = "zeroImpact";
    public static final String COUNTER_NAME = "counterName";
    public static final String PERIOD_NAME = "periodName";
    public static final String PERIOD_TARGET = "periodTarget";
    public static final String BASELINE = "baseline";
    public static final String BADGECOLLECTIONNAME = "badgeCollectionName";
    public static final String GOAL_TYPE = "goalType";
    public static final String INITIAL_BADGE_NUMBER = "initialBadgeNum";
    public static final String SURVEY = "survey";
    public static final String COMPLETED = "completed";
    public static final String DATE_COMPLETED = "dateCompleted";
    public static final String POI_NAME = "poiName";
    public static final String EVENT_NAME = "eventName";
    public static final String POI_STATE = "poiState";
    public static final String EVENT_STATE = "eventState";
    public static final String MIN_MAX_SEPARATOR = "<";
    public static final String MODE_GREEN_LEAVES = "green leaves";

    // counters for rules
    public static final String[] COUNTERS = {"gp_current", "zero_impact_trips_past", "walk_km_past",
            "walk_trips_past", "bike_km_past", "bike_trips_past", "bikesharing_km_past",
            "bikesharing_trips_past", "bus_km_past", "bus_trips_past", "train_km_past",
            "train_trips_past", "car_km_past", "car_trips_past"};

}
