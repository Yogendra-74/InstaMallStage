package com.instamall.components.consent;

import com.instamall.actions.ElementActions;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import java.time.Duration;
import java.time.Instant;

/**
 * Clears what stands between a fresh sign in and the app itself: the iOS
 * "Save Password?" alert, the Notification permission alert, the Location
 * permission alert, and the "Terms and Conditions" (not every
 * account/run), "AI & Biometric Consent", and "AI Stylist Consent"
 * sheets, each sheet accepted with Agree & Continue.
 *
 * The two permission alerts in particular are not guaranteed to arrive in
 * any fixed order — confirmed live to vary run to run — so neither is
 * checked ahead of the other as "the one that shows first". Each has its
 * own branch in the loop below, independently gated on whether that one
 * specific alert is on screen right now; every pass checks both, so
 * whichever actually shows gets answered regardless of which came first,
 * and the loop keeps going — through as many passes as it takes, within
 * its own overall budget — until neither is showing any more.
 *
 * None of them is guaranteed — the alert depends on the simulator's
 * password settings, and an account that has agreed before may not be
 * asked again — and none of them is quick: the sign in itself is a
 * staging round trip that can run well past ten seconds before anything
 * appears. So this does not wait a fixed time for a prompt; it watches
 * for prompts and for the landing screen together, answers whatever turns
 * up, and returns once the landing screen has been on its own for a
 * moment. While a sheet is up the screen behind it reports visible ==
 * false, which is what makes "landed" and "a sheet is up" distinct.
 */
public final class ConsentPrompts {

    // Sign in plus whatever the app puts up afterwards. Matches the home
    // screen's own first-paint allowance.
    private static final Duration LANDED_WITHIN = Duration.ofSeconds(60);

    // Once landed with nothing on top, the next sheet — if one is coming
    // — follows within a second or two; this long clear means they are
    // done.
    private static final Duration QUIET_FOR = Duration.ofSeconds(2);

    private static final long POLL_MILLIS = 500;

    // How long a single tap at Agree & Continue gets before giving up on
    // this pass — short, so a tap the Save Password alert is covering
    // fails fast rather than sitting out the default timeout.
    private static final Duration CLICK_ATTEMPT_TIMEOUT = Duration.ofSeconds(3);

    // How long a sheet gets to slide away after Agree & Continue. Running
    // out is not a failure — the loop looks again and taps whatever is up.
    private static final long SHEET_LEAVES_WITHIN_SECONDS = 4;

    private ConsentPrompts() {

    }

