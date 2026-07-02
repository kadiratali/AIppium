Feature: Manage Action Bar Tabs in the App

  @Regression @ActionBar
  Scenario: Verify adding and removing Action Bar Tabs
    Given the user navigates to the following menu
      | MenuItem        |
      | App             |
      | Action Bar      |
      | Action Bar Tabs |
    Then the "Toggle tab mode" option should be inactive
    When the user adds 3 new tabs
  #  Then the number of tabs should be 3