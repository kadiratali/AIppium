package helpers;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;

public class AssertHelper {

    private final WaitHelper waitHelper;
    private final ElementHelper elementHelper;

    public AssertHelper(WaitHelper waitHelper, ElementHelper elementHelper) {
        this.waitHelper = waitHelper;
        this.elementHelper = elementHelper;
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
}
