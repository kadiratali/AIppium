package pages;

import helpers.PageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static constants.DialogSelectionPageConstants.*;

public class DialogSelectionPage extends BasePage {

    private static final Logger logger = LoggerFactory.getLogger(DialogSelectionPage.class);

    private String listDialogOption;

    public DialogSelectionPage(PageContext ctx) {
        super(ctx);
    }

    public void theUserSelectTheOption(String option) {
        ctx.asserts.assertVisible(dialogSelectionList(option), "The " + option + " option is not displayed.");
        ctx.element.clickElement(dialogSelectionList(option));
        logger.info("The user selects the {} option.", option);
    }

    public void theUserSelectsARandomElementFromThe(String listOption) {
        ctx.asserts.assertVisible(LIST_DIALOG_OPTION_LST, "The list of options is not displayed.");
        int randListOption = ctx.element.getRandomIndexFromList(ctx.element.findElements(LIST_DIALOG_OPTION_LST));
        String listOptionText = ctx.element.findElements(LIST_DIALOG_OPTION_LST).get(randListOption).getText();
        ctx.element.clickElement(ctx.element.findElements(LIST_DIALOG_OPTION_LST).get(randListOption), 5);
        setListDialogOption(listOptionText);
        logger.info("The user selects a random element from the {} list.", listOption);
    }

    public void theAlertDisplaysTheCorrectElementNameAndPosition() {
        ctx.asserts.assertVisible(LIST_DIALOG_ALERT_MSG, "The alert is not displayed.");
        ctx.asserts.assertTrue(ctx.element.getText(LIST_DIALOG_ALERT_MSG).contains(getListDialogOption()),
                "The alert does not display the correct element name.");
        logger.info("The alert displays the correct element name and position.");
    }

    public String getListDialogOption() {
        return listDialogOption;
    }

    public void setListDialogOption(String listDialogOption) {
        this.listDialogOption = listDialogOption;
    }
}
