package com.instamall.pages.bag;

import com.instamall.base.BasePage;

public class ShoppingBagPageValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(ShoppingBagPageLocators.TITLE);
    }

    // Presence, not visibility: a second store group starts below the fold.
    public boolean isStoreGroupDisplayed(String storeName) {

        return isPresent(ShoppingBagPageLocators.storeGroup(storeName));
    }

    public boolean isEmpty() {

        return isDisplayed(ShoppingBagPageLocators.EMPTY_BAG_MESSAGE);
    }

}
