package com.instamall.pages.portal;

import org.openqa.selenium.By;

/**
 * The Delivery Portal's Orders screen — four tabs (Active, History,
 * Manifest, Route Plans), each its own hash route. Verified against live
 * page source dumps taken from both the Active and History tabs.
 *
 * The two tabs' search boxes are genuinely different fields with
 * different placeholders and, seemingly, different indexed content:
 * History's box (placeholder "Search Order ID") did not surface a match
 * on this app's own order number ("IM-48575") tried against the Active
 * tab's box either — confirmed live, that search returned "No Records
 * Found" for it. The Active tab's single order card was instead found by
 * simply being the only one on screen, and its customer name (the same
 * "Duel Sales" this suite's own order data uses) confirmed it was the
 * right one. What each search box actually indexes (portal-internal ID?
 * customer name? some other reference?) is still unconfirmed — this
 * app's own order number does not appear to be one of them.
 */
public final class PortalOrdersPageLocators {

    private PortalOrdersPageLocators() {

    }

    // ---------- Sidebar nav ----------

    /**
     * The left-nav "Orders" entry, present on every portal screen — distinct
     * from the in-page tabs below (ACTIVE_TAB et al.), which only exist once
     * already on an Orders screen. Verified against a live page source dump
     * taken from the History tab, where it carries the "active" class.
     */
    public static final By SIDEBAR_ORDERS_LINK =
            By.cssSelector("a.nav-link[href='#/deliveries/status']");

    // ---------- Tabs ----------

    public static final By ACTIVE_TAB =
            By.cssSelector("a#orderContainerAnchor");

    public static final By HISTORY_TAB =
            By.cssSelector("a[href='#/deliveries/status/details']");

    public static final By MANIFEST_TAB =
            By.cssSelector("a[href='#/deliveries/status/manifest/list']");

    public static final By ROUTE_PLANS_TAB =
            By.cssSelector("a[href='#/deliveries/status/route_optimization']");

    // ---------- Active tab ----------

    public static final By ACTIVE_SEARCH_INPUT =
            By.cssSelector("input[placeholder='Search Customer, Order, Group Id']");

    /**
     * Shared by both tabs' search sections — scope a find to whichever
     * tab is open rather than relying on this alone if both could ever
     * be present at once (they should not be, given the hash routing).
     */
    public static final By SEARCH_BUTTON =
            By.cssSelector("button.search-btn");

    // ---------- History tab ----------

    public static final By HISTORY_SEARCH_INPUT =
            By.cssSelector("input[placeholder='Search Order ID']");

    // ---------- Order card (Active tab) ----------

    /**
     * The card's own order-id link — opens the order's detail page in a
     * new tab/window (target="_blank"), so a caller needs to switch to it
     * (PortalDriverManager.getDriver().getWindowHandles(), then
     * switchTo().window(...)) rather than expecting navigation in place.
     * Its accessible text is the portal's own internal order id (e.g.
     * "01M3RM9RS4XTNVXHDQ4KQJ7W3P"), not this app's own order number.
     */
    public static final By ORDER_ID_LINK =
            By.cssSelector("a.order-id");

    // ---------- Order rows (History tab) ----------

    /**
     * Every order-id link in the History tab's table (one per row,
     * including "-exception-N" suffixed rows for retried orders) —
     * verified against a live page source dump. Each link's accessible
     * text is the portal's own internal order id (e.g.
     * "01M0FHYDGE3VNTA9FGCR6S24Q5"), same as ORDER_ID_LINK above; unlike
     * that one, these navigate in place rather than opening a new tab.
     */
    public static final By ORDER_ID_LINKS_LIST =
            By.cssSelector("a.text-monospace");

    public static final By NO_RECORDS_FOUND =
            By.xpath("//*[contains(text(), 'No Records Found')]");

}
