package com.instamall.base;

import com.instamall.actions.ElementActions;
import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.time.Duration;
import java.util.Map;

public abstract class BasePage {

    /** Enough drags to cross the longest screen in the app several times. */
    private static final int MAX_SCROLL_DRAGS = 8;

    // ---------- Actions ----------

    protected void click(By locator) {

        ElementActions.click(locator);
    }

    protected void type(By locator, String text) {

        ElementActions.type(locator, text);
    }

    protected void clear(By locator) {

        ElementActions.clear(locator);
    }

    protected String getText(By locator) {

        return ElementActions.getText(locator);
    }

    // ---------- Checks ----------

    protected boolean isDisplayed(By locator) {

        return ElementActions.isDisplayed(locator);
    }

    protected boolean isDisplayed(By locator, Duration timeout) {

        return WaitUtils.isVisible(locator, timeout);
    }

    protected boolean isPresent(By locator) {

        return WaitUtils.isPresent(locator);
    }

    /**
     * On screen right now, with no waiting at all — for racing two
     * outcomes against each other. See WaitUtils#isVisibleNow.
     */
    protected boolean isDisplayedNow(By locator) {

        return WaitUtils.isVisibleNow(locator);
    }

    // ---------- Waits ----------

    protected WebElement waitForVisibility(By locator) {

        return WaitUtils.waitForVisibility(locator);
    }

    /**
     * Acts as soon as the element is visible and clickable, without waiting
     * for it to stop moving. The default everywhere.
     */
    protected WebElement settle(By locator) {

        return WaitUtils.settle(locator);
    }

    protected boolean waitForInvisibility(By locator) {

        return WaitUtils.waitForInvisibility(locator);
    }

    /**
     * Drags the page until the element is on screen.
     *
     * For controls below the fold — a bag's Subtotal, a second store
     * group's buttons, a product's size row — which a tap cannot reach and
     * WDA reports as not visible until they are scrolled to.
     *
     * Done with short drags rather than WDA's scroll-to-visible: that
     * gives up ("max scroll count reached") on this app's lists when the
     * element is hidden under a fixed bar rather than past the end of the
     * content. Each drag moves the page by a third of the screen, in
     * whichever direction the element lies, and stops the moment WDA
     * reports it visible.
     */
    protected void scrollIntoView(WebElement element) {

        Dimension screen = DriverManager.getDriver().manage().window().getSize();
        int x = screen.getWidth() / 2;
        int step = screen.getHeight() / 3;

        for (int drag = 0; drag < MAX_SCROLL_DRAGS && !element.isDisplayed(); drag++) {

            // Below the visible band (or under the bottom bar): drag up.
            // Above it: drag down.
            boolean below = element.getRect().getY() > screen.getHeight() / 2;
            int fromY = below ? screen.getHeight() * 2 / 3 : screen.getHeight() / 3;
            int toY = below ? fromY - step : fromY + step;

            DriverManager.getDriver().executeScript("mobile: dragFromToForDuration",
                    Map.of("fromX", x, "fromY", fromY, "toX", x, "toY", toY, "duration", 0.3));
        }
    }

    protected <T> T waitUntil(ExpectedCondition<T> condition) {

        return WaitUtils.waitUntil(condition);
    }

    /**
     * See ElementActions.pressAndRelease()'s own doc comment for why
     * this exists alongside the plain click() above.
     */
    protected void pressAndRelease(By locator) {

        ElementActions.pressAndRelease(locator);
    }

}