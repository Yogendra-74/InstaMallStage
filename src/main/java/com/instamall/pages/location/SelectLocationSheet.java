package com.instamall.pages.location;

import com.instamall.base.BasePage;

public class SelectLocationSheet extends BasePage {

    /**
     * On screen, without waiting for it to stop moving — the sheet is
     * there, but may still be sliding.
     */
    public SelectLocationSheet waitUntilVisible() {

        waitForVisibility(SelectLocationSheetLocators.TITLE);

        return this;
    }

    /**
     * The sheet slides up, so wait for the title to stop moving before
     * tapping anything inside it.
     */
    public SelectLocationSheet waitForScreen() {

        settle(SelectLocationSheetLocators.TITLE);

        return this;
    }

    public SelectLocationSheet chooseSavedAddress(String addressName) {

        click(SelectLocationSheetLocators.savedAddress(addressName));

        return this;
    }

    public SelectLocationSheet searchAddress(String query) {

        type(SelectLocationSheetLocators.SEARCH_ADDRESS_TEXTBOX, query);

        return this;
    }

    public SelectLocationSheet close() {

        click(SelectLocationSheetLocators.CLOSE_BUTTON);

        return this;
    }

}
