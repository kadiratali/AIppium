Feature: Scrollable Tabs Navigation

  @Regression @ScrollableTabs @ResetAppBeforeTest
  Scenario: Navigating to the last tab in Scrollable Tabs and verifying Tab 30
    Given the user navigates to the following menu
      | MenuItem      |
      | Views         |
    Given the user scrolls to "Tabs" menu
    Then the user navigates to "5. Scrollable" Menu
    And the user clicks the "TAB 30" tab
    Then the opened tab information should belong to "Tab 30"
