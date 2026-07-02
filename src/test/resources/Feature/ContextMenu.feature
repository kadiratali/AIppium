Feature: Long Press Context Menu Test

  @Regression @LongPress @ContextMenu
  Scenario: Verify that Options A and B appear after long pressing the Long press me button
    Given the user navigates to the following menu
      | MenuItem       |
      | App            |
      | Fragment       |
      | Context Menu   |
    When the user long presses the "Long press me" button
    Then "Menu A" and "Menu B" are displayed
