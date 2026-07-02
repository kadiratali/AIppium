package driver;

import exception.DriverInitializationException;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.MutableCapabilities;

import java.net.URL;

/**
 * Factory implementation for creating Android-specific AppiumDriver instances.
 *
 * @author Kadir Atali
 */
public class AndroidDriverFactory extends BaseDriverFactory {

    @Override
    public String getPlatformName() {
        return "Android";
    }

    @Override
    protected MutableCapabilities createPlatformOptions() {
        UiAutomator2Options options = new UiAutomator2Options();

        // Apply capabilities from config.json (config is the single source of truth).
        // Defaults like noReset / autoGrantPermissions belong in config.json so they
        // are not silently overridden here.
        applyCapabilities(options);

        logger.debug("Android UiAutomator2Options created");
        return options;
    }

    @Override
    protected AppiumDriver initializeDriver(URL serverUrl, MutableCapabilities options) {
        return new AndroidDriver(serverUrl, (UiAutomator2Options) options);
    }

    @Override
    protected void validateCapabilities(MutableCapabilities options) {
        super.validateCapabilities(options);

        // Android-specific validation
        if (options.getCapability("appPackage") == null &&
                options.getCapability("app") == null) {
            throw new DriverInitializationException(
                    "Either 'appPackage' or 'app' capability is required for Android");
        }
    }
}
