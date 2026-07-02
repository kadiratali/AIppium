package helpers;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent store of "known good" element attributes per locator, used by
 * self-healing. Keyed by the locator's string form; values are the identity
 * attributes captured the last time the locator resolved successfully.
 * <p>
 * Backed by {@code .locator-cache.json} at the project root (gitignored) so
 * baselines survive across runs - healing can then kick in on the very first
 * run after an app change breaks a locator.
 */
public final class LocatorCache {

    private static final Logger logger = LoggerFactory.getLogger(LocatorCache.class);
    private static final Path CACHE_FILE = Path.of(System.getProperty("user.dir"), ".locator-cache.json");
    private static final LocatorCache INSTANCE = new LocatorCache();

    private final Map<String, Map<String, String>> entries = new ConcurrentHashMap<>();

    private LocatorCache() {
        load();
    }

    public static LocatorCache getInstance() {
        return INSTANCE;
    }

    /** Returns the baseline attributes for a locator, or null if never captured. */
    public Map<String, String> get(String locatorKey) {
        return entries.get(locatorKey);
    }

    /** Stores/updates the baseline for a locator and writes through to disk. */
    public void put(String locatorKey, Map<String, String> attributes) {
        entries.put(locatorKey, Map.copyOf(attributes));
        save();
    }

    private void load() {
        if (!Files.exists(CACHE_FILE)) {
            return;
        }
        try {
            JSONObject root = (JSONObject) new JSONParser().parse(Files.readString(CACHE_FILE));
            root.forEach((key, value) -> {
                Map<String, String> attrs = new HashMap<>();
                ((JSONObject) value).forEach((attrKey, attrValue) ->
                        attrs.put(attrKey.toString(), String.valueOf(attrValue)));
                entries.put(key.toString(), attrs);
            });
            logger.debug("Locator cache loaded: {} entries from {}", entries.size(), CACHE_FILE);
        } catch (Exception e) {
            logger.warn("Locator cache could not be read ({}), starting empty: {}", CACHE_FILE, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void save() {
        JSONObject root = new JSONObject();
        entries.forEach((key, attrs) -> {
            JSONObject entry = new JSONObject();
            entry.putAll(attrs);
            root.put(key, entry);
        });
        try {
            Files.writeString(CACHE_FILE, root.toJSONString());
        } catch (IOException e) {
            logger.warn("Locator cache could not be written ({}): {}", CACHE_FILE, e.getMessage());
        }
    }
}
