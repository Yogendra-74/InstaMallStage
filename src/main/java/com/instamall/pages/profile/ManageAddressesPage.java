package com.instamall.pages.profile;

import com.instamall.base.BasePage;

public class ManageAddressesPage extends BasePage {

    public ManageAddressesPage waitForScreen() {

        waitForVisibility(ManageAddressesPageLocators.TITLE);

        return this;
    }

    public ManageAddressesPage tapAddNewAddress() {

        click(ManageAddressesPageLocators.ADD_NEW_ADDRESS);

        return this;
    }

    public ManageAddressesPage openAddress(String label) {

        click(ManageAddressesPageLocators.addressCard(label));

        return this;
    }

}
