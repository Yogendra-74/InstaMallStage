package com.instamall.pages.profile;

import com.instamall.base.BasePage;

public class ManageAddressesPageValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(ManageAddressesPageLocators.TITLE);
    }

    public boolean isAddNewAddressDisplayed() {

        return isDisplayed(ManageAddressesPageLocators.ADD_NEW_ADDRESS);
    }

    public boolean isAddressDisplayed(String label) {

        return isDisplayed(ManageAddressesPageLocators.addressCard(label));
    }

}
