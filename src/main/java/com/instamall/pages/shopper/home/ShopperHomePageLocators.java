package com.instamall.pages.shopper.home;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * The Shopper app's dashboard — reached after login. Lists orders this
 * shopper is (or has been) fulfilling, under two tabs: ACTIVE and HISTORY.
 * Verified against a page source dump taken from the live screen.
 */
public final class ShopperHomePageLocators {

    private ShopperHomePageLocators() {

    }

    /**
     * Reads "PERSONAL SHOPPER" with a line break baked into the name
     * ("PERSONAL\nSHOPPER") — matched by prefix rather than reproducing
     * that literal newline in a predicate string.
     */
    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'PERSONAL' AND accessible == 1"
            );

    /**
     * Reads "ACTIVE, &lt;count&gt;" — the count changes as orders are
     * assigned or completed, so this is matched as a prefix.
     */
    public static final By ACTIVE_TAB =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'ACTIVE' AND accessible == 1"
            );

    public static final By HISTORY_TAB =
            AppiumBy.accessibilityId("HISTORY");

    /**
     * An active order's card. Its accessible name is the whole card read
     * out in one string — status, ETA, fulfilment type, customer first
     * name, store, and total — e.g. "IN PROGRESS, 38:33, DELIVERY,
     * Currently shopping for, PAY WITH CARD, Sudhanshu, Acne Studios ·
     * Miami Florida, $374.99, RUN PROGRESS · 1 items, IN PROGRESS", or,
     * for one not yet started, "✦, NEW ORDER, DELIVERY, PAY WITH CARD,
     * Rahul, Prada · Miami Florida, $7,325.99". No order id is rendered
     * anywhere on this screen or its detail view.
     */
    public static final By ANY_ACTIVE_ORDER_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther'"
                            + " AND (name CONTAINS 'IN PROGRESS' OR name CONTAINS 'NEW ORDER'"
                            + " OR name CONTAINS 'UP NEXT' OR name CONTAINS 'VIEW DETAILS')"
                            + " AND accessible == 1"
            );

    /**
     * An order already Got-it'd/Accepted, singled out from the generic
     * locator above. Confirmed live: that order's own "IN PROGRESS" card
     * stays on screen alongside a second, read-only "NEW ORDER" card for
     * the very same order under "UP NEXT" (same store, same total, same
     * order id once opened) — so both match ANY_ACTIVE_ORDER_CARD at
     * once, and findElements()'s undefined ordering can hand back the
     * inert preview card instead of the actionable one, which opens a
     * dead end with no Validate-and-proceed button.
     *
     * "VIEW DETAILS" is this same actionable card's CTA once the run has
     * moved past item acceptance — confirmed live on the
     * camera-bypass-enabled Shopper build — so it is matched as an
     * alternative to "IN PROGRESS" rather than a separate case, both
     * opening the identical actionable card by the same tap.
     */
    public static final By IN_PROGRESS_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther'"
                            + " AND (name CONTAINS 'IN PROGRESS' OR name CONTAINS 'VIEW DETAILS')"
                            + " AND accessible == 1"
            );

    /**
     * The first listing while the shopper is ON SHIFT with a new order
     * waiting. Confirmed live: one leaf Other with no children, named
     * "NEW ORDER, DELIVERY, New order for, PAY WITH CARD, Duel, Burberry
     * · Miami FL, $77.1, VIEW ORDER" — the "VIEW ORDER" button is fused
     * into that name, not a separate element, so it is reached by the
     * same bottom-biased tap as every other card CTA (see
     * ShopperHomePage.tapCardCta()).
     */
    public static final By VIEW_ORDER_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name CONTAINS 'VIEW ORDER'"
                            + " AND accessible == 1"
            );

    /**
     * A card whose order is ready for the driver handoff — its CTA reads
     * "START HANDOFF". Kept separate from ANY_ACTIVE_ORDER_CARD rather
     * than folded into it: confirmed live that a "READY FOR HANDOFF" card
     * and an "UP NEXT"/"NEW ORDER" one can both be on screen at once (one
     * order further along than another), and the generic locator would
     * then match more than one card with no guaranteed order.
     */
    /**
     * The order's card once its receipt is submitted and before a driver
     * is assigned on the Delivery Portal — confirmed live right after
     * the "Order processed" OK: "WAITING FOR DRIVER, 21:47, DELIVERY,
     * Awaiting driver for, PAY WITH CARD, Duel, Burberry · Miami FL,
     * $77.1, WAITING FOR DRIVER, DRIVER ASSIGNED, PERSONAL SHOPPER".
     */
    public static final By WAITING_FOR_DRIVER_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name CONTAINS 'WAITING FOR DRIVER'"
                            + " AND accessible == 1"
            );

    public static final By READY_FOR_HANDOFF_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name CONTAINS 'READY FOR HANDOFF'"
                            + " AND accessible == 1"
            );

    /**
     * What the Active tab shows once every one of this shopper's orders
     * has left it — confirmed live right after a single-item order's only
     * item was rejected, which cancels the order itself (see
     * OrderDetailPageLocators.ORDER_PROCESSED_TITLE's own doc comment)
     * and moves it to History as "ORDER REJECTED" instead.
     */
    public static final By NO_ACTIVE_ORDERS_MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'No active orders.' AND accessible == 1"
            );

    /**
     * An order card containing every one of the given fragments — e.g.
     * orderCardContaining("$374.99", "Currently shopping for") to find the
     * card for a specific order placed on the Customer app, matched by the
     * amount and customer name it echoes since no order id is shown here.
     */
    public static By orderCardContaining(String... fragments) {

        StringBuilder predicate = new StringBuilder(
                "type == 'XCUIElementTypeOther' AND accessible == 1");

        for (String fragment : fragments) {

            predicate.append(" AND name CONTAINS '").append(fragment).append('\'');
        }

        return AppiumBy.iOSNsPredicateString(predicate.toString());
    }

    // ---------- Bottom tab bar ----------

    public static final By TAB_HOME =
            AppiumBy.accessibilityId("Home");

    public static final By TAB_RETURNS =
            AppiumBy.accessibilityId("Returns");

    public static final By TAB_PROFILE =
            AppiumBy.accessibilityId("Profile");

    // ---------- Shift status ----------

    /**
     * The shift badge itself. Its accessible name never changes with
     * state ("View shift status options" whatever it displays), so this
     * only ever identifies the badge — what it reads is sampled from its
     * status dot in ShopperShift.readStatus().
     */
    public static final By SHIFT_STATUS_BADGE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'View shift status options'"
                            + " AND accessible == 1"
            );

    /**
     * Confirmed live against a real "On break" state: the same button
     * name is used both on the options sheet the badge opens and on the
     * "Resume shift?" confirmation that follows it.
     */
    public static final By RESUME_SHIFT_BUTTON =
            AppiumBy.iOSNsPredicateString("name == 'Resume shift' AND accessible == 1");

    public static final By GO_ON_SHIFT_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name == 'Go on shift'"
                            + " AND accessible == 1 AND visible == 1"
            );

}
