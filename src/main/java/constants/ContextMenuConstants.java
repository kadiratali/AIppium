package constants;

import org.openqa.selenium.By;
import static constants.BaseConstants.ID_PREFIX;


public class ContextMenuConstants {

    private ContextMenuConstants() {
    }

    public static final By CONTEXT_MENU_LONG_PRESS_ME_BTN = By.id(ID_PREFIX + "long_press");

    public static By contextMenuOptions(String menuName) {
        return By.xpath("//android.widget.TextView[@resource-id='android:id/title' and @text='" + menuName + "']");
    }
}
