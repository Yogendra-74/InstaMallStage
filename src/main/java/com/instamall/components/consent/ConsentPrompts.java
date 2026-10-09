package com.instamall.components.consent;

import com.instamall.components.permissions.PermissionPopupLocators;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import java.time.Duration;
import java.time.Instant;

/**
 * Clears what stands between a fresh sign in and the app itself: the iOS
 * "Save Password?" alert, the App Tracking Transparency alert, then the
 * "AI & Biometric Consent" and "AI Stylist Consent" sheets, each accepted
 * with Agree & Continue.
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

            if (WaitUtils.isVisibleNow(ConsentPromptLocators.SAVE_PASSWORD_SAVE)) {

                WaitUtils.waitForClickable(ConsentPromptLocators.SAVE_PASSWORD_SAVE).click();
                handled++;
                clearSince = null;
                continue;
            }

            // App Tracking Transparency — not covered by any pre-grant
            // (location and notifications are, via AppSession, but iOS
            // reserves ATT from that mechanism), so with autoDismissAlerts
            // off this is a real alert nothing else here answers. Its own
            // affirmative button is generically named "Allow", same as
            // location/notifications' — safe to match on here since those
            // two never reach this loop as real alerts when pre-granted.
            if (WaitUtils.isVisibleNow(PermissionPopupLocators.ALLOW)) {

                WaitUtils.waitForClickable(PermissionPopupLocators.ALLOW).click();
                handled++;
                clearSince = null;
                continue;
            }

            if (WaitUtils.isVisibleNow(ConsentPromptLocators.AGREE_AND_CONTINUE)) {

                // Which sheet this is, so its leaving can be watched by
                // title: the next sheet slides in under the very same
                // button name, often before this one has finished going,
                // so the button itself never reads as gone.
                By title = WaitUtils.isVisibleNow(ConsentPromptLocators.AI_STYLIST_TITLE)
                        ? ConsentPromptLocators.AI_STYLIST_TITLE
                        : ConsentPromptLocators.AI_BIOMETRIC_TITLE;

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
                            CLICK_ATTEMPT_TIMEOUT).click();
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
