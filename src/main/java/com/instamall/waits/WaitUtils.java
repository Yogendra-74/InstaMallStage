package com.instamall.waits;

import com.instamall.driver.DriverManager;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public final class WaitUtils {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration SHORT_TIMEOUT = Duration.ofSeconds(3);
    /**
     * How often every wait looks. Do not shorten this to speed the suite
     * up: tried at 200ms, and taps on animated buttons started missing.
     */
    private static final Duration POLLING_INTERVAL = Duration.ofMillis(500);

    private WaitUtils() {

    }

    private static IOSDriver getDriver() {

        return DriverManager.getDriver();
    }

    private static WebDriverWait newWait(Duration timeout) {

        WebDriverWait wait = new WebDriverWait(getDriver(), timeout);

        wait.pollingEvery(POLLING_INTERVAL);
        wait.ignoring(NoSuchElementException.class);
        wait.ignoring(StaleElementReferenceException.class);

        return wait;
    }

    // ---------- Visibility ----------

    public static WebElement waitForVisibility(By locator) {

        return waitForVisibility(locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForVisibility(By locator, Duration timeout) {

        return newWait(timeout)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static boolean waitForInvisibility(By locator) {

        return waitForInvisibility(locator, DEFAULT_TIMEOUT);
    }

    public static boolean waitForInvisibility(By locator, Duration timeout) {

        return newWait(timeout)
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Non-throwing counterpart to {@link #waitForInvisibility(By, Duration)}
     * — for a caller that wants a boolean, not an exception, when the
     * element is still there once the timeout runs out.
     *
     * This is the one to reach for when confirming something has
     * cleared: it returns the moment the element is gone rather than
     * always sitting out the full timeout the way {@link #isVisible(By,
     * Duration)} must whenever the element is already absent —
     * isVisible() only returns quickly when the thing IS there; its
     * timeout path is reached precisely when it never shows up at all,
     * which makes it the wrong primitive for checking "did this go
     * away" (confirmed live: a 12s timeout used that way cost a flat 12s
     * on every single successful case it was meant to clear fast).
     *
     * @return true if the element was gone (or never there) within
     *         timeout, false if it was still visible when time ran out
     */
    public static boolean isInvisible(By locator, Duration timeout) {

        try {

            return waitForInvisibility(locator, timeout);

        } catch (TimeoutException stillVisible) {

            return false;
        }
    }

    // ---------- Presence ----------

    public static WebElement waitForPresence(By locator) {

        return waitForPresence(locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForPresence(By locator, Duration timeout) {

        return newWait(timeout)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    // ---------- Clickability ----------

    public static WebElement waitForClickable(By locator) {

        return waitForClickable(locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForClickable(By locator, Duration timeout) {

        return newWait(timeout)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    // ---------- Enabled state (e.g., SEND OTP activates after valid input) ----------

    public static WebElement waitForEnabled(By locator) {

        return waitForEnabled(locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForEnabled(By locator, Duration timeout) {

        return newWait(timeout).until((ExpectedCondition<WebElement>) driver -> {

            WebElement element = driver.findElement(locator);

            return element.isEnabled() ? element : null;
        });
    }

    /**
     * Waits for the element to be visible and clickable, without waiting
     * for it to stop moving.
     *
     * Insisting on stability is too strict for screens the app keeps
     * animating — a floating ADD TO BAG button over a scrolling page never
     * reports the same location twice, and the test would fail for a
     * reason that has nothing to do with the app being broken.
     */
    public static WebElement settle(By locator) {

        // Acts the moment the element is visible and clickable, without
        // waiting for it to stop moving first. Every settle() call site in
        // the suite goes through here, so this is the one place that
        // decides it.
        //
        // What it gives up: waiting for stability cost about two and a half
        // seconds per call, on five or six calls a run. What it risks: a
        // tap landing while a screen is still animating in gets swallowed,
        // which is what happened to an animated entry button when the
        // poll interval was shortened. Store cards and the size sheet are
        // the other animated surfaces to watch.
        return waitForClickable(locator);
    }

    // ---------- Arbitrary conditions ----------

    /**
     * For what no single locator says on its own — "one fewer row than a
     * moment ago", say. Same polling and same ignore list as the rest.
     */
    public static <T> T waitUntil(ExpectedCondition<T> condition) {

        return waitUntil(condition, DEFAULT_TIMEOUT);
    }

    public static <T> T waitUntil(ExpectedCondition<T> condition, Duration timeout) {

        return newWait(timeout).until(condition);
    }

    // ---------- Boolean checks (non-throwing) ----------

    public static boolean isPresent(By locator) {

        return isPresent(locator, SHORT_TIMEOUT);
    }

    public static boolean isPresent(By locator, Duration timeout) {

        try {

            waitForPresence(locator, timeout);

            return true;

        } catch (TimeoutException exception) {

            return false;
        }
    }

    public static boolean isVisible(By locator) {

        return isVisible(locator, SHORT_TIMEOUT);
    }

    /**
     * On screen at this instant, with no waiting whatsoever.
     *
     * For racing two outcomes against each other: any wait inside the
     * check would hide whichever signal happened to arrive second, and
     * the caller would pay that wait before even looking for the other.
     */
    public static boolean isVisibleNow(By locator) {

        try {

            List<WebElement> found = getDriver().findElements(locator);

            return !found.isEmpty() && found.get(0).isDisplayed();

        } catch (WebDriverException exception) {

            // Gone stale between finding it and reading it, or the tree
            // shifted mid-query. Either way it is not reliably on screen.
            return false;
        }
    }

    public static boolean isVisible(By locator, Duration timeout) {

        try {

            waitForVisibility(locator, timeout);

            return true;

        } catch (TimeoutException exception) {

            return false;
        }
    }

}