package com.instamall.actions;

import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;

public final class ElementActions {

    private ElementActions() {

    }

    public static void click(By locator) {

        WebElement element = WaitUtils.waitForClickable(locator);

        element.click();
    }

    /**
     * Taps an element with a brief, deliberate dwell between touch-down
     * and touch-up, rather than the instantaneous down-up a plain
     * click() performs.
     *
     * Confirmed live and reproducible on a consent checkbox control (the
     * "I agree that InstaMall..." messaging-consent toggle shared by
     * LoginPage and EmailLoginPage) and on the "Agree & Continue" button
     * ConsentPrompts taps: a plain click() — tried both via the standard
     * element click and via a same-length W3C tap action — registered as
     * a successful tap (no exception, no stale-element issue) but never
     * actually activated the control, repeatedly. A ~120ms hold between
     * down and up is what actually registers with whatever custom
     * gesture recognizer backs these controls. Not the default
     * everywhere — only needed for controls proven to need it, since an
     * added, unnecessary dwell on every tap would slow the whole suite
     * for no reason.
     */
    public static void pressAndRelease(By locator) {

        WebElement element = WaitUtils.waitForPresence(locator);

        new Actions(DriverManager.getDriver())
                .moveToElement(element)
                .clickAndHold()
                .pause(Duration.ofMillis(120))
                .release()
                .perform();
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