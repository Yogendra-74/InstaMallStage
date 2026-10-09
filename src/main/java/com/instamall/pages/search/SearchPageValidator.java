package com.instamall.pages.search;

import com.instamall.base.BasePage;

public class SearchPageValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(SearchPageLocators.TITLE);
    }

    public boolean isStoreResultDisplayed(String storeName, String mallName) {

        return isDisplayed(SearchPageLocators.storeResult(storeName, mallName));
    }

    public boolean isStoreResultDisplayed(String storeName) {

        return isDisplayed(SearchPageLocators.storeResultContaining(storeName));
    }

    public boolean isNoResultsMessageDisplayed() {

        return isDisplayed(SearchPageLocators.NO_RESULTS_MESSAGE);
    }

}
