package config;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Reads config.json and exposes the settings statically.
 * Loads the settings once when the class is initialized (static block).
 */
public class AppSettings {

    private static final Logger logger = LoggerFactory.getLogger(AppSettings.class);
    private static final String CONFIG_FILE_PATH = System.getProperty("user.dir") + "/src/config.json";
    private static final String DEFAULT_APPIUM_SERVER_URL = "http://localhost:4723";
    private static JSONObject config;
    private static String platform;

    static {
        try (FileReader reader = new FileReader(CONFIG_FILE_PATH)) {
            JSONParser parser = new JSONParser();
            config = (JSONObject) parser.parse(reader);
            platform = (String) config.get("platform");
            logger.info("Configuration loaded. Platform: {}", platform);

        } catch (FileNotFoundException e) {
            logger.error("ERROR: {} file not found!", CONFIG_FILE_PATH, e);
            throw new RuntimeException(CONFIG_FILE_PATH + " dosyası bulunamadı.", e);
        } catch (IOException | ParseException e) {
            logger.error("ERROR: an error occurred while reading {}!", CONFIG_FILE_PATH, e);
            throw new RuntimeException(CONFIG_FILE_PATH + " okunurken hata.", e);
        }
    }

    /**
     * Returns the active platform (android/ios) from config.json.
     * @return Platform name (String)
     */
    public static String getPlatform() {
        return platform;
    }

    /**
     * Returns all capabilities for the active platform
     * as a JSONObject.
     * @return Capabilities (JSONObject)
     */
    public static JSONObject getCapabilities() {
        return (JSONObject) config.get(platform);
    }

    /**
     * Returns the Appium server URL from config.json.
     * Falls back to localhost:4723 if "appiumServerUrl" is not defined.
     * @return Appium sunucu URL'i (String)
     */
    public static String getAppiumServerUrl() {
        Object url = config.get("appiumServerUrl");
        return url == null ? DEFAULT_APPIUM_SERVER_URL : url.toString();
    }

    private AppSettings() {
    }
}