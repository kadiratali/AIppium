package helpers;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class WaitHelper {

    private static final Logger logger = LoggerFactory.getLogger(WaitHelper.class);
    static final int DEFAULT_TIMEOUT = 10;

    private final AppiumDriver driver;
    private WebDriverWait wait;

    public WaitHelper(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
    }

    <T> T waitForCondition(ExpectedCondition<T> condition, int timeout) {
        wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        try {
            return wait.until(condition);
        } catch (TimeoutException e) {
            // Propagate the actual cause (condition + timeout) instead of returning
            // null, which would surface as a confusing NullPointerException at the call site.
            String message = String.format(
                    "Condition not met within %d seconds: %s", timeout, condition);
            logger.error(message);
            throw new TimeoutException(message, e);
        }
    }

    public WebDriverWait wait(int second) {
        return new WebDriverWait(driver, Duration.ofSeconds(second));
    }

    /**
     * Fixed (static) wait. Prefer conditional waits
     * (isElementVisible / waitForCondition) whenever possible; this method
     * should only be used for cases that can't be expressed as a condition.
     */
    public void waitSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            // Interrupt durumunu yutmadan geri ver.
            Thread.currentThread().interrupt();
            logger.warn("waitSeconds interrupted after requesting {}s", seconds);
        }
    }

    public boolean isElementVisible(By by, int... timeout) {
        int time = timeout.length == 0 ? DEFAULT_TIMEOUT : timeout[0];
        try {
            wait(time).until(ExpectedConditions.visibilityOfElementLocated(by));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
