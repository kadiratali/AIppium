package constants;

import org.openqa.selenium.By;

public class CommonPageConstants {

    private CommonPageConstants() {
    }
    // Locator constants
    public static By menuItemList(String menuName) {
        return By.xpath("//android.widget.TextView[@content-desc='" + menuName + "']");
    }

    // Key for DataTable item in Cucumber
    public static final String MENU_ITEM_KEY = "MenuItem";
}
