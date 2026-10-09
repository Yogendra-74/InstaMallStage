package com.instamall.waits;

import com.instamall.driver.DriverManager;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
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
    private static final Duration SETTLE_TIMEOUT = Duration.ofSeconds(5);
    /**
     * Do not shorten this to speed the suite up. It is not only how often
     * a wait looks, it is the baseline waitForStable measures movement
     * against: that method calls an element settled when its position is
     * unchanged between two consecutive polls. Sample 200ms apart instead
     * of 500ms and the tail of an eased animation reads as stationary, so
     * settle() returns mid-transition and the tap that follows is
     * swallowed. Tried at 200ms; the onboarding Login button started
     * missing.
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

    // ---------- Stability (element stops moving; useful on animated screens) ----------

    public static WebElement waitForStable(By locator) {

        return waitForStable(locator, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForStable(By locator, Duration timeout) {

        final Point[] lastLocation = {null};

        return newWait(timeout).until((ExpectedCondition<WebElement>) driver -> {

            WebElement element = driver.findElement(locator);
            Point currentLocation = element.getLocation();

            if (currentLocation.equals(lastLocation[0]) && element.isDisplayed()) {

                return element;
            }

            lastLocation[0] = currentLocation;

            return null;
        });
    }

    /**
     * Best-effort version of {@link #waitForStable(By)}: give the element
     * a moment to stop moving, but fall back to plain clickability rather
     * than failing.
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
        // decides it and the one place to change it back.
        //
        // What it gives up: waiting for stability cost about two and a half
        // seconds per call, on five or six calls a run. What it risks: a
        // tap landing while a screen is still animating in gets swallowed,
        // which is what happened to the onboarding Login button when the
        // poll interval was shortened. Store cards and the size sheet are
        // the other animated surfaces to watch.
        return waitForClickable(locator);
    }

    /**
     * The stricter wait, still available for a caller that genuinely needs
     * an element to have stopped moving before it is touched.
     */
    public static WebElement settleUntilStill(By locator) {

        try {

            return waitForStable(locator, SETTLE_TIMEOUT);

        } catch (TimeoutException exception) {

            return waitForClickable(locator);
        }
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