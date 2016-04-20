package eu.trentorise.challenge;

import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