    /**
     * @param landed something only the screen behind the prompts shows
     *               when nothing is on top of it — the home tab, say
     * @return how many prompts were answered
     * @throws IllegalStateException if the landing screen never showed
     *                               with nothing on top of it
     */
    public static int acceptAfterSignIn(By landed) {

        int handled = 0;
        Instant started = Instant.now();
        Instant clearSince = null;

        while (Duration.between(started, Instant.now()).compareTo(LANDED_WITHIN) < 0) {

            // Save Password alert — timing is unpredictable, iOS can
            // raise it before or after either permission alert below.
            if (WaitUtils.isVisibleNow(ConsentPromptLocators.SAVE_PASSWORD_SAVE)) {

                // Same race as the Agree & Continue branch below: another
                // alert/sheet can cover or clear this button between the
                // isVisibleNow check above and the click landing. Tolerated
                // the same way — a timeout here is not a failure, the next
                // pass looks again.
                try {

                    WaitUtils.waitForClickable(ConsentPromptLocators.SAVE_PASSWORD_SAVE,
                            CLICK_ATTEMPT_TIMEOUT).click();
                    handled++;
                    clearSince = null;

                } catch (TimeoutException notClickableYet) {

                    // Covered or already gone — looked at again next pass.
                }

                continue;
            }

            // Notification and Location permission alerts — order between
            // them is not assumed (confirmed live to vary run to run).
            // Each call here is a single no-wait check-and-tap (see
            // SystemAlerts' own doc comments on these two methods), so
            // both are tried every single pass of this loop regardless of
            // which actually shows first.
            //
            // Also handled explicitly, in their own loop, by
            // Single_Order_Complete.phase1_placeOrderFromCustomerApp()
            // right after sign-in, before this method is reached — by the
            // time that loop hands off here, via clickNext(), neither
            // should still be showing. Kept here too since this method is
            // also called directly by other tests that sign in through
            // EmailLoginPage without a permissions loop of their own
            // (ItemPriceDecreaseComplete, ItemPriceIncreaseComplete).
            if (SystemAlerts.grantNotificationIfShowingNow() || SystemAlerts.grantLocationIfShowingNow()) {

                handled++;
                clearSince = null;
                continue;
            }

            if (WaitUtils.isVisibleNow(ConsentPromptLocators.AGREE_AND_CONTINUE)) {

                // Which sheet this is, so its leaving can be watched by
                // title: the next sheet slides in under the very same
                // button name, often before this one has finished going,
                // so the button itself never reads as gone. Checked in the
                // order the sheets actually arrive — Terms and Conditions
                // (not every account/run), then AI & Biometric Consent,
                // then AI Stylist Consent, confirmed live as the full
                // sequence; AI & Biometric Consent is the fallback default
                // since it is the one every account sees regardless of
                // history.
                By title;

                if (WaitUtils.isVisibleNow(ConsentPromptLocators.TERMS_AND_CONDITIONS_TITLE)) {

                    title = ConsentPromptLocators.TERMS_AND_CONDITIONS_TITLE;

                } else if (WaitUtils.isVisibleNow(ConsentPromptLocators.AI_STYLIST_TITLE)) {

                    title = ConsentPromptLocators.AI_STYLIST_TITLE;

                } else {

                    title = ConsentPromptLocators.AI_BIOMETRIC_TITLE;
                }

                // Short timeout on purpose: the iOS "Save Password?" alert
                // can slide in on top of this sheet after the button is
                // already seen as visible == true (WDA reports that
                // regardless — the alert renders in a separate layer), and
                // a click aimed at a button the alert is covering never
                // lands. A short attempt fails fast instead of blocking
                // the full default timeout on a tap that cannot land, so
                // the loop gets back to its Save Password branch and
                // clears it — after which this same branch tries again.
                try {

                    WaitUtils.waitForClickable(ConsentPromptLocators.AGREE_AND_CONTINUE,
                            CLICK_ATTEMPT_TIMEOUT);

                    // A plain click() was confirmed live to report success
                    // without ever actually dismissing this sheet — see
                    // ElementActions.pressAndRelease()'s own doc comment.
                    ElementActions.pressAndRelease(ConsentPromptLocators.AGREE_AND_CONTINUE);
                    handled++;
                    clearSince = null;

                    awaitGone(title, Duration.ofSeconds(SHEET_LEAVES_WITHIN_SECONDS));

                } catch (TimeoutException notClickableYet) {

                    // Covered by an alert, most likely — looked at again
                    // next pass, once the Save Password branch (or simply
                    // more time) has cleared it.
                }

                continue;
            }

            if (WaitUtils.isVisibleNow(landed)) {

                if (clearSince == null) {

                    clearSince = Instant.now();

                } else if (Duration.between(clearSince, Instant.now()).compareTo(QUIET_FOR) >= 0) {

                    return handled;
                }

            } else {

                clearSince = null;
            }

            try {

                Thread.sleep(POLL_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
                return handled;
            }
        }

        throw new IllegalStateException(
                "The app did not land on " + landed + " with nothing on top of it within "
                        + LANDED_WITHIN.toSeconds() + "s of signing in (" + handled
                        + " prompt(s) answered)");
    }

    /**
     * Polls for the element to leave the screen, and gives up quietly: a
     * sheet that is still there is dealt with by the next pass of the
     * loop, not by failing here.
     */
    private static void awaitGone(By locator, Duration within) {

        Instant deadline = Instant.now().plus(within);

        while (Instant.now().isBefore(deadline) && WaitUtils.isVisibleNow(locator)) {

            try {

                Thread.sleep(POLL_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
                return;
            }
        }
    }

}
