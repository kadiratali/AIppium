package driver;

import config.AppSettings;
import exception.DriverInitializationException;
import io.appium.java_client.AppiumDriver;
import org.json.simple.JSONObject;
import org.openqa.selenium.MutableCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Abstract base class for driver factories.
 * Provides common functionality for all platform-specific factories.
 */
public abstract class BaseDriverFactory implements DriverFactory {

    protected static final Logger logger = LoggerFactory.getLogger(BaseDriverFactory.class);
    protected static final int DEFAULT_IMPLICIT_WAIT = 15;

    @Override
    public final AppiumDriver createDriver() {
        try {
            logger.info("Creating {} driver for thread: {}",
                    getPlatformName(), Thread.currentThread().getId());

            URL serverUrl = getAppiumServerUrl();
            MutableCapabilities options = createPlatformOptions();

            validateCapabilities(options);

            AppiumDriver driver = initializeDriver(serverUrl, options);
            configureDriver(driver);

            logger.info("{} driver successfully created for thread: {}",
                    getPlatformName(), Thread.currentThread().getId());

            return driver;

        } catch (Exception e) {
            String errorMsg = String.format("Failed to create %s driver: %s",
                    getPlatformName(), e.getMessage());
            logger.error(errorMsg, e);
            throw new DriverInitializationException(errorMsg, e);
        }
    }

    /**
     * Creates platform-specific capabilities/options.
     *
     * @return Configured MutableCapabilities for the platform
     */
    protected abstract MutableCapabilities createPlatformOptions();

    /**
     * Initializes the actual driver instance.
     *
     * @param serverUrl Appium server URL
     * @param options Platform-specific options
     * @return Initialized AppiumDriver
     */
    protected abstract AppiumDriver initializeDriver(URL serverUrl, MutableCapabilities options);

    /**
     * Applies capabilities from AppSettings to the options object.
     *
     * @param options Options object to populate
     */
    protected void applyCapabilities(MutableCapabilities options) {
        JSONObject capabilities = AppSettings.getCapabilities();

        capabilities.forEach((key, value) -> {
            String capKey = key.toString();
            Object capValue = "app".equals(capKey) ? resolveAppPath((String) value) : value;
            options.setCapability(capKey, capValue);
            logger.debug("Capability set: {} = {}", capKey, capValue);
        });
    }

    /**
     * Resolves the "app" capability against the project root when it's a relative
     * path, so config.json can stay portable across machines/checkouts. Absolute
     * paths and URLs (used for already-installed/remote apps) are left untouched.
     */
    private static String resolveAppPath(String appPath) {
        if (appPath == null || appPath.isBlank()) {
            return appPath;
        }

        Path path = Paths.get(appPath);
        if (path.isAbsolute() || appPath.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            return appPath;
        }

        return Paths.get(System.getProperty("user.dir")).resolve(path).normalize().toString();
    }

    /**
     * Validates that required capabilities are present.
     *
     * @param options Options to validate
     * @throws DriverInitializationException if validation fails
     */
    protected void validateCapabilities(MutableCapabilities options) {
        // Platform-specific validation can be overridden
        if (options.getCapability("platformName") == null) {
            throw new DriverInitializationException("platformName capability is required");
        }
    }

    /**
     * Configures the driver after initialization (timeouts, etc.).
     *
     * @param driver Driver to configure
     */
    protected void configureDriver(AppiumDriver driver) {
        driver.manage().timeouts()
                .implicitlyWait(Duration.ofSeconds(0));

        logger.debug("Implicit wait disabled. Using explicit waits only.");
    }

    /**
     * Gets the Appium server URL from configuration (config.json's "appiumServerUrl").
     *
     * @return Appium server URL
     * @throws MalformedURLException if the configured URL is invalid
     */
    protected URL getAppiumServerUrl() throws MalformedURLException {
        return new URL(AppSettings.getAppiumServerUrl());
    }
}
