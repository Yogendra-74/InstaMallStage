package com.instamall.components.referral;

import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;

import java.time.Duration;
import java.util.Map;

/**
 * The referral sheet the app throws up right after an order is placed.
 *
 * It matters to automation because while it is open every element behind
 * it — the whole order confirmation — reports visible == false. A flow
 * that goes straight to checking the order finds nothing to look at.
 *
 * Tolerant by design: nothing here fails when the sheet is absent.
 */
public final class ReferralPrompt {

    private static final Duration SHEET_TIMEOUT = Duration.ofSeconds(20);

    private ReferralPrompt() {

    }

    public static boolean isShowing() {

        return WaitUtils.isVisible(ReferralPromptLocators.SHEET, SHEET_TIMEOUT);
    }

    /**
     * On screen at this instant, with no waiting — for racing the sheet's
     * arrival against the confirmation screen coming up. A plain
     * {@link #isShowing()} here would sit out the full timeout on every run
     * the sheet never appears on, which is exactly the delay the caller is
     * racing to avoid (see WaitUtils#isVisibleNow).
     */
    public static boolean isShowingNow() {

        return WaitUtils.isVisibleNow(ReferralPromptLocators.SHEET);
    }

    /**
     * Closes the sheet by tapping the backdrop, falling back to swiping it
     * down. The app offers no close control, so there is nothing more
     * direct to press.
     *
     * @return true if the sheet was there and is now gone.
     */
    public static boolean dismissIfShowing() {

        if (!isShowing()) {

            return false;
        }

        tapScrim();

        // isInvisible() rather than !isVisible(): the scrim tap working is
        // the common case, and isVisible(locator, timeout) only returns
        // quickly when the element IS there — when it is already gone, it
        // cannot say so before the full timeout elapses. isInvisible()
        // returns the moment the sheet actually clears, and only spends
        // the full 3s on the rare case this is actually meant to catch —
        // the tap not registering. Same fix as CheckoutPage.placeOrder()'s
        // own tap-settled check, same bug.
        if (WaitUtils.isInvisible(ReferralPromptLocators.SHEET, Duration.ofSeconds(3))) {

            return true;
        }

        System.out.println(">>> Referral sheet ignored the backdrop tap — swiping it down.");
        swipeSheetDown();

        return WaitUtils.isInvisible(ReferralPromptLocators.SHEET, Duration.ofSeconds(3));
    }

    private static void tapScrim() {

        try {

            WaitUtils.waitForClickable(ReferralPromptLocators.SCRIM, SHEET_TIMEOUT).click();

        } catch (Exception exception) {

            System.out.println(">>> Could not tap the referral backdrop ("
                    + exception.getMessage() + ").");
        }
    }

    private static void swipeSheetDown() {

        try {

            WebElement sheet = DriverManager.getDriver()
                    .findElement(ReferralPromptLocators.SHEET);

            DriverManager.getDriver().executeScript(
                    "mobile: swipe",
                    Map.of("elementId", ((RemoteWebElement) sheet).getId(),
                            "direction", "down"));

        } catch (Exception exception) {

            System.out.println(">>> Could not swipe the referral sheet away ("
                    + exception.getMessage() + ").");
        }
    }

}
