package com.instamall.pages.wishlist;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * WISHLIST screen. Verified against a page source dump.
 *
 * Saved products are cards in a two-column grid, 185 x 370 each. A card is
 * one accessibility element that reads itself out —
 * "Check Crossbody Bag&#92;u200B Burberry $1,150 MOVE TO BAG" — and iOS
 * collapses its subtree, so nothing inside it can be addressed on its own.
 * That is why WishlistPage reaches MOVE TO BAG by offset rather than by a
 * locator: there is no element to select.
 */
public final class WishlistPageLocators {

    private WishlistPageLocators() {

    }

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'WISHLIST' AND accessible == 1"
            );

    public static final By PRODUCTS_TAB =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Products' AND accessible == 1"
            );

    public static final By BRANDS_TAB =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Brands' AND accessible == 1"
            );

    /**
     * "9 ITEM(S) SAVED". Two of these are on screen while the count is
     * changing — the old and the new — so a caller reading it should treat
     * more than one as normal.
     */
    public static final By SAVED_COUNT =
            AppiumBy.iOSNsPredicateString(
                    "name ENDSWITH 'ITEM(S) SAVED' AND accessible == 1"
            );

    /**
     * Every saved card on screen. MOVE TO BAG is what marks a card out
     * from the sections around it, RECENTLY VIEWED included, whose tiles
     * carry a product and a price but no such button.
     */
    public static final By ANY_CARD =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS 'MOVE TO BAG' AND accessible == 1 AND visible == 1"
            );

    /**
     * One named product's card.
     *
     * Matched on the longest stretch of the name holding no zero-width
     * characters: this catalogue's names carry U+200B, and pasting one
     * straight into a predicate ties the match to those surviving intact
     * from the product page to here.
     */
    public static By card(String productName) {

        return AppiumBy.iOSNsPredicateString(
                "name CONTAINS '" + longestPlainRun(productName) + "'"
                        + " AND name CONTAINS 'MOVE TO BAG' AND accessible == 1 AND visible == 1"
        );
    }

    /**
     * The longest run of a name containing no zero-width characters.
     */
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
