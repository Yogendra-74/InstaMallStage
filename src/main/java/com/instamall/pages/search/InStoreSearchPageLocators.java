package com.instamall.pages.search;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * The dedicated search screen a store's own search box pushes into —
 * distinct from {@link SearchPageLocators}, the global search reached from
 * Home. Both screens share the title "Search", but this one's field reads
 * "Search in &lt;Store&gt;" (the store page's own widget reads "Search
 * &lt;Store&gt;", no "in") and offers category chips instead of
 * Malls/Stores/Products tabs.
 */
public final class InStoreSearchPageLocators {

    private InStoreSearchPageLocators() {

    }

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Search' AND accessible == 1"
            );

    /**
     * The search field; its placeholder carries the store name ("Search
     * in Moncler"), matched by type since it is otherwise unnamed. The
     * screen has exactly one text field.
     *
     * CAUTION: same dropped-character bug as every text field in this
     * app — clear, type a character at a time, and read the value back.
     */
    public static final By SEARCH_FIELD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND visible == 1"
            );

    public static final By EXPLORE_HEADER =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Explore' AND accessible == 1"
            );

    /**
     * A suggested category chip, e.g. "Accessories", "Knitwear" — the set
     * shown is store-specific.
     */
    public static By categoryChip(String category) {

        return AppiumBy.iOSNsPredicateString(
                "name == '" + category + "' AND accessible == 1"
        );
    }

    public static final By SUGGESTED_SEARCHES_DISCLAIMER =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                            + " AND name == 'Suggested searches, not live trending data.' AND accessible == 1"
            );

}
