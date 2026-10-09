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
     * A card whose order is ready for the driver handoff — its CTA reads
     * "START HANDOFF". Kept separate from ANY_ACTIVE_ORDER_CARD rather
     * than folded into it: confirmed live that a "READY FOR HANDOFF" card
     * and an "UP NEXT"/"NEW ORDER" one can both be on screen at once (one
     * order further along than another), and the generic locator would
     * then match more than one card with no guaranteed order.
     */
    public static final By READY_FOR_HANDOFF_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name CONTAINS 'READY FOR HANDOFF'"
                            + " AND accessible == 1"
            );

    /**
     * A history order's card — same shape, prefixed "ORDER DELIVERED",
     * "ORDER CANCELLED" or "ORDER REJECTED" instead.
     */
    public static final By ANY_HISTORY_ORDER_CARD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name BEGINSWITH 'ORDER ' AND accessible == 1"
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

    public static final By SHIFT_STATUS_TOGGLE =
            AppiumBy.accessibilityId("View shift status options");

    /**
     * Confirmed against a page source dump taken live: a shopper who logs
     * in offline is asked this before the dashboard is usable at all —
     * every element behind it, including {@link #ACTIVE_TAB}, reports
     * visible == false for as long as it is up, which is what made a
     * fresh login time out waiting for the dashboard with no indication
     * why. Not a system alert — an XCUIElementTypeOther sheet the app
     * itself renders, so SystemAlerts.grantAnyShowingNow() never touches
     * it.
     */
    public static final By GO_ON_SHIFT_SHEET_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Go on shift?' AND accessible == 1"
            );

    public static final By GO_ON_SHIFT_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name == 'Go on shift'"
                            + " AND accessible == 1 AND visible == 1"
            );

    public static final By SKIP_FOR_NOW_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name == 'Skip for now' AND accessible == 1"
            );

}
