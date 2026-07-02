package constants;

import org.openqa.selenium.By;

public class TabMenuConstants {
    private TabMenuConstants() {
    }

    public static final By SCROLL_VIEW_CLASS = By.className("android.widget.HorizontalScrollView");


    public static By tabListMenu(String menuName) {
        return By.xpath("//android.widget.TextView[@content-desc='" + menuName + "']");
    }

    public static By scrollableTabs(String tabName) {
        return By.xpath("//android.widget.TextView[@resource-id='android:id/title' and @text='" + tabName + "']");
    }


    public static By scrollTabsText(String tabName) {
        return By.xpath("//android.widget.TextView[@text='Content for tab with tag " + tabName + "']");
    }

}
