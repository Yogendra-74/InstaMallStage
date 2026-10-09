package com.instamall.pages.portal;

import com.instamall.base.PortalBasePage;

public class PortalOrderDetailsPage extends PortalBasePage {

    public PortalOrderDetailsPage waitForScreen() {

        waitForVisibility(PortalOrderDetailsPageLocators.PAGE_HEADER);

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
    public PortalOrderDetailsPage overrideStatusTo(String roughStatusName, String note) {

        return openStatusDropdown()
                .filterStatusOptions(roughStatusName)
                .selectStatusOptionContaining(roughStatusName)
                .enterOverrideNote(note)
                .clickOverride();
    }

}
