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
 */
public final class CheckoutPageLocators {

    private CheckoutPageLocators() {

    }

    // The "BACK" link is gone; the header chevron is the way back.
    public static final By BACK_BUTTON = HeaderLocators.BACK_CHEVRON;

    public static final By DELIVER_TO_HEADER =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Deliver to' AND accessible == 1"
            );

    public static final By SWITCH_TO_PICKUP =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Switch to pickup' AND accessible == 1"
            );

    /**
     * The header a checkout opened for collection carries, where a
     * delivery one carries "Deliver to".
     *
     * A store group already set for pickup opens this way, and then there
     * is no delivery section, no Priority option and no "Switch to pickup"
     * anywhere on the screen — so a flow that assumes the delivery form
     * reads a perfectly good checkout as one that never loaded.
     */
    public static final By PICKUP_FROM_HEADER =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Pickup from' AND accessible == 1"
            );

    /**
     * The way back to delivery, which only a pickup checkout offers — the
     * counterpart of SWITCH_TO_PICKUP, and the clearest sign of which mode
     * the screen is in.
     */
    public static final By SWITCH_TO_DELIVERY =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Switch to delivery' AND accessible == 1"
            );

    // ---------- Delivery speed ----------

    // Names carry fee and window: "Priority $14.99 7:21am–7:38am".
    public static final By PRIORITY_OPTION =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Priority' AND accessible == 1"
            );

    public static final By SCHEDULE_OPTION =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Schedule' AND accessible == 1"
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
     * Delivery address row: "Z9 Ventures 38 NW 24th St, Miami, FL 33127,
     * USA".
     *
     * Case-insensitive on purpose — the same address is uppercase on the
     * home header and in the saved-address list ("Z9 VENTURES") but title
     * case here.
     */
    public static By addressName(String name) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Deliver to.'"
                        + " AND name CONTAINS[c] '" + name + "' AND accessible == 1"
        );
    }

    // ---------- Tip ----------

    public static final By TIP_HEADER =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Tip to Your Delivery Driver' AND accessible == 1"
            );

    // ---------- Charge breakdown ----------

    /**
     * Charge label: "Subtotal", "Delivery Fee", "Tax", "Service Fee",
     * "Tip", "Total".
     */
    public static By chargeLabel(String label) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + label + "' AND accessible == 1"
        );
    }

    /**
     * Every charge amount, in render order: subtotal, delivery fee, tax,
     * service fee, tip, total.
     *
     * The type filter excludes the tip chips ("$5"…), and accessible == 1
     * keeps each amount from being counted twice — the app nests every
     * StaticText inside an identically named wrapper.
     *
     * These rows sit below the fold, so they report visible == false until
     * the page is scrolled. Read them by attribute rather than requiring
     * visibility.
     */
    /**
     * Every row label of the summary — "Subtotal", "Delivery", "Estimated
     * tax", "Service fee", "Small cart fee", "Tip", "Total" — so the
     * amounts can be paired with them by line. Amounts alone are no longer
     * enough: the store group above the summary prints "$14.99 delivery"
     * and each item its own price, all of them StaticTexts starting with
     * "$".
     *
     * Small cart fee: confirmed live, sits between Service fee and Tip.
     * Charged on a small order — this suite's own $16.00-subtotal single
     * item always carries it — and was missing from this list entirely,
     * which silently dropped it from every reconciliation sum while Total
     * kept including it: a checkout with $77.10 as its Total summed its
     * named rows to $67.11, short by exactly this fee.
     */
    public static final By ALL_CHARGE_LABELS =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND accessible == 1"
                            + " AND name IN {'Subtotal', 'Delivery', 'Estimated tax', 'Service fee',"
                            + " 'Small cart fee', 'Tip', 'Total'}"
            );

    public static final By ALL_CHARGE_AMOUNTS =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH '$' AND accessible == 1"
            );

    // ---------- Place order ----------

    /**
     * The button's copy follows the delivery speed that is selected:
     * "Schedule Delivery" while Schedule is chosen — which is how the
     * screen opens — and "Get it Today" once Priority is picked. Matching
     * only one of the two makes the screen look like it never loaded.
     */
    public static final By PLACE_ORDER_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "(name BEGINSWITH 'Get it' OR name BEGINSWITH 'Schedule Delivery')"
                            + " AND accessible == 1 AND visible == 1"
            );

    /**
     * The Priority flavour of that button, for asserting the copy actually
     * flips when the speed changes.
     */
    public static final By GET_IT_TODAY_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Get it' AND accessible == 1 AND visible == 1"
            );

}
