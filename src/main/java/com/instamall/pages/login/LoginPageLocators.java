package com.instamall.pages.login;

import com.instamall.components.navigation.HeaderLocators;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

public final class LoginPageLocators {

    private LoginPageLocators() {

    }

    // Only TextField on this screen — unique by type, immune to
    // placeholder text changes.
    public static final By PHONE_NUMBER_TEXTBOX =
            AppiumBy.className("XCUIElementTypeTextField");

    // Name changes with selected country ("🇺🇸 +1" → "🇮🇳 +91" etc.),
    // so match the pattern instead of the exact value.
    public static final By COUNTRY_CODE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name CONTAINS '+' AND accessible == 1"
            );

    // An Other, not a Button, and starts out enabled == false until a
    // full number is in.
    public static final By SEND_OTP_BUTTON =
            AppiumBy.accessibilityId("Send code");

    /**
     * The messaging-consent checkbox under the number. One Other carries
     * the whole paragraph as its name, and its value reports the state:
     * "checkbox, unchecked" / "checkbox, checked". Tapping it toggles the
     * box — the Terms and Privacy links inside are not hit by a tap on
     * the element's centre.
     */
    public static final By CONSENT_CHECKBOX =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name BEGINSWITH 'I agree that InstaMall' AND accessible == 1"
            );

    /**
     * The way on to the email sign-in screen. Confirmed live on the
     * current build: a real Button reading "Continue with Email", one of
     * four on the landing screen (Apple, Google, Email, "Show more
     * options") under the phone-number field. An earlier build put a
     * "Continue with email instead" StaticText here instead.
     */
    public static final By CONTINUE_WITH_EMAIL =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Continue with Email' AND accessible == 1"
            );

    // The old "Go Back" link is gone; the header chevron is the way back.
    // (The phone screen no longer offers "Forgot password?" either — that
    // lives on the email screen only.)
    public static final By GO_BACK = HeaderLocators.BACK_CHEVRON;

}