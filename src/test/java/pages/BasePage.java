package pages;

import driver.DriverManager;
import helpers.PageContext;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.support.PageFactory;

public abstract class BasePage {

    protected final PageContext ctx;

    public BasePage(PageContext ctx) {
        this.ctx = ctx;
        PageFactory.initElements(new AppiumFieldDecorator(DriverManager.getDriver()), this);
    }
}