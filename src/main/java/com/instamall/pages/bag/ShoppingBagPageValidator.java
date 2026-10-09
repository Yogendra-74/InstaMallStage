package com.instamall.pages.bag;

import com.instamall.base.BasePage;

public class ShoppingBagPageValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(ShoppingBagPageLocators.TITLE);
    }

    public boolean isItemDisplayed(String productName) {

        return isDisplayed(ShoppingBagPageLocators.item(productName));
    }

    /**
     * Whether one exact variant is in the bag. Name and size together,
     * because a store group can hold several rows sharing a name — so
     * neither half on its own says a particular variant is still there.
     */
    public boolean isItemDisplayed(String productName, String size) {

        return new ShoppingBagPage().hasItem(productName, size);
    }

    // Presence, not visibility: a second store group starts below the fold.
    public boolean isStoreGroupDisplayed(String storeName) {

        return isPresent(ShoppingBagPageLocators.storeGroup(storeName));
    }

    public boolean isItemSizeDisplayed(String size) {

        return isDisplayed(ShoppingBagPageLocators.itemSize(size));
    }

    public boolean isCheckoutButtonDisplayed() {

        return isDisplayed(ShoppingBagPageLocators.CHECKOUT_BUTTON);
    }

    public boolean isEmpty() {

        return isDisplayed(ShoppingBagPageLocators.EMPTY_BAG_MESSAGE);
    }

}
