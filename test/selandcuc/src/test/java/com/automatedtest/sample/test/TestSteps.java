package com.automatedtest.sample.test;

import io.cucumber.core.api.Scenario;
import io.cucumber.java.*;
import io.cucumber.java.en.*;

public class TestSteps {

    private final Test test;
    private String actualTestName;

    public TestSteps() {
        this.test = new Test();
    }

    @Given("^A user navigates to HomePage$")
    public void aUserNavigatesToHomePage() {
        this.test.goToHomePage();
    }

    @Then("^Headers are injected$")
    public void headersAreInjected() {
        this.test.checkTestName(actualTestName);
    }

    @Before
    public void saveScenarioName(Scenario scenario) {
        actualTestName = scenario.getName();
    }
}
