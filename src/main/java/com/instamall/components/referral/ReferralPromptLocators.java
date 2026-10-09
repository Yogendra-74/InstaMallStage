package com.instamall.components.referral;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * "Invite your friends, get rewards" sheet, which the app raises over the
 * order confirmation once an order is placed. Verified against a page
 * source dump.
 *
 * Unlike most of this app, the sheet carries real testIDs.
 */
public final class ReferralPromptLocators {

    private ReferralPromptLocators() {

    }

    public static final By SHEET =
            AppiumBy.iOSNsPredicateString(
                    "name == 'referral_program_sheet' AND visible == 1"
            );

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Invite your friends, get rewards'"
                            + " AND accessible == 1 AND visible == 1"
            );

    /**
     * The backdrop behind the sheet. Tapping it is the only way out: the
     * app gives the sheet no close, skip or "maybe later" control.
     *
     * It is the one accessible child of the sheet's wrapper, and it
     * stretches past the top of the screen (y = -162), so its centre lands
     * well above the sheet itself.
     */
    public static final By SCRIM =
            AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeOther[`name BEGINSWITH \"Invite your friends\"`]"
                            + "/XCUIElementTypeOther[`accessible == true`][1]"
            );

    public static final By INVITE_FRIENDS_BUTTON =
            AppiumBy.accessibilityId("referral_invite_friends_button");

}
