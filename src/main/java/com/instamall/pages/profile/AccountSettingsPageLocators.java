package com.instamall.pages.profile;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * ACCOUNT SETTINGS screen. Verified against
 * test-output/exploration/profile-account-settings.xml.
 *
 * Email and phone are read-only StaticTexts here; the editable copies
 * live on EDIT PROFILE. The "Delete account" row is located but never
 * tapped by any test — it is the one action on this screen that a
 * throwaway account still cannot afford by accident.
 */
public final class AccountSettingsPageLocators {

    private AccountSettingsPageLocators() {

    }

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Account settings' AND accessible == 1");

    // ---------- YOUR ACCOUNT ----------

    public static final By SECTION_YOUR_ACCOUNT = staticText("Your account");

    public static final By EMAIL_LABEL = staticText("EMAIL");

    public static By emailValue(String email) {

        return staticText(email);
    }

    public static final By PHONE_LABEL = staticText("PHONE NUMBER");

    /** The account's phone as displayed, e.g. "+1 (845) 778-5788". */
    public static final By PHONE_VALUE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH '+' AND accessible == 1");

    // ---------- PRIVACY ----------

    public static final By SECTION_PRIVACY = staticText("Privacy");

    // ---------- ACCOUNT ACTIONS ----------

    public static final By SECTION_ACCOUNT_ACTIONS = staticText("Account actions");

    /** Never tapped by the suite. See the class comment. */
    public static final By DELETE_ACCOUNT_ROW =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Delete account' AND accessible == 1");

    // ---------- Footer ----------

    public static final By APP_VERSION =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH[c] 'App version' AND accessible == 1");

    private static By staticText(String name) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + name + "' AND accessible == 1");
    }

}
