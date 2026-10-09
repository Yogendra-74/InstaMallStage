package com.instamall.pages.orders;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * The Customer app's "Your orders" screen, reached via Account -&gt; the
 * ORDERS stat tile (see ProfilePage.openOrders()). Verified against a
 * live page source dump.
 *
 * Each row is a single accessible button whose name concatenates the
 * order id, its current status, and the store brand with no separators
 * (e.g. "IM-AE078 Delivered Burberry") — there is no separate element per
 * field, so a row is matched by its id as a prefix rather than needing
 * three locators.
 */
public final class OrdersListPageLocators {

    private OrdersListPageLocators() {

    }

    /**
     * Both the screen's title elements found live report
     * accessible="false" (only their StaticText leaf matches by type/name
     * here) — accessible==1 would never match either, so it is left off
     * this one locator on purpose.
     */
    public static final By SCREEN_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Your orders'"
            );

    public static final By ANY_ORDER_ROW =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'IM-' AND accessible == 1"
            );

    /**
     * The row for a specific order id (e.g. "IM-AE078") — matched as a
     * prefix of the row's "&lt;orderId&gt; &lt;status&gt; &lt;brand&gt;"
     * name, so it is found regardless of its current status or brand.
     */
    public static By orderRow(String orderId) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH '" + orderId + "' AND accessible == 1"
        );
    }

    /**
     * The same row, additionally required to carry the given status
     * somewhere in its name — there is no separate "Status" element to
     * read a pill's text off of (see this class's own doc comment), so
     * the status is asserted this way instead of parsed out of the row's
     * full name.
     */
    public static By orderRowWithStatus(String orderId, String status) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH '" + orderId + "'"
                        + " AND name CONTAINS '" + status + "' AND accessible == 1"
        );
    }

}
