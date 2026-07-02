package constants;

import org.openqa.selenium.By;
import static constants.BaseConstants.ID_PREFIX;

public class ActionBarPageContants {
    private ActionBarPageContants() {
    }

    public static final By ACTION_BAR_TABS_TOGGLE_TAB_MODU_BTN = By.id(ID_PREFIX + "btn_toggle_tabs");
    public static final By ACTION_BAR_TABS_PAGE_HEADER = By.xpath("//android.widget.TextView[@text=\"App/Action Bar/Action Bar Tabs\"]");
    public static final By ACTION_BAR_TABS_ADD_NEW_TAB = By.id(ID_PREFIX + "btn_add_tab");
    public static final By ACTION_BAR_LST = By.xpath("//android.widget.ListView[@resource-id=\"android:id/list\"]");
}
