Feature: Incoming Message Notification

  @Regression @Notification
  Scenario: Verify that incoming notifications are shown and details match
    Given the user navigates to the following menu
      | MenuItem          |
      | App               |
      | Notification      |
      | IncomingMessage   |
    When the "Show Notification" button is clicked
    Then the notification bar should be displayed
    When the notification is clicked
    Then the notification detail page should open
    And the text in the notification bar should match the text in the detail page