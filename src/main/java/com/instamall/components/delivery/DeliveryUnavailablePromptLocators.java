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

}
