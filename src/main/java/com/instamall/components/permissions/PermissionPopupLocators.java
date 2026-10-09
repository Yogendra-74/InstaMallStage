package com.instamall.components.permissions;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Buttons on iOS system permission alerts. These belong to springboard,
 * not to the app, so accessibility ids are stable across app builds.
 */
public final class PermissionPopupLocators {

    private PermissionPopupLocators(){}

    public static final By ALLOW =
            AppiumBy.accessibilityId("Allow");

    public static final By ASK_APP_NOT_TO_TRACK =
            AppiumBy.accessibilityId("Ask App Not to Track");

    // ---------- Location ----------

    public static final By ALLOW_ONCE =
            AppiumBy.accessibilityId("Allow Once");

    public static final By ALLOW_WHILE_USING_APP =
            AppiumBy.accessibilityId("Allow While Using App");

    // ---------- Notifications / generic deny ----------

    // Curly apostrophe: iOS renders "Don’t Allow", not "Don't Allow".
    public static final By DONT_ALLOW =
            AppiumBy.accessibilityId("Don’t Allow");

    // ---------- Save Password ----------

    // iOS offers to store the password right after a sign in, on every
    // app alike — same alert that was found covering "Agree & Continue"
    // on the Customer app's ConsentPrompts. "Save Password?" is the
    // alert's title, not a button — confirmed live (Shopper app login)
    // that its buttons are "Not Now" and "Save"; the old "Save Password"
    // accessibility id matched neither, so this alert sat unanswered.
    public static final By SAVE_PASSWORD =
            AppiumBy.accessibilityId("Save");
}
