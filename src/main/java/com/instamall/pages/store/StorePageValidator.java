package com.instamall.pages.store;

import com.instamall.base.BasePage;

import java.time.Duration;

public class StorePageValidator extends BasePage {

    private static final Duration CATALOG_TIMEOUT = Duration.ofSeconds(45);

    public boolean isDisplayed(String storeName) {

        return isDisplayed(StorePageLocators.searchInStore(storeName), CATALOG_TIMEOUT);
    }

}
