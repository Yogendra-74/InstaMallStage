package com.instamall.base;

import com.instamall.waits.PortalWaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * BasePage's counterpart for the Delivery Portal — same shape, against
 * PortalWaitUtils instead of the mobile-only WaitUtils/ElementActions
 * stack (which is IOSDriver-specific and has no notion of a browser page).
 */
public abstract class PortalBasePage {

    protected void click(By locator) {

        PortalWaitUtils.waitForClickable(locator).click();
    }

    protected void type(By locator, String text) {

        WebElement field = PortalWaitUtils.waitForVisibility(locator);

        field.clear();
        field.sendKeys(text);
    }

    protected String getText(By locator) {

        return PortalWaitUtils.waitForVisibility(locator).getText();
    }

    protected boolean isDisplayed(By locator) {

        return PortalWaitUtils.isVisible(locator);
    }

    /**
     * On screen right now, with no waiting at all — for racing two
     * outcomes against each other.
     */
    protected boolean isDisplayedNow(By locator) {

        return PortalWaitUtils.isVisibleNow(locator);
    }

    protected WebElement waitForVisibility(By locator) {

        return PortalWaitUtils.waitForVisibility(locator);
    }

    protected WebElement waitForVisibility(By locator, Duration timeout) {

        return PortalWaitUtils.waitForVisibility(locator, timeout);
    }

}
