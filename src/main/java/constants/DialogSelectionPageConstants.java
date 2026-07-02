package constants;

import org.openqa.selenium.By;

public class DialogSelectionPageConstants {

    private DialogSelectionPageConstants() {
    }

    public static By dialogSelectionList(String option) {
        return By.xpath("//android.widget.Button[@content-desc='" + option + "']");
    }

    public static final By LIST_DIALOG_OPTION_LST = By.xpath("//android.widget.TextView[@resource-id='android:id/text1']");
    public static final By LIST_DIALOG_ALERT_MSG = By.xpath("//android.widget.TextView[@resource-id='android:id/message']");
}
