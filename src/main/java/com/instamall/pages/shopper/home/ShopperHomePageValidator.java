package com.instamall.pages.shopper.home;

import com.instamall.base.BasePage;

public class ShopperHomePageValidator extends BasePage {

    public boolean isActiveTabDisplayed() {

        return isDisplayed(ShopperHomePageLocators.ACTIVE_TAB);
    }

    public boolean isHistoryTabDisplayed() {

        return isDisplayed(ShopperHomePageLocators.HISTORY_TAB);
    }

    public boolean areAllTabsDisplayed() {

        return isDisplayed(ShopperHomePageLocators.TAB_HOME)
                && isDisplayed(ShopperHomePageLocators.TAB_RETURNS)
                && isDisplayed(ShopperHomePageLocators.TAB_PROFILE);
    }

    public boolean isOrderCardDisplayed(String... fragments) {

        return isDisplayed(ShopperHomePageLocators.orderCardContaining(fragments));
    }

    public boolean isNoActiveOrdersDisplayed() {

        return isDisplayed(ShopperHomePageLocators.NO_ACTIVE_ORDERS_MESSAGE);
    }

}
