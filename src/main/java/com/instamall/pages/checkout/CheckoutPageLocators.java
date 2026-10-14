package com.instamall.pages.checkout;

import com.instamall.components.navigation.HeaderLocators;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;


/**
 * Checkout screen. Verified against a page source dump.
 *
 * Worth knowing: the full charge breakdown (subtotal, delivery, tax,
 * service fee, tip, total) lives on THIS screen, below the tip selector.
 * The order confirmation screen only shows the total.
 *
 * Two kinds of thing live here. The {@link By} locators are what gets
 * tapped or waited on. The plain-string names and prefixes are what
 * CheckoutPage.read() matches against in a single page-source snapshot
 * — one call for every question the flow asks of this screen, where a
 * locator apiece used to cost a WebDriver round trip apiece (see
 * CheckoutPage.Screen's own doc comment for the numbers).
 */
public final class CheckoutPageLocators {

    private CheckoutPageLocators() {

    }

    // The "BACK" link is gone; the header chevron is the way back.
    public static final By BACK_BUTTON = HeaderLocators.BACK_CHEVRON;

    // ---------- Mode: delivery or pickup ----------

    /** The header a delivery checkout carries. */
    public static final String DELIVER_TO_HEADER_TEXT = "Deliver to";

    /**
     * The header a checkout opened for collection carries, where a
     * delivery one carries "Deliver to".
     *
     * A store group already set for pickup opens this way, and then there
     * is no delivery section, no Priority option and no "Switch to pickup"
     * anywhere on the screen — so a flow that assumes the delivery form
     * reads a perfectly good checkout as one that never loaded.
     */
    public static final String PICKUP_FROM_HEADER_TEXT = "Pickup from";

    /**
     * The way back to delivery, which only a pickup checkout offers — the
     * counterpart of SWITCH_TO_PICKUP, and the clearest sign of which mode
     * the screen is in.
     */
    public static final String SWITCH_TO_DELIVERY_TEXT = "Switch to delivery";

    public static final By SWITCH_TO_PICKUP =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Switch to pickup' AND accessible == 1"
            );

    // ---------- Delivery speed ----------

    // Names carry fee and window: "Priority $14.99 7:21am–7:38am".
    public static final String PRIORITY_PREFIX = "Priority";
    public static final String SCHEDULE_PREFIX = "Schedule";

    public static final By PRIORITY_OPTION =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH '" + PRIORITY_PREFIX + "' AND accessible == 1"
            );

    /**
     * Any slot the screen offers to collect or receive the order in.
     *
     * Matched on the clock time in the label — "Priority $14.99
     * 1:49am–2:06am", "Schedule $14.99 As soon as 11:14am today", and the
     * pickup windows that replace them once pickup is chosen all carry
     * one.
     */
    public static final By ANY_TIME_SLOT =
            AppiumBy.iOSNsPredicateString(
                    "(name CONTAINS 'am' OR name CONTAINS 'pm')"
                            + " AND accessible == 1 AND visible == 1"
            );

    /**
     * One slot, addressed by the full label {@link #ANY_TIME_SLOT}
     * reports.
     */
    public static By timeSlot(String label) {

        return AppiumBy.iOSNsPredicateString(
                "name == '" + label + "' AND accessible == 1 AND visible == 1"
        );
    }

    // ---------- Address / contact ----------

    /**
     * Delivery address row, a Button: "Deliver to. Z9 Ventures 38 NW
     * 24th St, Miami, FL 33127, USA". Matched by this prefix and then by
     * name inside CheckoutPage.Screen.isDeliveringTo(); a guest checkout,
     * which has no saved, named address to assert on, checks only that
     * the row populated at all.
     */
    public static final String ADDRESS_ROW_PREFIX = "Deliver to.";

    // ---------- Place order ----------

    /**
     * The button's copy follows the delivery speed that is selected:
     * "Schedule Delivery" while Schedule is chosen — which is how the
     * screen opens — and "Get it Today" once Priority is picked. Matching
     * only one of the two makes the screen look like it never loaded.
     */
    public static final String GET_IT_PREFIX = "Get it";
    public static final String SCHEDULE_DELIVERY_PREFIX = "Schedule Delivery";

    public static final By PLACE_ORDER_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "(name BEGINSWITH '" + GET_IT_PREFIX + "' OR name BEGINSWITH '" + SCHEDULE_DELIVERY_PREFIX + "')"
                            + " AND accessible == 1 AND visible == 1"
            );

    /**
     * The Priority flavour of that button, for waiting on the copy to
     * actually flip when the speed changes.
     */
    public static final By GET_IT_TODAY_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH '" + GET_IT_PREFIX + "' AND accessible == 1 AND visible == 1"
            );

}
