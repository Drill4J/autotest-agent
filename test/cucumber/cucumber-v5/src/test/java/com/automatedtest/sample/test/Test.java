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

import com.automatedtest.sample.infrastructure.driver.*;
import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.*;

import java.util.*;

//todo move/union to common module for v4 & v5
public class Test {
    protected WebDriver driver;
    protected Wait wait;
    protected int port;
    protected Set<String> testNames;

    Test() {
        this.testNames = Setup.testNames;
        this.port = Setup.port;
        this.driver = Setup.driver;
        this.wait = new Wait(this.driver);
        PageFactory.initElements(driver, this);
    }

    void goToHomePage() {
        driver.get("http://localhost:" + port + "/1");
        wait.forLoading(5);
    }

    void checkTestName(String testName) {
        Assert.assertFalse(testNames.isEmpty());
        Assert.assertTrue(testNames.stream().anyMatch(x -> x.contains(testName)));
    }

}
