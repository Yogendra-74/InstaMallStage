package com.instamall.pages.store;

import com.instamall.base.BasePage;

import java.time.Duration;

public class StorePageValidator extends BasePage {

    private static final Duration CATALOG_TIMEOUT = Duration.ofSeconds(45);

    public boolean isDisplayed(String storeName) {

        return isDisplayed(StorePageLocators.searchInStore(storeName), CATALOG_TIMEOUT);
    }

    public boolean isDeliveryPromiseDisplayed() {

        return isDisplayed(StorePageLocators.DELIVERY_PROMISE_BANNER);
    }

    public boolean isProductDisplayed(String productName) {

        return isDisplayed(StorePageLocators.productCard(productName), CATALOG_TIMEOUT);
    }

    public boolean isDepartmentDisplayed(String department) {

        return isDisplayed(StorePageLocators.departmentHeader(department));
    }

}
