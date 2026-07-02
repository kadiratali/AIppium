Feature: Custom Title Update

  @Regression @CustomTitle @ClearCache
  Scenario: Validate default and updated texts in Custom Title Activity
    Given the user navigates to the following menu
      | MenuItem     |
      | App          |
      | Activity     |
      | Custom Title |
    Then the default textBox and navigation bar texts are displayed
    When the user updates the "left" textBox to "Left Updated Text"
    And the user updates the "right" textBox to "Right Updated Text"
    Then the updated texts are shown in all text fields and navigation bar