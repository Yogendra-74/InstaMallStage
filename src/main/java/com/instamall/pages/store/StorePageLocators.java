package com.instamall.pages.store;

import com.instamall.components.navigation.HeaderLocators;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Single store screen (e.g. Burberry). Verified against a page source
 * dump.
 */
public final class StorePageLocators {

    private StorePageLocators() {

    }

    // The "BACK" link is gone; the header chevron is the way back.
    public static final By BACK_BUTTON = HeaderLocators.BACK_CHEVRON;

    public static final By DELIVERY_PROMISE_BANNER =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS[c] 'Earliest arrival' AND accessible == 1"
            );

    /**
     * The mall name under the store title, in caps ("AVENTURA MALL").
     * Callers pass it exactly as shown, uppercase.
     *
     * CAUTION: seen to disagree with the mall name given elsewhere for the
     * same store instance — Moncler read "BRICKELL CITY CENTRE" here, yet
     * "Bal Harbour" on its Home card and "Miami Design District" on the
     * order it produced. Treat any single mall label as unreliable on its
     * own; do not assert store-to-mall mapping from this alone.
     */
    public static By mallLabel(String mallNameUppercase) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + mallNameUppercase + "'"
                        + " AND accessible == 1 AND visible == 1"
        );
    }

    public static final By INDEPENDENT_SHOPPING_BANNER =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Independent Shopping Service' AND accessible == 1"
            );

    public static final By SHOP_BY_CATEGORY =
            AppiumBy.iOSNsPredicateString(
                    "name == 'SHOP BY CATEGORY' AND accessible == 1"
            );

    /**
     * In-store search box; its name carries the brand, e.g.
     * "Search Burberry".
     */
    public static By searchInStore(String storeName) {

        return AppiumBy.iOSNsPredicateString(
                "name == 'Search " + storeName + "' AND accessible == 1"
        );
    }

    /**
     * The in-store search box without knowing the brand. Its name is
     * "Search &lt;Brand&gt;", which makes it both the screen's readiness
     * gate and the place to read the brand from.
     */
    // Home's own search box also starts with "Search ", and is still in
    // the tree for a moment while the store screen pushes in over it —
    // so it is ruled out by name, or the brand gets read off the wrong
    // screen.
    public static final By SEARCH_BOX =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Search ' AND name != 'Search designers, stores and pieces'"
                            + " AND accessible == 1 AND visible == 1"
            );

    /**
     * Any product tile on screen, whatever the product is.
     *
     * Tiles are the only accessible elements here whose name carries a
     * price ("&lt;product&gt; &lt;brand&gt; $750"), so the "$" identifies
     * them.
     */
    public static final By ANY_PRODUCT_TILE =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS ' $' AND accessible == 1 AND visible == 1"
            );

    /**
     * Department header on the grid: "MEN", "WOMEN", "ALL PRODUCTS".
     */
    public static By departmentHeader(String department) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + department + "' AND accessible == 1"
        );
    }

    /**
     * "VIEW ALL" appears once per department, so it is picked by position
     * (1-based, in render order).
     */
    public static By viewAll(int occurrence) {

        return AppiumBy.iOSClassChain(
                "**/XCUIElementTypeStaticText[`name == \"VIEW ALL\" AND accessible == true`][" + occurrence + "]"
        );
    }

    /**
     * Product tile. The whole tile is one accessible element whose name is
     * "<product> <brand> <price>", e.g.
     * "Check Cotton Poplin Shirt Burberry $750".
     *
     * CAUTION: product names are not unique in this catalogue — there are
     * two "Check Cotton Poplin Shirt" tiles at $750 and $680. This matches
     * the first one; use {@link #productCard(String, String, String)} when
     * the test needs a specific variant.
     */
    public static By productCard(String productName) {

        return AppiumBy.iOSNsPredicateString(
                "name BEGINSWITH '" + productName + "' AND accessible == 1"
        );
    }

    /**
     * Exact product tile, disambiguated by brand and price as they are
     * printed on the card, e.g. ("Check Cotton Poplin Shirt", "Burberry",
     * "$750").
     */
    public static By productCard(String productName, String brand, String price) {

        return productTile(productName + " " + brand + " " + price);
    }

    /**
     * A tile addressed by its full accessible name, exactly as
     * {@link #ANY_PRODUCT_TILE} reports it.
     *
     * The visibility filter is essential: the same product is listed under
     * its department AND again under ALL PRODUCTS, so a name-only match can
     * resolve to the copy that is scrolled off screen — which never becomes
     * clickable.
     */
    public static By productTile(String tileName) {

        return AppiumBy.iOSNsPredicateString(
                "name == '" + tileName + "' AND accessible == 1 AND visible == 1"
        );
    }

    /**
     * Floating pill that replaces nothing and simply appears once the bag
     * has contents: "View bag (1)". It lives on this screen because the
     * app returns here after Add to bag.
     *
     * VERIFY: seen on the product page in the redesign, not yet on the
     * store page itself.
     */
    public static final By VIEW_BAG =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH[c] 'View bag' AND accessible == 1"
            );

}
