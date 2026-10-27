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

    public static final By SHOP_BY_CATEGORY =
            AppiumBy.iOSNsPredicateString(
                    "name == 'SHOP BY CATEGORY' AND accessible == 1"
            );


    /**
     * The store's name, as the header prints it. Confirmed live: the
     * header carries two lines, the brand ("Burberry") above the mall it
     * sits in ("MIAMI DESIGN DISTRICT"), and this is the upper of the
     * two.
     */
    public static final By STORE_NAME =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND accessible == 1"
                            + " AND rect.x > 30 AND rect.y BETWEEN {55, 85}"
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
     * The same tile addressed by name but WITHOUT the visibility filter, so
     * it matches a tile still scrolled off screen. Used only to locate a
     * below-the-fold tile and scroll it into view, after which the visible
     * {@link #productTile} above is what is actually tapped.
     */
    public static By productTilePresent(String tileName) {

        return AppiumBy.iOSNsPredicateString(
                "name == '" + tileName + "' AND accessible == 1"
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
