package com.instamall.components.consent;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

public final class ConsentPromptLocators {

    private ConsentPromptLocators() {

    }

    // Two sheets arrive one after the other on a fresh sign in, each with
    // its own title and the same button.
    public static final By AI_BIOMETRIC_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'AI & Biometric Consent'"
                            + " AND accessible == 1 AND visible == 1"
            );

    public static final By AI_STYLIST_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'AI Stylist Consent'"
                            + " AND accessible == 1 AND visible == 1"
            );

    // Enabled from the start — the sheet does not make you scroll the
    // text first.
    public static final By AGREE_AND_CONTINUE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Agree & Continue' AND accessible == 1 AND visible == 1"
            );

    /**
     * iOS offers to store the password right after a sign in. Tapped to
     * save it, per instruction to allow every save-password prompt this
     * suite meets — this locator used to target "Not Now" instead, which
     * would have declined it every time autoDismissAlerts was off for a
     * Customer-app run; caught before any run actually exercised that
     * path with the wrong button.
     */
    public static final By SAVE_PASSWORD_SAVE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Save'"
            );

}
