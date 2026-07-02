package stepdefinations;

import helpers.PageContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.DialogSelectionPage;

public class DialogSelectionStep {

    private final DialogSelectionPage dialogSelectionPage;

    public DialogSelectionStep(PageContext ctx) {
        this.dialogSelectionPage = new DialogSelectionPage(ctx);
    }

    @When("the user select the {string} option")
    public void theUserSelectTheOption(String option) {
        dialogSelectionPage.theUserSelectTheOption(option);
    }

    @Then("the user selects a random element from the {string}")
    public void theUserSelectsARandomElementFromThe(String listOption) {
        dialogSelectionPage.theUserSelectsARandomElementFromThe(listOption);
    }

    @Then("the alert displays the correct element name and position")
    public void theAlertDisplaysTheCorrectElementNameAndPosition() {
        dialogSelectionPage.theAlertDisplaysTheCorrectElementNameAndPosition();
    }
}
