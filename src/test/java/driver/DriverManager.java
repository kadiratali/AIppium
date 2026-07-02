package driver;

import config.AppSettings;
import exception.DriverInitializationException;
import io.appium.java_client.AppiumDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe driver manager using Factory Pattern with Registry.
 * Manages AppiumDriver instances per thread.
 *
 * @author Kadir Atali
 */
public class DriverManager {

    private static final Logger logger = LoggerFactory.getLogger(DriverManager.class);
    private static final ThreadLocal<AppiumDriver> driver = new ThreadLocal<>();

    // Factory registry for extensibility
    private static final Map<String, DriverFactory> factoryRegistry = new HashMap<>();

    static {
        // Register platform factories
        registerFactory(new AndroidDriverFactory());
        registerFactory(new IOSDriverFactory());

        logger.debug("Driver factories registered: {}", factoryRegistry.keySet());
    }

    /**
     * Registers a driver factory for a specific platform.
     *
     * @param factory Factory to register
     */
    public static void registerFactory(DriverFactory factory) {
        String platform = factory.getPlatformName().toLowerCase();
        factoryRegistry.put(platform, factory);
        logger.debug("Factory registered for platform: {}", platform);
    }

    /**
     * Gets the current thread's AppiumDriver instance.
     *
     * @return AppiumDriver for current thread, or null if not initialized
     */
    public static AppiumDriver getDriver() {
        AppiumDriver currentDriver = driver.get();

        if (currentDriver == null) {
            logger.warn("Driver not initialized for thread: {}",
                    Thread.currentThread().getId());
        }

        return currentDriver;
    }

    /**
     * Initializes driver for the configured platform.
     *
     * @throws DriverInitializationException if initialization fails
     */
    public static void initializeDriver() {
        if (driver.get() != null) {
            logger.warn("Driver already initialized for thread: {}. Removing old instance.",
                    Thread.currentThread().getId());
            removeDriver();
        }

        String platformName = AppSettings.getPlatform();

        if (platformName == null || platformName.trim().isEmpty()) {
            throw new DriverInitializationException("Platform not configured in AppSettings");
        }

        logger.info("Initializing driver for platform: {} on thread: {}",
                platformName, Thread.currentThread().getId());

        DriverFactory factory = getDriverFactory(platformName);
        AppiumDriver driverInstance = factory.createDriver();

        driver.set(driverInstance);
        logger.info("Driver successfully initialized for thread: {}",
                Thread.currentThread().getId());
    }

    /**
     * Gets the appropriate DriverFactory for the platform.
     *
     * @param platform Platform name
     * @return DriverFactory implementation
     * @throws DriverInitializationException if platform is not supported
     */
    private static DriverFactory getDriverFactory(String platform) {
        String platformKey = platform.toLowerCase().trim();
        DriverFactory factory = factoryRegistry.get(platformKey);

        if (factory == null) {
            throw new DriverInitializationException(
                    String.format("Unsupported platform: %s. Available platforms: %s",
                            platform, factoryRegistry.keySet()));
        }

        logger.debug("Selected factory: {} for platform: {}",
                factory.getClass().getSimpleName(), platform);

        return factory;
    }

    /**
     * Quits and removes the driver for the current thread.
     */
    public static void removeDriver() {
        AppiumDriver currentDriver = driver.get();

        if (currentDriver != null) {
            try {
                currentDriver.quit();
                logger.info("Driver quit successfully for thread: {}",
                        Thread.currentThread().getId());
            } catch (Exception e) {
                logger.error("Error while quitting driver for thread: {}",
                        Thread.currentThread().getId(), e);
            } finally {
                driver.remove();
                logger.debug("Driver removed from ThreadLocal for thread: {}",
                        Thread.currentThread().getId());
            }
        } else {
            logger.debug("No driver to remove for thread: {}",
                    Thread.currentThread().getId());
        }
    }
}
