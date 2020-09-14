package com.automatedtest.sample.infrastructure.driver;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

import java.time.*;

public class Wait {

    private final WebDriver driver;

    public Wait(WebDriver driver) {
        this.driver = driver;
    }

    private void waitUntilCondition(ExpectedCondition<Object> condition, String timeoutMessage, int timeout) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeout));
        wait.withMessage(timeoutMessage);
        wait.until(condition);
    }

    public void forLoading(int timeout) {
        ExpectedCondition<Object> condition = ExpectedConditions.jsReturnsValue("return document.readyState==\"complete\";");
        String timeoutMessage = "Page didn't load after " + timeout + " seconds.";
        waitUntilCondition(condition, timeoutMessage, timeout);
    }
}
