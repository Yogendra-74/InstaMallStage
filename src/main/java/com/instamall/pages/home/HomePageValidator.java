package com.instamall.pages.home;

import com.instamall.base.BasePage;

import java.time.Duration;

public class HomePageValidator extends BasePage {

    /**
     * Home is the post-login landing screen, so callers use this to decide
     * whether a session is already active. Give it room to load.
     */
    private static final Duration LANDING_TIMEOUT = Duration.ofSeconds(45);

    /**
     * The Home tab, on screen at this instant and without waiting — the
     * mark of an app that came up already signed in.
     */
    public boolean isHomeTabDisplayedNow() {

        return isDisplayedNow(HomePageLocators.TAB_HOME);
    }

    public boolean isDisplayed() {

        return isDisplayed(HomePageLocators.TAB_HOME, LANDING_TIMEOUT);
    }

    public boolean isSearchBarDisplayed() {

        return isDisplayed(HomePageLocators.SEARCH_INSTAMALL);
    }

    public boolean isSectionDisplayed(String title) {

        return isDisplayed(HomePageLocators.sectionHeader(title));
    }

    public boolean isStoreDisplayed(String storeName) {

        return isDisplayed(HomePageLocators.storeCard(storeName));
    }

    public boolean isLocationSelected(String locationName) {

        return isDisplayed(HomePageLocators.selectedLocation(locationName));
    }

    public boolean areAllTabsDisplayed() {

        return isDisplayed(HomePageLocators.TAB_HOME)
                && isDisplayed(HomePageLocators.TAB_WISHLIST)
                && isDisplayed(HomePageLocators.TAB_AI_STYLIST)
                && isDisplayed(HomePageLocators.TAB_BAG)
                && isDisplayed(HomePageLocators.TAB_PROFILE);
    }

}
