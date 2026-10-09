package com.instamall.pages.home;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class HomePage extends BasePage {

    /**
     * Home is reached straight after login, so the first paint waits on
     * the network (session, location, store feed) — hence the longer
     * timeout than the framework default.
     */
    private static final Duration FIRST_PAINT_TIMEOUT = Duration.ofSeconds(45);

    public HomePage waitForScreen() {

        WaitUtils.waitForVisibility(HomePageLocators.TAB_HOME, FIRST_PAINT_TIMEOUT);
        WaitUtils.waitForVisibility(HomePageLocators.SEARCH_INSTAMALL, FIRST_PAINT_TIMEOUT);

        return this;
    }

    /**
     * Opens the "Deliver to" address sheet from the header chip.
     */
    public HomePage openLocationSelector() {

        click(HomePageLocators.LOCATION_SELECTOR);

        return this;
    }

    /**
     * Same sheet, reached from the body's "Select location" button — the
     * app shows that empty state until an address has been chosen.
     */
    public HomePage openLocationSelectorFromEmptyState() {

        click(HomePageLocators.SELECT_LOCATION_BUTTON);

        return this;
    }

    public HomePage openStore(String storeName) {

        // The feed animates in; wait for the card to stop moving so the
        // tap does not land on the neighbouring card.
        settle(HomePageLocators.storeCard(storeName));

        click(HomePageLocators.storeCard(storeName));

        return this;
    }

    /**
     * @return the accessible names of the store cards currently on the
     *         feed, each reading brand, address and delivery estimate.
     */
    public List<String> listVisibleStoreCards() {

        WaitUtils.waitForVisibility(
                HomePageLocators.ANY_STORE_CARD, FIRST_PAINT_TIMEOUT);

        List<String> cards = new ArrayList<>();

        for (WebElement card : DriverManager.getDriver()
                .findElements(HomePageLocators.ANY_STORE_CARD)) {

            String name = card.getAttribute("name");

            if (name != null && !name.isBlank()) {

                cards.add(name);
            }
        }

        return cards;
    }

    /**
     * Opens a store by its full card name, as listed by
     * {@link #listVisibleStoreCards()}.
     */
    public HomePage openStoreCard(String cardName) {

        settle(HomePageLocators.storeCardNamed(cardName));

        click(HomePageLocators.storeCardNamed(cardName));

        return this;
    }

    /**
     * Opens whichever store the feed is leading with.
     *
     * @return the card's accessible name, for the log.
     */
    public String openFirstAvailableStore() {

        WebElement card = WaitUtils.waitForVisibility(
                HomePageLocators.ANY_STORE_CARD, FIRST_PAINT_TIMEOUT);

        String cardName = card.getAttribute("name");

        settle(HomePageLocators.ANY_STORE_CARD);

        click(HomePageLocators.ANY_STORE_CARD);

        return cardName;
    }

    public HomePage search(String query) {

        type(HomePageLocators.SEARCH_INSTAMALL, query);

        return this;
    }

    /**
     * Taps into the full Search screen (Malls / Stores / Products tabs) —
     * see com.instamall.pages.search.SearchPage. A store cannot reliably
     * be found by name without it: the "EXPLORE MALLS" mall cards on this
     * screen open a mall landing page that was confirmed NOT to work as a
     * per-mall store directory (always shows the same static "Your Local
     * Edit" card regardless of which mall was tapped).
     */
    public HomePage openSearch() {

        click(HomePageLocators.SEARCH_INSTAMALL);

        return this;
    }

    /**
     * Opens the dedicated Malls list (com.instamall.pages.malls.MallsListPage)
     * via EXPLORE MALLS's own "See all" link.
     *
     * FRAGILE: seeAll() is positional (1-based, in render order) and
     * EXPLORE MALLS was the 3rd section when this was mapped — MIAMI'S
     * FAVORITES, then NEWEST TO INSTAMALL, then EXPLORE MALLS. A reordered
     * or added section on Home shifts this without warning.
     */
    public HomePage openExploreMallsSeeAll() {

        click(HomePageLocators.seeAll(3));

        return this;
    }

    public HomePage openShopByCategory() {

        click(HomePageLocators.SHOP_BY_CATEGORY);

        return this;
    }

    public HomePage openBag() {

        click(HomePageLocators.TAB_BAG);

        return this;
    }

    public HomePage openProfile() {

        click(HomePageLocators.TAB_PROFILE);

        return this;
    }

    /**
     * Back to the feed from wherever the tab bar is showing — the other
     * tabs all carry it, so this is the way back without guessing how many
     * times to go back.
     */
    public HomePage openHomeTab() {

        click(HomePageLocators.TAB_HOME);

        return this;
    }

    public HomePage openWishList() {

        click(HomePageLocators.TAB_WISHLIST);

        return this;
    }

}
