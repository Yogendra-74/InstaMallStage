package com.instamall.pages.bag;

import com.instamall.components.navigation.HeaderLocators;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * "Your bag" screen. Verified against page source dumps of the redesigned
 * build.
 *
 * The bag no longer wraps each item in a row element of its own. Every
 * piece of every item — title, variant, price, stepper, Save for later,
 * Remove — is a direct child of one list container, one after another,
 * and the store-group header and its "Review delivery &amp; checkout"
 * button sit in the same list. So there is nothing here that names "the
 * row for product X"; ShoppingBagPage puts rows together from the
 * elements' positions instead, and the locators below are the pieces it
 * works from.
 */
public final class ShoppingBagPageLocators {

    private ShoppingBagPageLocators() {

    }

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Your bag' AND accessible == 1"
            );

    // VERIFY: not seen on the redesigned build — the bag was never empty
    // while it was being mapped.
    public static final By EMPTY_BAG_MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS[c] 'empty' AND accessible == 1"
            );

    /**
     * A store group's header, a Button named brand and location:
     * "Burberry Miami Design District".
     */
    public static By storeGroup(String storeName) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH '" + storeName + "' AND accessible == 1"
        );
    }

    /**
     * An item's title line. Case-insensitive because the page and the
     * grid tile do not always agree on capitals, and a CONTAINS on the
     * longest plain run so a name carrying a zero-width space still
     * matches.
     */
    public static By item(String productName) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name CONTAINS[c] '" + longestPlainRun(productName) + "'"
                        + " AND accessible == 1"
        );
    }

    /**
     * Every item title on screen — told apart from the other texts by
     * being the one StaticText a variant Button follows. Matched here as
     * any accessible StaticText; ShoppingBagPage pairs them with variants.
     */
    public static final By ANY_TEXT =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND accessible == 1"
            );

    /**
     * Any Button that belongs to an item rather than being one of the row's
     * actions: the variant line ("Vineyard Red · M", or just "Black" on a
     * one-size product) and the store-group headers. ShoppingBagPage tells
     * them apart by column.
     */
    public static final By ANY_ITEM_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND accessible == 1"
                            + " AND NOT (name IN {'Save for later', 'Remove'})"
            );

    /**
     * The variant line ending in the size. A one-size product's line
     * carries no size at all — just the colour — so "One Size" is matched
     * as a variant line without a middle dot.
     */
    public static By itemSize(String size) {

        if (size == null || size.isEmpty() || size.equalsIgnoreCase("One Size")) {

            return AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND NOT (name CONTAINS ' · ')"
                            + " AND NOT (name IN {'Save for later', 'Remove'}) AND accessible == 1"
            );
        }

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name ENDSWITH ' · " + size + "' AND accessible == 1"
        );
    }

    /**
     * Any amount on the screen — item prices, the delivery fee, the
     * summary rows. Which one is which comes from where it sits.
     */
    public static final By ANY_AMOUNT =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH '$' AND accessible == 1"
            );

    public static final By REMOVE_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Remove' AND accessible == 1"
            );

    public static final By SAVE_FOR_LATER_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Save for later' AND accessible == 1"
            );

    /**
     * What the app writes onto a line it cannot sell once a checkout has
     * been refused.
     *
     * VERIFY: carried over from the old build ("Item is out of stock.
     * Please update your cart to proceed.") and matched loosely; no
     * refusal was provoked on the redesigned build.
     */
    public static final By OUT_OF_STOCK_MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name CONTAINS[c] 'out of stock' AND accessible == 1"
            );

    public static final By QUANTITY =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name MATCHES '^[0-9]+$' AND accessible == 1"
            );

    // ---------- Summary ----------

    public static final By SUBTOTAL_LABEL =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Subtotal' AND accessible == 1"
            );

    /**
     * One per store group — each delivery checks out on its own — and no
     * amount in the label any more.
     */
    public static final By CHECKOUT_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Review delivery & checkout' AND accessible == 1"
            );

    // The header X became the shared back chevron.
    public static final By CLOSE_BUTTON = HeaderLocators.BACK_CHEVRON;

    static String longestPlainRun(String productName) {

        String longest = "";

        for (String run : productName.split("[\\u200B-\\u200D\\uFEFF]")) {

            if (run.trim().length() > longest.length()) {

                longest = run.trim();
            }
        }

        return longest.isEmpty() ? productName : longest;
    }

}
