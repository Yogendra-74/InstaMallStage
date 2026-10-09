package com.instamall.pages.search;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * The global Search screen, reached from Home's search bar. Verified
 * against a page source dump.
 *
 * Opens on the PRODUCTS tab by default — a test after a specific store or
 * mall must switch tabs explicitly, or the search runs against the wrong
 * catalogue and comes back empty.
 */
public final class SearchPageLocators {

    private SearchPageLocators() {

    }

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Search' AND accessible == 1"
            );

    /**
     * The search field. Unnamed — only a placeholder, "Designers, pieces,
     * inspiration" — so matched by type; the screen has exactly one.
     *
     * CAUTION: typing here is subject to the same dropped-character bug
     * seen on every text field in this app (confirmed: a fast sendKeys of
     * "Ema Savahl Couture" landed as "Eavahl Couture"). Clear, type a
     * character at a time, and read the value back to verify.
     */
    public static final By SEARCH_FIELD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND visible == 1"
            );

    // ---------- Tabs ----------

    /**
     * The tab labels ("Malls", "Stores", "Products") are NOT individually
     * accessible elements — confirmed against a page source dump: no node
     * anywhere in the tree carries the name "Stores" (or "Malls" or
     * "Products") on its own; that text only ever appears folded into a
     * parent container's aggregated name. The tabs are internally named
     * "search_scope_tab_0/1/2" instead (left to right), which is what
     * these match — the one stable, tappable identifier this row offers.
     */
    public static final By STORES_TAB =
            AppiumBy.accessibilityId("search_scope_tab_1");

    public static final By PRODUCTS_TAB =
            AppiumBy.accessibilityId("search_scope_tab_2");

    // ---------- Empty-query state ----------

    public static final By EXPLORE_HEADER =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Explore' AND accessible == 1"
            );

    // ---------- Results ----------

    /**
     * A Stores-tab result row by store name alone, without needing the
     * mall name it is paired with.
     *
     * Same disambiguation shape as
     * {@link com.instamall.pages.product.ProductDetailPageLocators#SIZE_OPTIONS}:
     * the wrapping ancestors that also CONTAINS-match the store name are
     * full-screen-height Others (845-956pt tall in the dumps this was
     * mapped from), while the actual row is a bounded ~74pt tall Other —
     * so a height ceiling well under either full-screen figure picks out
     * the real row without the mall name.
     */
    public static By storeResultContaining(String storeName) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeOther' AND name CONTAINS '" + storeName + "'"
                        + " AND accessible == 1 AND rect.height < 200"
        );
    }

    /**
     * "No pieces found" empty state on the Products tab — also the
     * general shape of the "nothing here" message on the other tabs.
     */
    public static final By NO_RESULTS_MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name CONTAINS[c] 'No pieces found'"
                            + " AND accessible == 1"
            );

}
