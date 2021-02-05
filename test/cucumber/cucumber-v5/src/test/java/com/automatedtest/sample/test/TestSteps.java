package com.automatedtest.sample.test;

import com.epam.drill.test.common.*;
import io.cucumber.java.*;
import io.cucumber.java.en.*;


public class TestSteps {

    private final Test test;
    private String actualTestName;

    public TestSteps() {
        this.test = new Test();
    }

    @Before
    public void saveScenarioName(Scenario scenario) {
        actualTestName = UtilKt.urlEncode(scenario.getName());
    }

    @Given("^A user navigates to HomePage$")
    public void aUserNavigatesToHomePage() {
        this.test.goToHomePage();
    }

    @Then("^Headers are injected$")
    public void headersAreInjected() {
        this.test.checkTestName(actualTestName);
    }
}
