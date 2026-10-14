package com.instamall.components.consent;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

public final class ConsentPromptLocators {

    private ConsentPromptLocators() {

    }

    // Two sheets arrive one after the other on a fresh sign in, each with
    // its own title and the same button: "Terms and Conditions" first,
    // then "AI & Biometric Consent" — confirmed live. AI_STYLIST_TITLE is
    // kept alongside them (rather than replaced) since it was the title
    // this suite was originally built against and may still show for some
    // accounts/flows; the loop that uses these only needs whichever title
    // is actually up, not all three at once.
    public static final By TERMS_AND_CONDITIONS_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Terms and Conditions'"
                            + " AND visible == 1"
            );

    public static final By AI_BIOMETRIC_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'AI & Biometric Consent'"
                            + " AND visible == 1"
            );

    public static final By AI_STYLIST_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'AI Stylist Consent'"
                            + " AND visible == 1"
            );

    // Enabled from the start — the sheet does not make you scroll the
    // text first.
    //
    // No "accessible == 1" here (unlike the two titles above) — confirmed
    // live that this button sat on screen through an entire 60s consent
    // wait without this predicate ever matching it, same accessible-flag
    // unreliability already called out elsewhere in this suite (e.g. the
    // Shopper app's shift status badge). "visible == 1" alone is enough to
    // tell it apart from whatever else is on screen.
    public static final By AGREE_AND_CONTINUE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Agree & Continue' AND visible == 1"
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
