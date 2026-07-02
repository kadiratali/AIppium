package stepdefinations;

import helpers.PageContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import pages.TabMenuPage;

public class TabMenuStep {

    private final TabMenuPage tabMenuPage;

    public TabMenuStep(PageContext ctx) {
        this.tabMenuPage = new TabMenuPage(ctx);
    }

    @Given("the user scrolls to {string} menu")
    public void theUserScrollsToMenu(String tabMenu) {
        tabMenuPage.theUserScrollsToMenu(tabMenu);
    }

    @Then("the user navigates to {string} Menu")
    public void theUserNavigatesToMenu(String menu) {
        tabMenuPage.theUserNavigatesToMenu(menu);
    }

    @And("the user clicks the {string} tab")
    public void theUserClicksTheTab(String tabNumber) {
        tabMenuPage.theUserClicksTheTab(tabNumber);
    }

    @Then("the opened tab information should belong to {string}")
    public void theOpenedTabInformationShouldBelongTo(String tabName) {
        tabMenuPage.theOpenedTabInformationShouldBelongTo(tabName);
    }
}
