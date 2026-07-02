package driver;

import exception.DriverInitializationException;
import io.appium.java_client.AppiumDriver;

/**
 * Factory interface for creating platform-specific AppiumDriver instances.
 * Implementations must provide platform-specific configuration and initialization.
 *
 * @author Kadir Atali
 */
public interface DriverFactory {

    /**
     * Creates and configures an AppiumDriver instance for the target platform.
     *
     * @return Fully configured AppiumDriver instance
     * @throws DriverInitializationException if driver creation fails
     */
    AppiumDriver createDriver();

    /**
     * Returns the platform name this factory handles.
     *
     * @return Platform name (e.g., "Android", "iOS")
     */
    String getPlatformName();
}
