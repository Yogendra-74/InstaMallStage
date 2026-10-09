package com.instamall.components.delivery;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * "Delivery not available at this time" popup, which the app raises for
 * products the current address cannot be served with.
 *
 * NOTE: written from the message text, not from a page source dump — no
 * run has captured this popup yet. The dismiss control is matched against
 * the labels such a dialog usually carries; if none of them fit,
 * DeliveryUnavailablePrompt reports what it did find so the list can be
 * corrected from real data rather than guessed at again.
 */
public final class DeliveryUnavailablePromptLocators {

    private DeliveryUnavailablePromptLocators() {

    }

    public static final By PROMPT =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS[c] 'elivery not available' AND visible == 1"
            );

    public static final By DISMISS_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name IN {'OK', 'Okay', 'Got it', 'GOT IT', 'Close', 'Dismiss',"
                            + " 'Cancel', 'Continue', 'Change Address', 'Try Another Store'}"
                            + " AND accessible == 1 AND visible == 1"
            );

    /**
     * Everything on screen, used only to describe an undismissable popup
     * in the failure message.
     */
    public static final By ANY_VISIBLE_CONTROL =
            AppiumBy.iOSNsPredicateString(
                    "accessible == 1 AND visible == 1"
            );

}
