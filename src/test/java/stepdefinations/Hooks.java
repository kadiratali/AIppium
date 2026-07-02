package stepdefinations;

import com.google.common.collect.ImmutableMap;
import config.AppSettings;
import driver.DriverManager;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.screenrecording.CanRecordScreen;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.json.simple.JSONObject;
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class Hooks {

    private static final Logger logger = LoggerFactory.getLogger(Hooks.class);
    private static final String TARGET_TAG = "@ResetAppBeforeTest";

    // Load the settings needed for ResetAppBeforeTest once
    private static final JSONObject platformSettings = AppSettings.getCapabilities();
    private static final String APP_PACKAGE = (String) platformSettings.get("appPackage");
    private static final String APP_BUNDLE_ID = (String) platformSettings.get("bundleId");
    private static final String APP_PATH = (String) platformSettings.get("app");

    /**
     * Runs BEFORE every scenario.
     * @param scenario The scenario about to run
     */
    @Before
    public void setUp(Scenario scenario) {
        logger.info("Scenario starting: {} [Thread: {}]", scenario.getName(), Thread.currentThread().getId());

        DriverManager.initializeDriver();
        startScreenRecording();

        if (scenario.getSourceTagNames().contains(TARGET_TAG)) {
            logger.info("Scenario has the @ResetAppBeforeTest tag. Reinstalling the app...");
            AppiumDriver driver = DriverManager.getDriver();
            reinstallApp(driver);
        }

        if (scenario.getSourceTagNames().contains("@ClearCache")) {
            AppiumDriver driver = DriverManager.getDriver();
            clearCache(driver);
        }
    }

    /**
     * Runs AFTER every scenario.
     * Takes a screenshot and stops the screen recording, attaching both on
     * failure, and quits the driver.
     */
    @After
    public void tearDown(Scenario scenario) {

        // Always stop the recording (even on pass) so it doesn't keep running on
        // the device; only the failure path decodes and attaches it.
        String recording = stopScreenRecording();

        if (scenario.isFailed()) {
            logger.error("Scenario FAILED: {}", scenario.getName());

            AppiumDriver driver = DriverManager.getDriver();

            if (driver != null) {
                try {
                    byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
                    scenario.attach(screenshot, "image/png", "Failure screenshot - " + scenario.getName());
                    logger.info("Screenshot captured and attached to the report.");

                } catch (Exception e) {
                    logger.error("Error while capturing the screenshot!", e);
                }

                if (recording != null) {
                    scenario.attach(Base64.getDecoder().decode(recording), "video/mp4",
                            "Failure video - " + scenario.getName());
                    logger.info("Screen recording captured and attached to the report.");
                }
            } else {
                logger.warn("Driver was null, screenshot could not be captured.");
            }

        } else {
            logger.info("Scenario PASSED: {}", scenario.getName());
        }
        DriverManager.removeDriver();
    }

    private void startScreenRecording() {
        AppiumDriver driver = DriverManager.getDriver();
        if (!(driver instanceof CanRecordScreen)) {
            return;
        }
        try {
            ((CanRecordScreen) driver).startRecordingScreen();
        } catch (Exception e) {
            logger.warn("Could not start screen recording.", e);
        }
    }

    private String stopScreenRecording() {
        AppiumDriver driver = DriverManager.getDriver();
        if (!(driver instanceof CanRecordScreen)) {
            return null;
        }
        try {
            return ((CanRecordScreen) driver).stopRecordingScreen();
        } catch (Exception e) {
            logger.error("Error while stopping the screen recording!", e);
            return null;
        }
    }


    private void reinstallApp(AppiumDriver driver) {
        if (driver == null) {
            logger.error("Driver was null, could not reinstall!");
            return;
        }

        if (driver instanceof AndroidDriver) {
            reinstallAppOnAndroid((AndroidDriver) driver);
        } else if (driver instanceof IOSDriver) {
            reinstallAppOnIOS((IOSDriver) driver);
        }
    }

    private void reinstallAppOnAndroid(AndroidDriver androidDriver) {
        requireCapability(APP_PACKAGE, "appPackage");
        requireCapability(APP_PATH, "app");
        try {
            if (androidDriver.isAppInstalled(APP_PACKAGE)) {
                androidDriver.removeApp(APP_PACKAGE);
            }
            androidDriver.installApp(APP_PATH);
            androidDriver.activateApp(APP_PACKAGE);
            logger.info("Android app reinstalled. [Thread: {}]", Thread.currentThread().getId());
        } catch (Exception e) {
            logger.error("Error while reinstalling the Android app!", e);
            throw new RuntimeException("Android uygulama yeniden yüklenemedi", e);
        }
    }

    private void reinstallAppOnIOS(IOSDriver iosDriver) {
        // iOS apps are identified by bundleId, not appPackage.
        requireCapability(APP_BUNDLE_ID, "bundleId");
        requireCapability(APP_PATH, "app");
        try {
            if (iosDriver.isAppInstalled(APP_BUNDLE_ID)) {
                iosDriver.removeApp(APP_BUNDLE_ID);
            }
            iosDriver.installApp(APP_PATH);
            iosDriver.activateApp(APP_BUNDLE_ID);
            logger.info("iOS app reinstalled. [Thread: {}]", Thread.currentThread().getId());
        } catch (Exception e) {
            logger.error("Error while reinstalling the iOS app!", e);
            throw new RuntimeException("iOS uygulama yeniden yüklenemedi", e);
        }
    }

    private void clearCache(AppiumDriver driver) {
        String appId = (driver instanceof IOSDriver) ? APP_BUNDLE_ID : APP_PACKAGE;
        requireCapability(appId, (driver instanceof IOSDriver) ? "bundleId" : "appPackage");
        driver.executeScript("mobile: clearApp", ImmutableMap.of("appId", appId));
        driver.executeScript("mobile: activateApp", ImmutableMap.of("appId", appId));
    }

    private static void requireCapability(String value, String capabilityName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "config.json içinde '" + capabilityName + "' capability'si tanımlı değil.");
        }
    }
}