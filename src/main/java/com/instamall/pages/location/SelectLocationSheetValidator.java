package com.instamall.pages.location;

import com.instamall.base.BasePage;

public class SelectLocationSheetValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(SelectLocationSheetLocators.TITLE);
    }

    public boolean isSearchBoxDisplayed() {

        return isDisplayed(SelectLocationSheetLocators.SEARCH_ADDRESS_TEXTBOX);
    }

    public boolean areQuickSlotsDisplayed() {

        return isDisplayed(SelectLocationSheetLocators.SET_HOME)
                && isDisplayed(SelectLocationSheetLocators.SET_WORK)
                && isDisplayed(SelectLocationSheetLocators.OTHER);
    }

    public boolean isSavedAddressDisplayed(String addressName) {

        return isDisplayed(SelectLocationSheetLocators.savedAddress(addressName));
    }

    public boolean isSavedAddressesSectionDisplayed() {

        return isDisplayed(SelectLocationSheetLocators.SAVED_ADDRESSES_HEADER);
    }

}
