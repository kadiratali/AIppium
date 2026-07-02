package helpers;

import ai.AiAssertEvaluator;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

public class AssertHelper {

    private static final Logger logger = LoggerFactory.getLogger(AssertHelper.class);

    private final WaitHelper waitHelper;
    private final ElementHelper elementHelper;
    private final AppiumDriver driver;
    /** Lazily created so tests that never call assertAi don't need an API key. */
    private AiAssertEvaluator aiEvaluator;

    public AssertHelper(WaitHelper waitHelper, ElementHelper elementHelper, AppiumDriver driver) {
        this.waitHelper = waitHelper;
        this.elementHelper = elementHelper;
        this.driver = driver;
    }

    public <T> void assertEquals(T actual, T expected, String message) {
        Assert.assertEquals(actual, expected, message);
    }

    public void assertTrue(boolean condition, String errorMessage) {
        Assert.assertTrue(condition, errorMessage);
    }

    public void assertFalse(boolean condition, String errorMessage) {
        Assert.assertFalse(condition, errorMessage);
    }

    public void assertFail(String message) {
        Assert.fail(message);
    }

    public void assertVisible(By by, String errorMessage, int... timeout) {
        int timeoutFinal = timeout.length == 0 ? WaitHelper.DEFAULT_TIMEOUT : timeout[0];
        // Resolve through ElementHelper (instead of WaitHelper.isElementVisible)
        // so the lookup captures baselines and can self-heal; the assert
        // semantics stay the same when the element is genuinely absent.
        try {
            elementHelper.findElement(by, timeoutFinal);
        } catch (TimeoutException e) {
            assertFail(errorMessage);
        }
    }

    /**
     * Evaluates a natural-language expectation against the current screen
     * (screenshot + page source) with AI. Best suited for checks that are hard
     * to express with locators: perceptual ones ("no overlapping text", "an
     * error banner is shown") and derived-data ones ("the list is sorted by
     * date", "the total equals the sum of the items"). For exact values keep
     * using the deterministic asserts - AI verdicts are not deterministic.
     * <p>
     * Fails with the AI-generated explanation when the expectation isn't met,
     * and fails loudly (rather than silently passing) when the evaluation
     * itself is impossible, e.g. ANTHROPIC_API_KEY is missing.
     */
    public void assertAi(String expectation) {
        AiAssertEvaluator.Verdict verdict;
        try {
            if (aiEvaluator == null) {
                aiEvaluator = new AiAssertEvaluator();
            }
            String screenshot = driver.getScreenshotAs(OutputType.BASE64);
            verdict = aiEvaluator.evaluate(expectation, driver.getPageSource(), screenshot);
        } catch (Exception e) {
            Assert.fail("AI assertion could not be evaluated: " + e.getMessage()
                    + " | Expectation: " + expectation);
            return;
        }

        String note = "Expectation: " + expectation
                + "\nVerdict    : " + (verdict.passed() ? "PASS" : "FAIL")
                + "\nReason     : " + verdict.reason();
        logger.info("AI assertion evaluated.\n{}", note);
        try {
            Allure.addAttachment("AI assertion", note);
        } catch (Throwable t) {
            // No active Allure lifecycle - the log line above remains.
        }

        if (!verdict.passed()) {
            Assert.fail("AI assertion failed: " + verdict.reason()
                    + " | Expectation: " + expectation);
        }
    }
}
