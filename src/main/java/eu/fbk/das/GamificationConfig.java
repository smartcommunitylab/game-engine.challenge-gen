package eu.fbk.das;

import eu.fbk.das.utils.Utils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import static eu.fbk.das.utils.Utils.p;

public class GamificationConfig {

    private static final Logger logger =
            Logger.getLogger(GamificationConfig.class);

    /**
     * --------------------------------------
     * main configuration
     * --------------------------------------
     */

    public static final String PROD_PROPERTIES = "prod.properties";
    public static final String TEST_PROPERTIES = "test.properties";

    private static Properties prop;

    public GamificationConfig() {
        this(false);
    }

    public GamificationConfig(boolean prod) {
        String f;
        if (prod) f = PROD_PROPERTIES; else f = TEST_PROPERTIES;

        prop = new Properties();
        try {
            prop.load(getClass().getClassLoader().getResourceAsStream(f));
        } catch (IOException e) {
            Utils.logExp(logger, e);
        }
    }

    public HashMap<String, String> extract() {
        HashMap<String, String> conf = new HashMap<>();
        for (final String name: prop.stringPropertyNames())
            conf.put(name.toUpperCase(), prop.getProperty(name));
        // p(conf);
        return conf;
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
