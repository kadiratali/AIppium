package constants;

import org.openqa.selenium.By;
import static constants.BaseConstants.ID_PREFIX;



public class NotificationControlConstants {
    private NotificationControlConstants() {
    }

    public static final By INCOMING_MESSAGE_SHOW_APP_NOTIFICATION = By.id(ID_PREFIX + "notify_app");
    public static final By NOTIFICATION_HEADER = By.xpath(
            "//android.widget.TextView[@resource-id=\"android:id/text\"]");

    public static final By NOTIFICATION_DETAIL_HEADER_TXT = By.xpath("//android.widget.TextView[@resource-id=\"android:id/title\" and @text=\"Joe\"]");
    public static final By NOTIFICATION_DETAIL_MSG = By.xpath("//android.widget.TextView[@resource-id=\"android:id/text\"]");
    public static final By NOTIFICATION_PAGE_NOTIFICATION_MSG = By.xpath("//android.widget.TextView[@content-desc=\"This is the text of the posted notification.\"]");
    public static final By NOTIFICATION_PAGE_NOTIFICATION_TXT = By.xpath("//android.widget.TextView[@resource-id=\"io.appium.android.apis:id/message\"]");
    public static final By NOTIFICATION_PAGE_FROM_INFO = By.xpath("//android.widget.TextView[@resource-id=\"io.appium.android.apis:id/from\"]");
    public static final By NOTIFICATION_PAGE_IMG = By.xpath("//android.widget.ImageView");
}
