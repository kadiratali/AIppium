package helpers;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ElementHelper {

    private static final Logger logger = LoggerFactory.getLogger(ElementHelper.class);

    private final AppiumDriver driver;
    private final WaitHelper waitHelper;

    public ElementHelper(AppiumDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }

    public WebElement findElement(By by) {
        return findElement(by, WaitHelper.DEFAULT_TIMEOUT);
    }

    public WebElement findElement(By by, int timeout) {
        return waitHelper.waitForCondition(ExpectedConditions.visibilityOfElementLocated(by), timeout);
    }

    public List<WebElement> findElements(By by) {
        try {
            List<WebElement> elements = driver.findElements(by);
            if (elements == null || elements.isEmpty()) {
                logger.info("No elements found for locator: " + by);
            }
            return elements;
        } catch (Exception e) {
            logger.info("Error finding elements by: " + by + " -> " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public void clickElement(By by) {
        // On failure, a meaningful exception (locator + cause) propagates up;
        // the assertion decision is left to the page/step layer.
        waitHelper.wait(WaitHelper.DEFAULT_TIMEOUT)
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.elementToBeClickable(by))
                .click();
    }

    public void clickElement(WebElement element, int timeout) {
        waitHelper.waitForCondition(ExpectedConditions.elementToBeClickable(element), timeout).click();
    }

    public void sendKeys(By by, String text) {
        WebElement element = findElement(by);
        element.clear();
        element.sendKeys(text);
    }

    public String getText(By locator) {
        return findElement(locator, 5).getText();
    }

    public int getRandomIndexFromList(List<?> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty");
        }
        return ThreadLocalRandom.current().nextInt(0, list.size());
    }
}
