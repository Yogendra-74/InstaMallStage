package com.instamall.pages.payment;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Payment sheet. Verified against a page source dump.
 *
 * This sheet comes from the payment provider's SDK, not from the RN app:
 * its controls are real XCUIElementTypeButtons with stable ids, which
 * makes it the most reliable screen in the flow to automate.
 */
public final class PaymentSheetLocators {

    private PaymentSheetLocators() {

    }

    /**
     * Staging marker. If this is missing, the sheet is live — a test must
     * not pay.
     */
    public static final By TEST_MODE_BADGE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'TEST'"
            );

    public static final By CLOSE_BUTTON =
            AppiumBy.accessibilityId("UIButton.Close");

    /**
     * Saved card button. Its name is masked with bullets ("•••• 4242") and
     * its label spells the digits out ("Visa ending in 4 2 4 2"), so the
     * last four digits are matched inside the name.
     *
     * The button's value is "1" and its traits include "Selected" when it
     * is the chosen method — see {@link #SELECTED_CARD}.
     */
    public static By savedCard(String lastFourDigits) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS '" + lastFourDigits + "'"
        );
    }

    public static final By SELECTED_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND value == '1'"
            );

    /**
     * The pencil beside a saved card. Confirmed live: it is present only
     * once a card is on the account, under a "Saved" heading beside the
     * masked number — which makes it the tell for "there is a card here
     * already, just pay" as against "no card, type one in".
     */
    public static final By EDIT_SAVED_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Edit' AND accessible == 1"
            );

    // ---------- Entering a card, when none is saved ----------

    public static final By CARD_NUMBER_FIELD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND name == 'Card number'"
            );

    public static final By CARD_EXPIRY_FIELD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND name == 'expiration date'"
            );

    public static final By CARD_CVC_FIELD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND name == 'CVC'"
            );

    // Name carries the payable amount, e.g. "Pay $962.49" — matched on
    // the fixed part only, since the amount differs on every order.
    public static final By PAY_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Pay $'"
            );

}
