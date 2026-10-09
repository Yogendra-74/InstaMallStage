package com.instamall.pages.profile;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Profile landing screen (the Profile tab). Verified against
 * test-output/exploration/profile-landing.xml.
 *
 * Same tree rules as the rest of the app: containers aggregate the names
 * of everything inside them and are accessible == 0, real controls are
 * accessible == 1. The menu rows render as XCUIElementTypeOther, not
 * Button, so they are matched by name + accessibility alone.
 *
 * ACCOUNT SETTINGS and LOGOUT sit below the fold on this screen — they
 * are in the tree from the start but visible == 0 until scrolled to.
 */
public final class ProfilePageLocators {

    private ProfilePageLocators() {

    }

    // ---------- Header ----------

    /**
     * The signed-in user's display name, as configured for the profile
     * account. Rendered as a plain StaticText under the INSTAMALL header.
     */
    /**
     * The display name, matched loosely on whitespace: the header prints
     * it with stray spaces ("  Sudhanshu   Ranjan"), so each gap in the
     * expected name is allowed to be any run of spaces.
     */
    public static By userName(String displayName) {

        String pattern = "\\\\s*"
                + String.join("\\\\s+", displayName.trim().split("\\s+"))
                + "\\\\s*";

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name MATCHES '"
                        + pattern + "' AND accessible == 1");
    }

    public static By userEmail(String email) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '"
                        + email + "' AND accessible == 1");
    }

    // ---------- Stats row (counts are live data, so match the suffix) ----------

    public static final By STAT_WISHLIST =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'WISHLIST ' AND accessible == 1");

    public static final By STAT_ORDERS =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'ORDERS ' AND accessible == 1");

    /**
     * Reads "$0.00 WALLET" — the name carries the balance, so the page
     * exposes it for assertions rather than just presence.
     */
    public static final By STAT_WALLET =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'WALLET ' AND accessible == 1");

    // ---------- Referral card ----------

    public static final By REFERRAL_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Give $25, Get $25' AND accessible == 1");

    /**
     * The referral code itself — eight uppercase letters/digits on its own
     * StaticText ("ZCRYFXCM"). Nothing else on this screen matches that
     * shape: the stat counts are short numbers and the wallet reads "$…".
     */
    public static final By REFERRAL_CODE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name MATCHES '^[A-Z0-9]{8}$' AND accessible == 1");

    public static final By COPY_REFERRAL_CODE_BUTTON =
            AppiumBy.accessibilityId("referral_copy_code_button");

    public static final By INVITE_FRIENDS_BUTTON =
            AppiumBy.accessibilityId("referral_invite_friends_button");

    public static final By REFERRAL_QR_CODE_BUTTON =
            AppiumBy.accessibilityId("referral_qr_code_button");

    // ---------- Menu rows ----------

    public static final By MENU_MY_PROFILE = menuRow("My profile");

    public static final By MENU_MANAGE_ADDRESSES = menuRow("Manage addresses");

    public static final By MENU_ACCOUNT_SETTINGS = menuRow("Account settings");

    public static final By MENU_HELP_SUPPORT = menuRow("Help & support");

    public static final By MENU_ABOUT_LEGAL = menuRow("About & legal");

    public static final By MENU_LOGOUT = menuRow("Log out");

    // ---------- Logout confirmation dialog ----------

    public static final By LOGOUT_CONFIRM_MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Are you sure you want to log out?' AND accessible == 1");

    public static final By LOGOUT_CANCEL =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Cancel' AND accessible == 1");

    public static final By LOGOUT_CONFIRM =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Yes, Logout' AND accessible == 1");

    // Rows are Buttons in sentence case now ("My profile", "Log out");
    // matched case-insensitively so older callers' capitals still land.
    public static By menuRow(String title) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name ==[c] '" + title + "' AND accessible == 1");
    }

    /**
     * Same row, but only while it is actually on screen — for tapping
     * after a scroll, where the tree still holds the off-screen copy.
     */
    public static By visibleMenuRow(String title) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name ==[c] '" + title + "'"
                        + " AND accessible == 1 AND visible == 1");
    }

}
