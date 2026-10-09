package com.instamall.pages.profile;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * EDIT PROFILE screen, reached from MY PROFILE -> "Edit Account →".
 * Verified against test-output/exploration/profile-edit-account.xml.
 *
 * None of the four inputs carries a name or accessibility id — the only
 * handle is document order, so the fields are addressed by index. The
 * order today: first name, last name, phone, email. Ask the app team for
 * accessibility ids; until then a reshuffled form breaks these silently.
 */
public final class EditAccountPageLocators {

    private EditAccountPageLocators() {

    }

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'EDIT PROFILE' AND accessible == 1");

    public static final By FIRST_NAME_FIELD = textField(1);

    public static final By LAST_NAME_FIELD = textField(2);

    public static final By PHONE_FIELD = textField(3);

    public static final By EMAIL_FIELD = textField(4);

    public static final By SAVE_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == 'SAVE' AND accessible == 1");

    private static By textField(int position) {

        return AppiumBy.iOSClassChain(
                "**/XCUIElementTypeTextField[" + position + "]");
    }

}
