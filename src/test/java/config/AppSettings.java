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
    private static final double DEFAULT_HEALING_THRESHOLD = 0.75;
    private static final double DEFAULT_HEALING_MARGIN = 0.10;
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

    /**
     * Whether self-healing locators are enabled ("selfHealing.enabled" in config.json).
     * Defaults to false when the block is missing.
     */
    public static boolean isSelfHealingEnabled() {
        JSONObject settings = (JSONObject) config.get("selfHealing");
        return settings != null && Boolean.TRUE.equals(settings.get("enabled"));
    }

    /** Minimum match score a healing candidate must reach (default 0.75). */
    public static double getSelfHealingThreshold() {
        return selfHealingNumber("threshold", DEFAULT_HEALING_THRESHOLD);
    }

    /** Minimum score gap required between the best and second-best candidate (default 0.10). */
    public static double getSelfHealingMargin() {
        return selfHealingNumber("margin", DEFAULT_HEALING_MARGIN);
    }

    private static double selfHealingNumber(String key, double fallback) {
        JSONObject settings = (JSONObject) config.get("selfHealing");
        if (settings == null) {
            return fallback;
        }
        Object value = settings.get(key);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private AppSettings() {
    }
}