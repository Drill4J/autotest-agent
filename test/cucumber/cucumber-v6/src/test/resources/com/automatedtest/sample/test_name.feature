Feature: Test name


  @home_page @home_page_display
  Scenario: Check test name
    Given A user navigates to HomePage
    Then Headers are injected

  @home_page @home_page_display
  Scenario: Check test name in the second time
    Given A user navigates to HomePage
    Then Headers are injected

  @home_page @home_page_display
  Scenario: Кириллица в названии сценария
    Given A user navigates to HomePage
    Then Headers are injected
