package com.instamall.pages.wishlist;

import com.instamall.base.BasePage;

public class WishlistPageValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(WishlistPageLocators.TITLE);
    }

    public boolean areTabsDisplayed() {

        return isDisplayed(WishlistPageLocators.PRODUCTS_TAB)
                && isDisplayed(WishlistPageLocators.BRANDS_TAB);
    }

    public boolean isProductSaved(String productName) {

        return isPresent(WishlistPageLocators.card(productName));
    }

}
