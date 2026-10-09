package com.instamall.pages.login;

import com.instamall.components.navigation.HeaderLocators;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * "Continue with email instead" variant of the login screen. Verified
 * against a page source dump taken from the live screen.
 */
public final class EmailLoginPageLocators {

    private EmailLoginPageLocators() {

    }

    /**
     * Both inputs carry no accessible name — only a placeholder
     * ("Enter your email" / "Enter your password") — so they are matched
     * by type. That is unambiguous here: the screen has exactly one
     * TextField and one SecureTextField.
     */
    public static final By EMAIL_TEXTBOX =
            AppiumBy.className("XCUIElementTypeTextField");

    public static final By PASSWORD_TEXTBOX =
            AppiumBy.className("XCUIElementTypeSecureTextField");

    /**
     * Continue is an XCUIElementTypeOther, not a Button, and starts out
     * enabled == false. It flips to enabled once both fields hold text
     * and the consent box is ticked — confirmed on the device — so
     * waiting for it to become enabled is a valid gate.
     */
    public static final By NEXT_BUTTON =
            AppiumBy.accessibilityId("Continue");

    /**
     * Messaging-consent checkbox. Same control as on the phone screen,
     * but here it only appears once both fields are filled. Its value
     * reads "checkbox, unchecked" / "checkbox, checked".
     */
    public static final By CONSENT_CHECKBOX = LoginPageLocators.CONSENT_CHECKBOX;

    /**
     * The screen headline. Tapping it is how the keyboard is put away:
     * the app offers no Done key, WDA's hideKeyboard cannot dismiss it,
     * and while it is up it covers the consent box and Continue.
     */
    public static final By HEADLINE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Your personal edit awaits.' AND accessible == 1"
            );

    public static final By CONTINUE_WITH_PHONE =
            AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeStaticText[`name == \"Continue with phone instead\" AND accessible == true`]"
            );

    public static final By FORGOT_PASSWORD =
            AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeStaticText[`name == \"Forgot password?\" AND accessible == true`]"
            );

    // The old "Go Back" link is gone; the header chevron is the way back.
    public static final By GO_BACK = HeaderLocators.BACK_CHEVRON;

    /**
     * Error text after the backend rejects the credentials.
     *
     * VERIFY: this is the one locator on the screen still unconfirmed —
     * checking it means a deliberately failed sign-in, which was skipped
     * to avoid tripping account lockout on the shared staging user.
     */
    public static final By INVALID_CREDENTIALS_ERROR =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name CONTAINS[c] 'invalid' AND accessible == 1"
            );

    // NOTE: three controls on this screen are unnamed, unlabelled Others
    // and so cannot be targeted dependably — the show/hide password eye
    // next to the password field, and the two "OR CONTINUE WITH" social
    // sign-in buttons. They need accessibility ids from the app team before any
    // test can use them.

}
