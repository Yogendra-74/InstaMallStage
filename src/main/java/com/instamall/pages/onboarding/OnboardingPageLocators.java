package com.instamall.pages.onboarding;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

public final class OnboardingPageLocators {

    private OnboardingPageLocators() {

    }

    // The sign-in entry is a single Button whose name is the whole
    // sentence, "Sign in" link included.
    public static final By LOGIN_BUTTON =
            AppiumBy.accessibilityId("Already have an account? Sign in");

    public static final By SIGNUP_BUTTON =
            AppiumBy.accessibilityId("Make it personal");

    // The guest entry point is back, in the slot the old
    // "Scan invite QR" button used to take.
    public static final By EXPLORE_AS_GUEST =
            AppiumBy.accessibilityId("Explore as a guest");

    // The wordmark is an image now, not text — matched by its asset path.
    public static final By APP_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeImage' AND name ENDSWITH 'logo_text@3x.png'"
            );

    // Rendered over two lines ("Fashion.\nOn your terms."), so only the
    // start is matched rather than the exact line break.
    public static final By APP_TAGLINE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH 'Fashion.' AND accessible == 1"
            );

}
