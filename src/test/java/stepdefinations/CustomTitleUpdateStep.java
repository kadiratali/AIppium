package stepdefinations;

import helpers.PageContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.CustomTitleUpdatePage;

public class CustomTitleUpdateStep {

    private final CustomTitleUpdatePage customTitleUpdatePage;

    public CustomTitleUpdateStep(PageContext ctx) {
        this.customTitleUpdatePage = new CustomTitleUpdatePage(ctx);
    }

    @Then("the default textBox and navigation bar texts are displayed")
    public void theDefaultTextBoxAndNavigationBarTextsAreDisplayed() {
        customTitleUpdatePage.theDefaultTextBoxAndNavigationBarTextsAreDisplayed();
    }

    @When("the user updates the {string} textBox to {string}")
    public void theUserUpdatesTheTextBoxTo(String textBox, String text) {
        customTitleUpdatePage.theUserUpdatesTheTextBoxTo(textBox, text);
    }

    @Then("the updated texts are shown in all text fields and navigation bar")
    public void theUpdatedTextsAreShownInAllTextFieldsAndNavigationBar() {
        customTitleUpdatePage.theUpdatedTextsAreShownInAllTextFieldsAndNavigationBar();
    }
}
