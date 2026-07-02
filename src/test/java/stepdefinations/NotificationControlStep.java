package stepdefinations;

import helpers.PageContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.NotificationControlPage;

public class NotificationControlStep {

    private final NotificationControlPage notificationControlPage;

    public NotificationControlStep(PageContext ctx) {
        this.notificationControlPage = new NotificationControlPage(ctx);
    }

    @When("the {string} button is clicked")
    public void theButtonIsClicked(String showNotification) {
        notificationControlPage.clickShowNotification(showNotification);
    }

    @Then("the notification bar should be displayed")
    public void theNotificationBarShouldBeDisplayed() {
        notificationControlPage.theNotificationBarShouldBeDisplayed();
    }

    @When("the notification is clicked")
    public void theNotificationIsClicked() {
        notificationControlPage.theNotificationIsClicked();
    }

    @Then("the notification detail page should open")
    public void theNotificationDetailPageShouldOpen() {
        notificationControlPage.theNotificationDetailPageShouldOpen();
    }

    @And("the text in the notification bar should match the text in the detail page")
    public void theTextInTheNotificationBarShouldMatchTheTextInTheDetailPage() {
        notificationControlPage.theTextInTheNotificationBarShouldMatchTheTextInTheDetailPage();
    }
}
