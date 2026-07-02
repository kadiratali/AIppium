Feature: List Dialog Selection

  @Regression @ListDialog
  Scenario: Verify the order and name of the selected element in the list dialog
    Given the user navigates to the following menu
      | MenuItem        |
      | App             |
      | Alert Dialogs   |
    When the user select the "List dialog" option
    Then the user selects a random element from the "List Dialog"
    Then the alert displays the correct element name and position


