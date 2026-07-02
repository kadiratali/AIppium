package constants;

import org.openqa.selenium.By;
import static constants.BaseConstants.ID_PREFIX;


public class CustomTitlePageConstants {
    private CustomTitlePageConstants() {
    }

    // Locator constants
    public static final By CUSTOM_TITLE_DEFAULT_HEADER_LEFT = By.id(ID_PREFIX + "left_text");
    public static final By CUSTOM_TITLE_DEFAULT_HEADER_RIGHT = By.id(ID_PREFIX + "right_text");
    public static final By CUSTOM_TITLE_DEFAULT_LEFT_TEXT = By.id(ID_PREFIX + "left_text_edit");
    public static final By CUSTOM_TITLE_DEFAULT_RIGHT_TEXT = By.id(ID_PREFIX + "right_text_edit");
    public static final By CUSTOM_TITLE_CHANGE_LEFT_BTN = By.id(ID_PREFIX + "left_text_button");
    public static final By CUSTOM_TITLE_CHANGE_RIGHT_BTN = By.id(ID_PREFIX + "right_text_button");

    // Default Areas
    public static final String DEFAULT_HEADER_LEFT_TEXT = "Left is best";
    public static final String DEFAULT_HEADER_RIGHT_TEXT = "Right is always right";
}
