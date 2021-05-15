/**
 * Copyright 2020 EPAM Systems
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.automatedtest.sample.test;

import com.epam.drill.*;
import com.epam.drill.plugins.test2code.api.*;
import com.epam.drill.test.agent.instrumentation.testing.cucumber.CucumberV5;
import com.epam.drill.test.common.*;
import io.cucumber.java.*;
import io.cucumber.java.en.*;

import java.util.*;


public class TestSteps {

    private final Test test;
    private final Set<TestData> actualTests = new HashSet<>();
    private String actualTestName;

    public TestSteps() {
        test = new Test();
    }

    @Before
    public void saveScenarioName(Scenario scenario) {
        actualTestName = UtilKt.urlEncode(scenario.getName());
        actualTests.add(new TestData(getTestName(scenario.getName()), TestResult.PASSED));
    }

    @Given("^A user navigates to HomePage$")
    public void aUserNavigatesToHomePage() {
        test.goToHomePage();
    }

    @Then("^Headers are injected$")
    public void headersAreInjected() {
        test.checkTestName(actualTestName);
    }

    private final String sessionId = UUID.randomUUID().toString();


    @Before
    public void startSession() {
        SessionProvider.INSTANCE.startSession(sessionId, "AUTO", false, "", false);
    }

    @After
    public void checkTests() {
        SessionProvider.INSTANCE.stopSession(sessionId);
        final ServerDate serverDate = UtilKt.getAdminData();
        final List<TestInfo> testFromAdmin = serverDate.getTests().get(sessionId);
        AssertKt.shouldContainsAllTests(testFromAdmin, actualTests);
        AssertKt.assertTestTime(testFromAdmin);
    }

    //TODO Will be removed in next commit
    private String getTestName(String name) {
        return CucumberV5.engineSegment + "/[class:" + TestSteps.class.getName() + "]/[method:" + name + "]";
    }

}
