package com.instamall.actions;

import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public final class ElementActions {

    private ElementActions() {

    }

    public static void click(By locator) {

        WebElement element = WaitUtils.waitForClickable(locator);

        element.click();
    }

    public static void type(By locator, String text) {

        WebElement element = WaitUtils.waitForVisibility(locator);

        element.clear();
        element.sendKeys(text);
    }

    public static void clear(By locator) {

        WaitUtils.waitForVisibility(locator).clear();
    }

    public static String getText(By locator) {

        return WaitUtils.waitForVisibility(locator).getText();
    }

    public static boolean isDisplayed(By locator) {

        return WaitUtils.isVisible(locator);
    }

    public static boolean isVisible(By locator) {

        return WaitUtils.isVisible(locator);
    }

    public static boolean isVisible(By locator, Duration timeout) {

        return WaitUtils.isVisible(locator, timeout);
    }

}