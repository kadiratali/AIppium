package pages;

import helpers.PageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static constants.HideAndShowConstants.*;

public class HideAndShowPage extends BasePage {

    private static final Logger logger = LoggerFactory.getLogger(HideAndShowPage.class);

    public HideAndShowPage(PageContext ctx) {
        super(ctx);
    }

    public void thereShouldBeTwoButtonsAndTwoTextBoxesVisible(String hideButton) {
        ctx.asserts.assertVisible(HIDE_BUTTON_ONE, "The " + hideButton + " button is not displayed.");
        ctx.asserts.assertVisible(HIDE_BUTTON_TWO, "The second " + hideButton + " button is not displayed.");
        ctx.asserts.assertVisible(INITIAL_TEXT_1, "The initial text box is not displayed.");
        ctx.asserts.assertVisible(INITIAL_TEXT_2, "The second initial text box is not displayed.");
        logger.info("There should be two buttons and two text boxes visible.");
    }

    public void theSecondButtonIsClicked(String button) {
        ctx.asserts.assertVisible(HIDE_BUTTON_TWO, "The second " + button + " button is not displayed.");
        ctx.element.clickElement(HIDE_BUTTON_TWO);
        logger.info("The second {} button is clicked.", button);
    }

    public void theCorrespondingTextBoxShouldBeHiddenAndTheButtonTextShouldChangeTo(String buttonText) {
        ctx.asserts.assertVisible(HIDE_BUTTON_TWO, "The second " + buttonText + " button is not displayed.");
        ctx.asserts.assertEquals(ctx.element.getText(HIDE_BUTTON_TWO), buttonText, "The " + buttonText + " button text is not correct.");
        ctx.asserts.assertFalse(ctx.wait.isElementVisible(INITIAL_TEXT_2), "The second initial text box is still displayed.");
        logger.info("The corresponding text box should be hidden and the button text should change to {}.", buttonText);
    }

    public void theUserClicksShowButton() {
        ctx.asserts.assertVisible(HIDE_BUTTON_TWO, "The second show button is not displayed.");
        if (ctx.element.getText(HIDE_BUTTON_TWO).equals("Show")) {
            ctx.element.clickElement(HIDE_BUTTON_TWO);
        }
        logger.info("The user clicks the show button.");
    }

    public void theSecondTextBoxShouldBeVisible(String buttonText) {
        ctx.asserts.assertVisible(HIDE_BUTTON_TWO, "The second " + buttonText + " button is not displayed.");
        ctx.asserts.assertEquals(ctx.element.getText(HIDE_BUTTON_TWO), buttonText, "The " + buttonText + " button text is not correct.");
        ctx.asserts.assertVisible(INITIAL_TEXT_2, "The second initial text box is not displayed.");
        logger.info("The second text box should be visible.");
    }
}
