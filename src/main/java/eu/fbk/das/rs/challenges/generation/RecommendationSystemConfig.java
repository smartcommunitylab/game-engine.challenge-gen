package eu.fbk.das.rs.challenges.generation;

import eu.fbk.das.rs.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class RecommendationSystemConfig {

    private static final Logger logger =
            LogManager.getLogger(RecommendationSystemConfig.class);

    /**
     * --------------------------------------
     * main configuration
     * --------------------------------------
     */

    public static final String RS_PROPERTIES = "rs.properties";

    private static Properties prop;

    public RecommendationSystemConfig() {
        prop = new Properties();
        try {
            prop.load(getClass().getClassLoader().getResourceAsStream(RS_PROPERTIES));
        } catch (IOException e) {
            Utils.logExp(logger, e);
        }

    }

    public String get(String key) {
        if (prop != null)
            return prop.getProperty(key, "");

        return "";
    }

    public void put(String key, String value) {
        if (prop != null)
             prop.setProperty(key, value);

    }
}
