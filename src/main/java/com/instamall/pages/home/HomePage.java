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

        // Presence, not visibility: Home reopens wherever it was last
        // scrolled to, and its header can then be above the viewport
        // while still in the hierarchy.
        WaitUtils.waitForPresence(HomePageLocators.HOME_HEADER, FIRST_PAINT_TIMEOUT);

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
     * Opens a mall's own store directory from its "EXPLORE MALLS" card,
     * e.g. "Miami Design District" — confirmed live this opens a genuine
     * per-mall directory (see com.instamall.pages.malls.MallDetailPage).
     *
     * "EXPLORE MALLS" is the last section on the feed, below the fold on
     * a fresh load — click() alone timed out waiting for the card to
     * become clickable, since WDA does not report an off-screen element
     * as visible. Scrolled into view first, the same way a bag's
     * Subtotal or a second store group's buttons already are.
     *
     * Waited for with the same long, network-bound timeout as
     * listVisibleStoreCards() rather than WaitUtils' own 20s default:
     * "EXPLORE MALLS" is the last section to render on a cold guest
     * session, below "MIAMI'S FAVORITES" and "NEWEST TO INSTAMALL" —
     * seen live to still be loading past 20s on a fresh sign-up, which
     * read as the card never existing rather than just not yet.
     */
    public HomePage openMallCard(String mallName) {

        WebElement card = WaitUtils.waitForPresence(HomePageLocators.mallCard(mallName), FIRST_PAINT_TIMEOUT);

        scrollIntoView(card);

        card.click();

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
