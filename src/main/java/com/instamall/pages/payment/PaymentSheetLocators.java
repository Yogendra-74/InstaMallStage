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

    public static final By APPLE_PAY =
            AppiumBy.accessibilityId("apple_pay_button");

    public static final By OR_PAY_USING_DIVIDER =
            AppiumBy.accessibilityId("Or pay using");

    public static final By SAVED_SECTION =
            AppiumBy.accessibilityId("Saved");

    public static final By NEW_PAYMENT_METHOD_SECTION =
            AppiumBy.accessibilityId("New payment method");

    public static final By NEW_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'New card'"
            );

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

    public static final By EDIT_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Edit'"
            );

    // Name carries the payable amount: "Pay $962.49".
    public static final By PAY_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Pay $'"
            );

}
