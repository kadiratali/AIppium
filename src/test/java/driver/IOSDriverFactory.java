package driver;

import exception.DriverInitializationException;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.MutableCapabilities;

import java.net.URL;

/**
 * Factory implementation for creating iOS-specific AppiumDriver instances.
 *
 * @author Kadir Atali
 */
public class IOSDriverFactory extends BaseDriverFactory {

    @Override
    public String getPlatformName() {
        return "iOS";
    }

    @Override
    protected MutableCapabilities createPlatformOptions() {
        XCUITestOptions options = new XCUITestOptions();

        // Apply capabilities from config.json (config is the single source of truth).
        applyCapabilities(options);

        logger.debug("iOS XCUITestOptions created");
        return options;
    }

    @Override
    protected AppiumDriver initializeDriver(URL serverUrl, MutableCapabilities options) {
        return new IOSDriver(serverUrl, (XCUITestOptions) options);
    }

    @Override
    protected void validateCapabilities(MutableCapabilities options) {
        super.validateCapabilities(options);

        // iOS-specific validation
        if (options.getCapability("bundleId") == null &&
                options.getCapability("app") == null) {
            throw new DriverInitializationException(
                    "Either 'bundleId' or 'app' capability is required for iOS");
        }
    }
}