package com.instamall.pages.portal;

import com.instamall.base.PortalBasePage;
import com.instamall.waits.PortalWaitUtils;

import java.time.Duration;

public class PortalOrderDetailsPage extends PortalBasePage {

    /**
     * How long the detail page gets to render after its row is clicked.
     * The sandbox portal is slow right after a login — the History list
     * alone took 22s in one run — and the plain 20s default was not
     * enough: confirmed live that the page does open (breadcrumb "View
     * Order", Order Summary, Order ID) but can take longer than that.
     */
    private static final Duration PAGE_LOADS_WITHIN = Duration.ofSeconds(90);

    public PortalOrderDetailsPage waitForScreen() {

        waitForVisibility(PortalOrderDetailsPageLocators.PAGE_HEADER, PAGE_LOADS_WITHIN);
        waitForVisibility(PortalOrderDetailsPageLocators.ORDER_ID_VALUE, PAGE_LOADS_WITHIN);

        return this;
    }

    public String getOrderIdValue() {

        return getText(PortalOrderDetailsPageLocators.ORDER_ID_VALUE);
    }

    public String getStatusBadge() {

        return getText(PortalOrderDetailsPageLocators.ORDER_SUMMARY_STATUS_BADGE);
    }

    // ---------- Override Status ----------

    public PortalOrderDetailsPage openStatusDropdown() {

        click(PortalOrderDetailsPageLocators.ORDER_STATUS_DROPDOWN);

        return this;
    }

    /**
     * Filters the (short, cut-off) option panel down to whatever the
     * portal's own option list contains matching this rough query —
     * confirmed live this is necessary for options further down the list
     * (e.g. "Driver At PickUp"), which do not all fit in the panel's own
     * scroll area as rendered.
     */
    public PortalOrderDetailsPage filterStatusOptions(String roughQuery) {

        type(PortalOrderDetailsPageLocators.ORDER_STATUS_SEARCH_INPUT, roughQuery);

        return this;
    }

    /**
     * Selects whichever rendered option contains this text, case-
     * insensitively — pair with {@link #filterStatusOptions(String)}
     * first so there is only one candidate, since the portal's own
     * casing (e.g. "Driver At PickUp") should not need to be guessed
     * exactly to select it.
     */
    public PortalOrderDetailsPage selectStatusOptionContaining(String roughStatusName) {

        click(PortalOrderDetailsPageLocators.orderStatusOptionContaining(roughStatusName));

        return this;
    }

    public PortalOrderDetailsPage enterOverrideNote(String note) {

        type(PortalOrderDetailsPageLocators.NOTE_INPUT, note);

        return this;
    }

    public PortalOrderDetailsPage clickOverride() {

        click(PortalOrderDetailsPageLocators.OVERRIDE_BUTTON);

        return this;
    }

    /**
     * The whole Override Status flow in one call: open the dropdown,
     * filter to and select the status, enter the note, and submit.
     */
    /**
     * How long the Order Summary badge gets to show the new status after
     * Override is clicked — confirmed live at about 3s for both
     * "Assigned" and "Driver At PickUp".
     */
    private static final Duration OVERRIDE_APPLIES_WITHIN = Duration.ofSeconds(30);

    public PortalOrderDetailsPage overrideStatusTo(String roughStatusName, String note) {

        openStatusDropdown()
                .filterStatusOptions(roughStatusName)
                .selectStatusOptionContaining(roughStatusName)
                .enterOverrideNote(note)
                .clickOverride();

        // Confirmed live: the form clears itself and the badge flips to
        // the new status a few seconds later. Waiting on the badge is
        // what says the override actually applied, rather than that the
        // button was merely tapped.
        PortalWaitUtils.waitUntil(driver -> {

            String badge = getStatusBadge();

            return badge != null && badge.toLowerCase().contains(roughStatusName.toLowerCase())
                    ? badge
                    : null;

        }, OVERRIDE_APPLIES_WITHIN);

        return this;
    }

}
