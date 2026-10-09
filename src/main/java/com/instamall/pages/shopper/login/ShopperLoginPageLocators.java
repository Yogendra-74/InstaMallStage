package com.instamall.pages.shopper.login;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * The Shopper app's only opening screen — it has no onboarding step, unlike
 * the Customer app: a fresh launch goes straight to this login form.
 * Verified against a page source dump taken from the live screen.
 */
public final class ShopperLoginPageLocators {

    private ShopperLoginPageLocators() {

    }

    public static final By APP_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'IM Shopper' AND accessible == 1"
            );

    /**
     * Both inputs carry no accessible name — only a placeholder ("Email
     * address" / "Password") — so they are matched by type. That is
     * unambiguous here: the screen has exactly one TextField and one
     * SecureTextField.
     */
    public static final By EMAIL_TEXTBOX =
            AppiumBy.className("XCUIElementTypeTextField");

    public static final By PASSWORD_TEXTBOX =
            AppiumBy.className("XCUIElementTypeSecureTextField");

    /**
     * An XCUIElementTypeOther, not a Button — starts disabled (traits
     * "NotEnabled") until both fields hold text.
     */
    public static final By LOG_IN_BUTTON =
            AppiumBy.accessibilityId("Log in");

    public static final By FORGOT_PASSWORD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Forgot password?' AND accessible == 1"
            );

    // NOTE: the password field's show/hide eye is an unnamed, unlabelled
    // Other (same shape as the Customer app's) — cannot be targeted
    // dependably without an accessibility id from the app team.

}
