package com.instamall.pages.store;

import com.instamall.base.BasePage;

import java.time.Duration;

public class StorePageValidator extends BasePage {

    private static final Duration CATALOG_TIMEOUT = Duration.ofSeconds(45);

    /**
     * Whether the store screen for this brand is up, by the name in its
     * header.
     */
    public boolean isDisplayed(String storeName) {

        return isDisplayed(StorePageLocators.STORE_NAME, CATALOG_TIMEOUT)
                && storeName.equalsIgnoreCase(getText(StorePageLocators.STORE_NAME).trim());
    }

}
