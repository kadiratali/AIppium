package helpers;

import org.openqa.selenium.By;
import org.testng.Assert;

public class AssertHelper {

    private final WaitHelper waitHelper;

    public AssertHelper(WaitHelper waitHelper) {
        this.waitHelper = waitHelper;
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
        assertTrue(waitHelper.isElementVisible(by, timeoutFinal), errorMessage);
    }
}
