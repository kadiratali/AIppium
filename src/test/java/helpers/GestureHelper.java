package helpers;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

public class GestureHelper {

    private static final Logger logger = LoggerFactory.getLogger(GestureHelper.class);

    private final AppiumDriver driver;

    public GestureHelper(AppiumDriver driver) {
        this.driver = driver;
    }

    public enum Direction {
        VERTICAL,
        HORIZONTAL_LEFT_TO_RIGHT,
        HORIZONTAL_RIGHT_TO_LEFT
    }

    public void longPress(WebElement element, int seconds) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");

        Sequence longPress = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(),
                        element.getLocation().getX(), element.getLocation().getY()))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new org.openqa.selenium.interactions.Pause(finger, Duration.ofSeconds(seconds)))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Arrays.asList(longPress));
    }

    public void openNotificationBar() {
        if (driver instanceof AndroidDriver) {
            ((AndroidDriver) driver).openNotifications();
            logger.info("Notification bar opened on Android device.");
        } else {
            logger.info("This feature is only available on Android devices.");
        }
    }

    public void swipeUntilElementVisible(WebElement element, int maxSwipe, Direction direction) {
        int alreadySwiped = 0;
        int screenWidth = driver.manage().window().getSize().width;
        int screenHeight = driver.manage().window().getSize().height;

        while (alreadySwiped < maxSwipe) {
            try {
                if (element.isDisplayed()) break;
            } catch (Exception e) {
                // Element not visible, continue swiping
            }

            int startX, startY, endX, endY;

            switch (direction) {
                case VERTICAL:
                    startX = screenWidth / 2;
                    startY = (int) (screenHeight * 0.8);
                    endX = startX;
                    endY = (int) (screenHeight * 0.2);
                    break;
                case HORIZONTAL_LEFT_TO_RIGHT:
                    startY = screenHeight / 2;
                    startX = (int) (screenWidth * 0.1);
                    endX = (int) (screenWidth * 0.9);
                    endY = startY;
                    break;
                case HORIZONTAL_RIGHT_TO_LEFT:
                    startY = screenHeight / 2;
                    startX = (int) (screenWidth * 0.9);
                    endX = (int) (screenWidth * 0.1);
                    endY = startY;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid swipe direction");
            }

            performSwipe(startX, startY, endX, endY, 500);
            alreadySwiped++;
        }
    }

    public void scrollRight(WebElement scrollView, int startPercentage, int endPercentage, int durationMs) {
        int startX = scrollView.getLocation().getX() + (scrollView.getSize().getWidth() * startPercentage / 100);
        int endX = scrollView.getLocation().getX() + (scrollView.getSize().getWidth() * endPercentage / 100);
        int startY = scrollView.getLocation().getY() + (scrollView.getSize().getHeight() / 2);
        performSwipe(startX, startY, endX, startY, durationMs);
    }

    private void performSwipe(int startX, int startY, int endX, int endY, int durationMs) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1);

        swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), startX, startY));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(durationMs), PointerInput.Origin.viewport(), endX, endY));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        driver.perform(Collections.singletonList(swipe));
    }
}
