package stepdefinations;

import helpers.PageContext;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.ActionBarTabPage;

public class ActionBarTabStep {

    private final ActionBarTabPage actionBarTabPage;

    public ActionBarTabStep(PageContext ctx) {
        this.actionBarTabPage = new ActionBarTabPage(ctx);
    }

    @Then("the {string} option should be inactive")
    public void theOptionShouldBeInactive(String tabBar) {
        actionBarTabPage.theOptionShouldBeInactive(tabBar);
    }

    @When("the user adds {int} new tabs")
    public void theUserAddsNewTabs(int newTabCounter) {
        actionBarTabPage.theUserAddsNewTabs(newTabCounter);
    }
}
