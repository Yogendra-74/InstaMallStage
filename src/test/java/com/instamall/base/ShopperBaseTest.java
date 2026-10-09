package com.instamall.base;

import com.instamall.components.permissions.SystemAlerts;
import com.instamall.pages.shopper.home.ShopperHomePage;
import com.instamall.pages.shopper.login.ShopperLoginPageLocators;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.TimeoutException;

import java.time.Duration;

/**
 * Base for tests driving the Shopper app — the personal-shopper app that
 * fulfills an order a Customer has placed. Installed on the same simulator
 * as the Customer app, and reached the same way BaseTest reaches that one:
 * this only swaps which app's config keys the session lifecycle reads.
 */
public class ShopperBaseTest extends BaseTest {

    /** Cold start: bundle load plus splash before the first screen paints. */
    private static final Duration FIRST_SCREEN_TIMEOUT = Duration.ofSeconds(45);

    /**
     * Every Shopper app test grants permission prompts (location,
     * notifications, camera) rather than having iOS auto-decline them, and
     * taps Save on the Save Password alert — ShopperLoginPage's own
     * awaitDashboardPastPrompts() already answers whatever comes up during
     * login this way. Scoped to this one base class, so the Customer app's
     * tests are unaffected.
     */
    @Override
    protected boolean autoDismissAlerts() {

        return false;
    }

    @Override
    protected String appPathKey() {

        return "shopperAppPath";
    }

    @Override
    protected String bundleIdKey() {

        return "shopperBundleId";
    }

    /**
     * Waits for whichever of the app's two opening screens comes up —
     * unlike the Customer app, this one's own session was not found to
     * survive a relaunch, so a fresh launch has so far always meant the
     * login screen, but both are watched all the same rather than assumed.
     *
     * Also grants any permission alert that shows up here, same as
     * ShopperLoginPage's own login-time polling — a relaunch can raise one
     * (e.g. notifications) before any test method gets a chance to log in
     * and answer it itself. Left unanswered, it belongs to springboard,
     * not the app, so it survives being terminated and relaunched
     * underneath it and silently blocks every element lookup in whichever
     * @Test runs next (confirmed live).
     */
    @Override
    protected void awaitRelaunchFirstScreen() {

        try {

            WaitUtils.waitUntil(driver -> {

                        SystemAlerts.grantAnyShowingNow();

                        return WaitUtils.isVisibleNow(ShopperLoginPageLocators.LOG_IN_BUTTON)
                                        || ShopperHomePage.isDisplayedNow()
                                ? Boolean.TRUE
                                : null;
                    },
                    FIRST_SCREEN_TIMEOUT);

        } catch (TimeoutException neitherAppeared) {

            System.out.println(">>> Neither the login screen nor the dashboard showed within "
                    + FIRST_SCREEN_TIMEOUT.toSeconds() + "s of relaunch — carrying on.");
        }
    }

}
