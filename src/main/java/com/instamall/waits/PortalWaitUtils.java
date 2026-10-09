package com.instamall.waits;

import com.instamall.driver.PortalDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * WaitUtils' counterpart for the Delivery Portal — same shape, against
 * PortalDriverManager's WebDriver instead of DriverManager's IOSDriver. Kept
 * as its own class rather than a generified WaitUtils: the two drivers
 * genuinely differ (page loads and standard HTML waits here, no mobile
 * gestures), and a shared abstraction would mostly be indirection.
 */
public final class PortalWaitUtils {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration SHORT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration POLLING_INTERVAL = Duration.ofMillis(500);

    private PortalWaitUtils() {

    }

    private static WebDriver getDriver() {

        return PortalDriverManager.getDriver();
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

    // ---------- Arbitrary conditions ----------

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

    public static boolean isVisible(By locator, Duration timeout) {

        try {

            waitForVisibility(locator, timeout);

            return true;

        } catch (TimeoutException exception) {

            return false;
        }
    }

    /**
     * On screen at this instant, with no waiting whatsoever — for racing
     * two outcomes against each other. See WaitUtils#isVisibleNow, which
     * this mirrors.
     */
    public static boolean isVisibleNow(By locator) {

        try {

            List<WebElement> found = getDriver().findElements(locator);

            return !found.isEmpty() && found.get(0).isDisplayed();

        } catch (WebDriverException exception) {

            return false;
        }
    }

}
