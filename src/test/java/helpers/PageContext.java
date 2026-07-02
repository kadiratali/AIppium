package helpers;

import driver.DriverManager;
import io.appium.java_client.AppiumDriver;

public class PageContext {

    public final ElementHelper element;
    public final AssertHelper asserts;
    public final GestureHelper gesture;
    public final WaitHelper wait;

    public PageContext() {
        AppiumDriver driver = DriverManager.getDriver();
        this.wait = new WaitHelper(driver);
        this.element = new ElementHelper(driver, wait);
        this.asserts = new AssertHelper(wait);
        this.gesture = new GestureHelper(driver);
    }
}
