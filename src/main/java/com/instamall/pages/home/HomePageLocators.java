package com.instamall.pages.home;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Home / store-listing screen. Verified against a page source dump.
 *
 * Two things this screen teaches about the app's tree:
 *  - Containers carry an aggregated name of everything inside them, and
 *    they are accessible == 0. Every real control is accessible == 1, so
 *    that flag is what separates a tappable element from its wrapper.
 *  - StaticTexts come in pairs (outer accessible == 0, inner == 1), so
 *    findElements without an accessibility filter returns duplicates.
 */
public final class HomePageLocators {

    private HomePageLocators() {

    }

    // ---------- Header ----------

    /**
     * The location chip. Its name is "Deliver to " plus the current value —
     * "Deliver to Z9 VENTURES" once an address is picked, "Deliver to
     * Select location" while none is. Tapping it opens the "Deliver to"
     * address sheet.
     */
    public static final By LOCATION_SELECTOR =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name BEGINSWITH 'Deliver to ' AND accessible == 1"
            );

    /**
     * The selected address as rendered in the header — uppercase here
     * ("Z9 VENTURES"), title case on the checkout screen ("Z9 Ventures"),
     * hence the case-insensitive match.
     */
    public static By selectedLocation(String locationName) {

        return AppiumBy.iOSNsPredicateString(
                "name BEGINSWITH[c] 'Deliver to " + locationName + "' AND accessible == 1"
        );
    }

    // Unread-notification count next to the bell.
    // VERIFY: not seen in the redesigned header, which shows an unnamed
    // bell and the bag count ("3", an Other) instead.
    public static final By NOTIFICATION_BADGE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name MATCHES '^[0-9]+$' AND accessible == 1"
            );

    // ---------- Order tracking strip ----------

    public static final By ORDER_IN_PROGRESS_CARD =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Confirming your order' AND accessible == 1"
            );

    // VERIFY: no returned order was on the account to check this against
    // the redesigned strip.
    public static final By ORDER_RETURN_CARD =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'RECEIVED' AND accessible == 1"
            );

    // ---------- Body ----------

    public static final By SEARCH_INSTAMALL =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Search designers, stores and pieces' AND accessible == 1"
            );

    // The old "Your Favorite Brands Delivered In…" banner is gone; the
    // promise now sits in the header, beside the location chip, as
    // "From 14 min" over "Arrival by piece".
    public static final By DELIVERY_PROMISE_BANNER =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH 'From ' AND name ENDSWITH ' min'"
                            + " AND accessible == 1"
            );

    public static final By SHOP_BY_CATEGORY =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Shop by category' AND accessible == 1"
            );

    /**
     * Section header: "MIAMI'S FAVORITES", "NEWEST TO INSTAMALL",
     * "EXPLORE MALLS".
     *
     * Class chain, not a predicate: the app writes a straight apostrophe
     * in "MIAMI'S", which would terminate a single-quoted predicate.
     */
    public static By sectionHeader(String title) {

        return AppiumBy.iOSClassChain(
                "**/XCUIElementTypeStaticText[`name == \"" + title + "\" AND accessible == true`]"
        );
    }

    /**
     * "See all" repeats once per section, so it has to be picked by
     * position. 1-based, in the order the sections are rendered.
     */
    public static By seeAll(int occurrence) {

        return AppiumBy.iOSClassChain(
                "**/XCUIElementTypeOther[`name == \"See all\" AND accessible == true`][" + occurrence + "]"
        );
    }

    /**
     * Store card on the feed. Its name is the whole card read out —
     * "Burberry 112 NE 39th St. Suite 101 14-24 mins · 0.9 miles away" —
     * so match the brand as a prefix.
     *
     * BEGINSWITH, not CONTAINS: the order-tracking cards above the feed
     * mention brands too ("CONFIRMING WITH BURBERRY").
     */
    public static By storeCard(String storeName) {

        return AppiumBy.iOSNsPredicateString(
                "name BEGINSWITH '" + storeName + "' AND accessible == 1"
        );
    }

    /**
     * Any store card currently on screen, whichever brand it is.
     *
     * The delivery estimate ("14-24 mins · ") is what separates a store
     * from a mall card — malls read "&lt;name&gt; &lt;address&gt; &lt;n&gt;
     * miles away" with no delivery time.
     */
    public static final By ANY_STORE_CARD =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS ' mins · ' AND accessible == 1 AND visible == 1"
            );

    /**
     * A store card addressed by its full name, exactly as
     * {@link #ANY_STORE_CARD} reports it. Visibility matters: the feed
     * keeps off-screen cards in the tree and they never become clickable.
     */
    public static By storeCardNamed(String cardName) {

        return AppiumBy.iOSNsPredicateString(
                "name == '" + cardName + "' AND accessible == 1 AND visible == 1"
        );
    }

    public static By mallCard(String mallName) {

        return AppiumBy.iOSNsPredicateString(
                "name BEGINSWITH '" + mallName + "' AND accessible == 1"
        );
    }

    // ---------- Location empty state (shown until an address is chosen) ----------

    public static final By LOCATION_EMPTY_STATE_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Choose your location to start shopping' AND accessible == 1"
            );

    public static final By ENABLE_LOCATION_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Enable Location' AND accessible == 1"
            );

    public static final By SELECT_LOCATION_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Select location' AND accessible == 1"
            );

    // ---------- Bottom tab bar ----------

    public static final By TAB_HOME =
            AppiumBy.accessibilityId("Home");

    public static final By TAB_WISHLIST =
            AppiumBy.accessibilityId("Wishlist");

    public static final By TAB_AI_STYLIST =
            AppiumBy.accessibilityId("AI Stylist");

    /**
     * The bag tab is renamed once the bag is not empty: "Bag" becomes
     * "1 Bag". A plain accessibility id of "Bag" therefore stops matching
     * mid-flow, which is why this is a predicate.
     */
    public static final By TAB_BAG =
            AppiumBy.iOSNsPredicateString(
                    "(name == 'Bag' OR name ENDSWITH ' Bag') AND accessible == 1"
            );

    /**
     * Bag tab with an expected item count, e.g. tabBagWithCount(1) →
     * "1 Bag". Use it to assert the badge rather than just to tap.
     */
    public static By tabBagWithCount(int itemCount) {

        return AppiumBy.accessibilityId(itemCount + " Bag");
    }

    public static final By TAB_PROFILE =
            AppiumBy.accessibilityId("Account");

}
