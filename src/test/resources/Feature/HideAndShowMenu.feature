Feature: Hide and Show Menu

  @Regression @HideShow
  Scenario: Hide and Show buttons functionality
    Given the user navigates to the following menu
      | MenuItem      |
      | App           |
      | Fragment      |
      | Hide and Show |
    Then there should be two "Hide" buttons and two text boxes visible
    When the second "Hide" button is clicked
    Then the corresponding text box should be hidden and the button text should change to "Show"
    When the user clicks Show button
    Then the text box should be visible again and the button text should be "Hide"
