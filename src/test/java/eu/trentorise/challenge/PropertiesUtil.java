package eu.trentorise.challenge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public final class PropertiesUtil {

    private static final Logger logger = LogManager
            .getLogger(PropertiesUtil.class);

    public static final String TEST_PROPERTIES = "test.properties";
    public static final String SAVE_ITINERARY = "SAVE_ITINERARY";
    public static final String HOST = "HOST";
    public static final String CONTEXT = "CONTEXT";
    public static final String INSERT_CONTEXT = "INSERT_CONTEXT";
    public static final String GAMEID = "GAMEID";
    public static final String USERNAME = "USERNAME";
    public static final String PASSWORD = "PASSWORD";
    public static final String RELEVANT_CUSTOM_DATA = "RELEVANT_CUSTOM_DATA";
    public static final String FILTERING_ENABLED = "FILTERING_ENABLED";
    public static final String FILTERING = "FILTERING";

    private static Properties prop;

    private PropertiesUtil() {
    }

    public static String get(String key) {
        if (prop == null) {
            prop = new Properties();
            try {
                prop.load(PropertiesUtil.class
                        .getResourceAsStream(TEST_PROPERTIES));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        return prop.getProperty(key, "");
    }
}
