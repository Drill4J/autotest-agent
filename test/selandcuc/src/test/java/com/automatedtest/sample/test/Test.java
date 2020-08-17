package com.automatedtest.sample.test;

import com.automatedtest.sample.infrastructure.driver.*;
import org.junit.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.*;

import java.util.*;


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
        Assert.assertTrue(testNames.contains(testName));
    }

}
