package pages;

import helpers.PageContext;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static constants.CustomTitlePageConstants.*;

public class CustomTitleUpdatePage extends BasePage {

    private static final Logger logger = LoggerFactory.getLogger(CustomTitleUpdatePage.class);

    private String leftChangeText;
    private String rightChangeText;

    public CustomTitleUpdatePage(PageContext ctx) {
        super(ctx);
    }

    public void theDefaultTextBoxAndNavigationBarTextsAreDisplayed() {
        ctx.asserts.assertVisible(CUSTOM_TITLE_DEFAULT_HEADER_LEFT, "The default header left is not displayed.");
        ctx.asserts.assertVisible(CUSTOM_TITLE_DEFAULT_HEADER_RIGHT, "The default header right is not displayed.");
        ctx.asserts.assertVisible(CUSTOM_TITLE_DEFAULT_LEFT_TEXT, "The default text left box is not displayed.");
        ctx.asserts.assertVisible(CUSTOM_TITLE_DEFAULT_RIGHT_TEXT, "The default text right box is not displayed.");
        ctx.asserts.assertEquals(ctx.element.getText(CUSTOM_TITLE_DEFAULT_HEADER_LEFT), DEFAULT_HEADER_LEFT_TEXT, "The default header left text is not correct.");
        ctx.asserts.assertEquals(ctx.element.getText(CUSTOM_TITLE_DEFAULT_HEADER_RIGHT), DEFAULT_HEADER_RIGHT_TEXT, "The default header right text is not correct.");
        logger.info("The default text box and navigation bar texts are displayed.");
    }

    public void theUserUpdatesTheTextBoxTo(String textBox, String text) {
        By textBoxLocator = textBox.equals("left") ? CUSTOM_TITLE_DEFAULT_LEFT_TEXT : CUSTOM_TITLE_DEFAULT_RIGHT_TEXT;
        By changeButton = textBox.equals("left") ? CUSTOM_TITLE_CHANGE_LEFT_BTN : CUSTOM_TITLE_CHANGE_RIGHT_BTN;

        if (textBox.equals("left")) {
            setLeftChangeText(text);
        } else {
            setRightChangeText(text);
        }

        ctx.element.sendKeys(textBoxLocator, text);
        ctx.element.clickElement(ctx.element.findElement(changeButton), 5);
        logger.info("The user updates the {} text box to {}.", textBox, text);
    }

    public void theUpdatedTextsAreShownInAllTextFieldsAndNavigationBar() {
        ctx.asserts.assertEquals(ctx.element.getText(CUSTOM_TITLE_DEFAULT_HEADER_LEFT), getLeftChangeText(), "The updated header left text is not correct.");
        ctx.asserts.assertEquals(ctx.element.getText(CUSTOM_TITLE_DEFAULT_HEADER_RIGHT), getRightChangeText(), "The updated header right text is not correct.");
        ctx.asserts.assertEquals(ctx.element.getText(CUSTOM_TITLE_DEFAULT_LEFT_TEXT), getLeftChangeText(), "The updated text left box is not correct.");
        ctx.asserts.assertEquals(ctx.element.getText(CUSTOM_TITLE_DEFAULT_RIGHT_TEXT), getRightChangeText(), "The updated text right box is not correct.");
        logger.info("The updated texts are shown in all text fields and navigation bar.");
    }

    public String getLeftChangeText() { return leftChangeText; }
    public void setLeftChangeText(String leftChangeText) { this.leftChangeText = leftChangeText; }
    public String getRightChangeText() { return rightChangeText; }
    public void setRightChangeText(String rightChangeText) { this.rightChangeText = rightChangeText; }
}
