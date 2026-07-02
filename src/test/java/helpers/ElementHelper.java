package helpers;

import config.AppSettings;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ElementHelper {

    private static final Logger logger = LoggerFactory.getLogger(ElementHelper.class);

    /** Short retry window for a healed locator - the page is already rendered. */
    private static final int HEALED_RETRY_TIMEOUT = 5;
    private static final List<String> BASELINE_ATTRIBUTES =
            List.of("resource-id", "content-desc", "text", "class");
    /** Locators whose baseline was already captured this run (avoid re-reading attributes). */
    private static final Set<String> capturedBaselines = ConcurrentHashMap.newKeySet();

    private final AppiumDriver driver;
    private final WaitHelper waitHelper;
    private final LocatorCache locatorCache = LocatorCache.getInstance();
    private final LocatorHealer locatorHealer = new LocatorHealer(
            AppSettings.getSelfHealingThreshold(), AppSettings.getSelfHealingMargin());

    public ElementHelper(AppiumDriver driver, WaitHelper waitHelper) {
        this.driver = driver;
        this.waitHelper = waitHelper;
    }

    public WebElement findElement(By by) {
        return findElement(by, WaitHelper.DEFAULT_TIMEOUT);
    }

    public WebElement findElement(By by, int timeout) {
        try {
            WebElement element = waitHelper.waitForCondition(
                    ExpectedConditions.visibilityOfElementLocated(by), timeout);
            captureBaseline(by, element);
            return element;
        } catch (TimeoutException e) {
            By healed = healedByOrThrow(by, e);
            try {
                return waitHelper.waitForCondition(
                        ExpectedConditions.visibilityOfElementLocated(healed), HEALED_RETRY_TIMEOUT);
            } catch (TimeoutException retryFailure) {
                logger.warn("Healed locator did not resolve either: {}", healed);
                throw e;
            }
        }
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
        try {
            WebElement element = waitHelper.wait(WaitHelper.DEFAULT_TIMEOUT)
                    .ignoring(StaleElementReferenceException.class)
                    .until(ExpectedConditions.elementToBeClickable(by));
            captureBaseline(by, element);
            element.click();
        } catch (TimeoutException e) {
            By healed = healedByOrThrow(by, e);
            try {
                waitHelper.wait(HEALED_RETRY_TIMEOUT)
                        .until(ExpectedConditions.elementToBeClickable(healed))
                        .click();
            } catch (TimeoutException retryFailure) {
                logger.warn("Healed locator did not resolve either: {}", healed);
                throw e;
            }
        }
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

    /**
     * Records the element's identity attributes into the locator cache the first
     * time a locator resolves in this run, so healing has a baseline to match
     * against if the same locator breaks later.
     */
    private void captureBaseline(By by, WebElement element) {
        if (!AppSettings.isSelfHealingEnabled()) {
            return;
        }
        String key = by.toString();
        if (!capturedBaselines.add(key)) {
            return;
        }
        Map<String, String> attrs = new HashMap<>();
        for (String attribute : BASELINE_ATTRIBUTES) {
            try {
                String value = element.getAttribute(attribute);
                if (value != null && !value.isBlank()) {
                    attrs.put(attribute, value);
                }
            } catch (Exception ignored) {
                // Attribute not supported on this platform/element - just skip it.
            }
        }
        // A single attribute is not enough to re-identify an element safely.
        if (attrs.size() >= 2) {
            locatorCache.put(key, attrs);
        }
    }

    /**
     * Attempts to heal a broken locator from its cached baseline; rethrows the
     * original timeout when healing is disabled, has no baseline, or finds no
     * unambiguous candidate. A successful heal is logged and attached to the
     * Allure report so the broken locator stays visible despite the green test.
     */
    private By healedByOrThrow(By by, TimeoutException original) {
        if (!AppSettings.isSelfHealingEnabled()) {
            throw original;
        }
        Map<String, String> baseline = locatorCache.get(by.toString());
        if (baseline == null) {
            throw original;
        }

        String pageSource;
        try {
            pageSource = driver.getPageSource();
        } catch (Exception e) {
            logger.warn("Page source could not be fetched for healing: {}", e.getMessage());
            throw original;
        }

        LocatorHealer.HealResult result = locatorHealer.heal(baseline, pageSource)
                .orElseThrow(() -> original);

        String note = String.format(
                "Broken locator : %s%nHealed to      : %s%nMatch score    : %.2f%n"
                        + "Update the constants class with the new locator.",
                by, result.by(), result.score());
        logger.warn("SELF-HEALING applied.\n{}", note);
        try {
            Allure.addAttachment("Self-healed locator", note);
        } catch (Throwable t) {
            // No active Allure lifecycle (e.g. plain unit test) - the WARN log remains.
        }
        return result.by();
    }
}
