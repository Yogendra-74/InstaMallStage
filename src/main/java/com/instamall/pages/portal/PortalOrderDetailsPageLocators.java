package com.instamall.pages.portal;

import org.openqa.selenium.By;

/**
 * The Delivery Portal's "View Order" screen, opened from
 * PortalOrdersPageLocators.ORDER_ID_LINK (a new tab). Verified against a
 * live page source dump; still being mapped — locators below cover only
 * what has been confirmed so far (Order Summary card, top-level status).
 */
public final class PortalOrderDetailsPageLocators {

    private PortalOrderDetailsPageLocators() {

    }

    public static final By PAGE_HEADER =
            By.xpath("//*[contains(text(), 'View Order')]");

    // ---------- Order Summary card ----------

    /**
     * The pill next to "Order Summary" — read this rather than assuming
     * a fixed value; seen as "Dispatched" for the order this was mapped
     * against, matching the Active tab's own card.
     */
    public static final By ORDER_SUMMARY_STATUS_BADGE =
            By.xpath("//*[contains(@class,'card-header')][.//*[contains(text(),'Order Summary')]]//*[contains(@class,'badge') or contains(@class,'ds-btn')]");

    public static final By ORDER_ID_VALUE =
            By.xpath("//*[normalize-space(text())='Order ID']/following-sibling::*[1]");

    public static final By PRIMARY_PROVIDER_VALUE =
            By.xpath("//*[normalize-space(text())='Primary Provider']/following-sibling::*[1]");

    public static final By TRACKING_NUMBER_VALUE =
            By.xpath("//*[normalize-space(text())='Tracking Number']/following-sibling::*[1]");

    // ---------- Override Status section ----------
    //
    // An Angular ng-select, not a native <select> — its option list is
    // appended to <body> (appendto="body" in the markup) only once
    // opened, so it is not a descendant of ORDER_STATUS_DROPDOWN in the
    // DOM. Click the dropdown to open it, then locate the option by its
    // own visible text.
    //
    // The full option list (Dispatched, Staged, Assigned, Pickup
    // Started, Driver Approaching, Driver At PickUp, and likely more) did
    // not all fit in the panel's own scroll area (confirmed live: "Driver
    // At PickUp" was not among the ones initially rendered) — rather than
    // scrolling the panel, type into ORDER_STATUS_SEARCH_INPUT (the same
    // ng-select is searchable) to filter down to the one option wanted.
    // Confirmed live that the portal's own capitalization can differ
    // from what one might guess or type — it is "Driver At PickUp", not
    // "Driver at Pickup" — so match against the option actually rendered
    // after filtering rather than assuming a wording.

    public static final By ORDER_STATUS_DROPDOWN =
            By.cssSelector("ng-select[formcontrolname='selectedOrderStatus']");

    /**
     * The same ng-select's own internal filter input, rendered inside it
     * once open — typing here narrows ORDER_STATUS_OPTION_MATCHING to
     * whatever the portal's option list actually contains, without
     * needing to scroll the (short, cut-off) panel to find it.
     */
    public static final By ORDER_STATUS_SEARCH_INPUT =
            By.cssSelector("ng-select[formcontrolname='selectedOrderStatus'] input");

    /**
     * Any rendered option, matched case-insensitively and by substring
     * rather than requiring the exact wording/casing the portal itself
     * uses (e.g. "Driver At PickUp") — pair with
     * ORDER_STATUS_SEARCH_INPUT: type a rough query there first to
     * narrow the list, then use this to find whatever it actually
     * rendered without needing to already know its exact casing.
     */
    public static By orderStatusOptionContaining(String roughStatusName) {

        return By.xpath("//div[contains(@class,'ng-option')][contains(translate(., "
                + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '"
                + roughStatusName.toLowerCase() + "')]");
    }

    public static final By NOTE_INPUT =
            By.cssSelector("input[formcontrolname='orderStatusNote']");

    public static final By OVERRIDE_BUTTON =
            By.cssSelector("ds-button[elementid='updateStatus'] button[type='submit']");

}
