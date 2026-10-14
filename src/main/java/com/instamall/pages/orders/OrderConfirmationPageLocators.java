package com.instamall.pages.orders;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Order confirmation / tracking screen. Verified against a page source
 * dump.
 *
 * This screen shows only the total; the itemised charges are on the
 * checkout screen (see CheckoutPage.getCharges()).
 */
public final class OrderConfirmationPageLocators {

    private OrderConfirmationPageLocators() {

    }

    // A Button reading "ORDER IM-0THZ8" (it was the text "Order: #IM-MT1PH").
    public static final By ORDER_ID =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'ORDER ' AND accessible == 1"
            );

    // A fresh delivery opens on "Order received" (it read IN PROGRESS
    // before the redesign).
    public static final By STATUS_IN_PROGRESS =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name IN {'Order received', 'IN PROGRESS'}"
                            + " AND accessible == 1"
            );

    /**
     * This screen doubles as the Model 2 order tracker: the same headline
     * element is reused across a live order's whole life, its text simply
     * changing as the order advances — "Order received" once placed,
     * "Order confirmed" once the boutique (Sales Associate) has confirmed
     * it. Further stages (Shopper assigned, driver states) have not been
     * observed yet.
     */
    public static final By STATUS_HEADLINE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                            + " AND name IN {'Order received', 'IN PROGRESS', 'Order confirmed'}"
                            + " AND accessible == 1"
            );

    /**
     * What the pill reads on a collection order. The two are exclusive:
     * a pickup order carries no "IN PROGRESS" anywhere on the screen.
     */
    // VERIFY: no collection order was placed on the redesigned build; a
    // pickup most likely opens on "Order received" too.
    public static final By STATUS_ORDER_PLACED =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name IN {'Order received', 'ORDER PLACED'}"
                            + " AND accessible == 1"
            );

    /**
     * The status subtext under the headline. Its wording changes with the
     * headline: "Your order is in - confirming with the boutique." while
     * "Order received", "Your piece is reserved. Your store is preparing
     * it." once "Order confirmed" — so this is matched loosely rather
     * than pinned to one sentence.
     */
    public static final By STATUS_MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                            + " AND (name BEGINSWITH 'Your order is' OR name BEGINSWITH 'Your piece is')"
                            + " AND accessible == 1"
            );

    /**
     * The longer explainer paragraph shown alongside "Order confirmed":
     * "Burberry has confirmed your pieces and is holding them for you.
     * We're matching you with a personal shopper to collect your order
     * and bring it to you." Names the store, so it is matched by shape
     * rather than by the exact sentence.
     *
     * VERIFY: only seen at the "Order confirmed" stage so far — whether
     * later stages (Shopper assigned, driver states) keep a paragraph
     * like this one is unconfirmed.
     */
    public static final By STATUS_EXPLAINER =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name CONTAINS 'confirmed your pieces'"
                            + " AND accessible == 1"
            );

    /**
     * The explainer paragraph shown at the earlier "Order received" stage,
     * before the boutique has confirmed anything: "The boutique is
     * confirming your pieces are in stock right now. As soon as they're
     * ready, we'll match you with a personal shopper to collect your
     * order and bring it to you." Distinct from {@link #STATUS_EXPLAINER}
     * (which reads "confirmed", past tense, once the boutique has acted)
     * — matched on "confirming" to tell the two stages apart.
     */
    public static final By STATUS_EXPLAINER_STILL_CONFIRMING =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name CONTAINS 'confirming your pieces'"
                            + " AND accessible == 1"
            );

    /**
     * The arrival-estimate block shown at the same fresh "Order received"
     * stage: "Estimated arrival — not final" over "Updating arrival time"
     * over "Locked in once the boutique confirms." All three disappear
     * once the boutique has confirmed and a real window replaces them.
     */
    public static final By ARRIVAL_NOT_FINAL_LABEL =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Estimated arrival — not final'"
                            + " AND accessible == 1"
            );

    public static final By UPDATING_ARRIVAL_TIME =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Updating arrival time'"
                            + " AND accessible == 1"
            );

    public static final By VISIT_STORE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Visit Store' AND accessible == 1"
            );

    public static By storeName(String brand) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + brand + "' AND accessible == 1"
        );
    }

    /**
     * "Your Order 1 Item" header.
     *
     * NOTE: the app marks it enabled == false / "NotEnabled", so it can be
     * read but never waited on for clickability.
     */
    public static final By YOUR_ORDER_SECTION =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Your Order' AND accessible == 1"
            );

    // The "Delivery Details" heading is gone; the address block is headed
    // "DELIVERING TO" now, well below the fold.
    public static final By DELIVERY_DETAILS_SECTION =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name IN {'DELIVERING TO', 'Delivery Details'}"
                            + " AND accessible == 1"
            );

    /**
     * Pickup's counterpart to Delivery Details — where the order is to be
     * collected from rather than sent to.
     */
    // VERIFY: not seen on the redesigned build — matched loosely.
    public static final By PICKUP_DETAILS_SECTION =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name CONTAINS[c] 'pick' AND name CONTAINS[c] 'from'"
                            + " AND accessible == 1"
            );

    public static final By DELIVERING_TO =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'DELIVERING TO' AND accessible == 1"
            );

    public static final By CONTINUE_SHOPPING =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Keep shopping' AND accessible == 1"
            );

    /**
     * One stage of the tracker: PLACED, SHOPPING, DISPATCH, ON THE WAY.
     */
    /**
     * VERIFY: the PLACED / SHOPPING / DISPATCH / ON THE WAY tracker was
     * not on the redesigned order screen, which opens on a status headline
     * and an arrival estimate instead.
     */
    public static By progressStage(String stage) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + stage + "' AND accessible == 1"
        );
    }

    public static By orderedItem(String productName) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name ==[c] '" + productName + "' AND accessible == 1"
        );
    }

    /**
     * Variant line under the ordered item: "Size: M | Color: Grain Beige |
     * Qty: 1".
     */
    public static By orderedItemVariant(String size, String color, int quantity) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == 'Size: " + size
                        + " | Color: " + color + " | Qty: " + quantity + "' AND accessible == 1"
        );
    }

    // ---------- Total bar ----------

    public static final By TOTAL_LABEL =
            AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeOther[`name BEGINSWITH \"Total $\"`]/XCUIElementTypeStaticText[`name == \"Total\"`]"
            );

    /**
     * The order total, scoped to the bottom bar so the item's line price
     * ("$750.00", further up the tree) cannot be picked up instead.
     */
    public static final By TOTAL_AMOUNT =
            AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeOther[`name BEGINSWITH \"Total $\"`]/XCUIElementTypeStaticText[`name BEGINSWITH \"$\"`]"
            );

    /**
     * The (i) control that opens the ORDER INFO breakdown sheet.
     *
     * NOTE: unnamed in the dump, so this targets it by position next to
     * the Total label. No page source for the sheet it opens was captured
     * yet, so nothing reads that sheet.
     */
    public static final By ORDER_INFO_TOGGLE =
            AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeOther[`name BEGINSWITH \"Total $\"`]"
                            + "/XCUIElementTypeOther[`accessible == true`][1]"
            );

}
