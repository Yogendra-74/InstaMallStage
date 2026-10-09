package com.instamall.pages.shopper.home;

import com.instamall.base.BasePage;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ShopperHomePage extends BasePage {

    /**
     * An order card's own CTA ("VIEW ORDER", "START HANDOFF" …) sits near
     * the bottom of the card, but iOS collapses the whole card into one
     * accessibility element with no separately-tappable sub-button —
     * confirmed against a page source dump, the same shape
     * WishlistPage.moveToBag() works around. A plain click() lands on the
     * element's centre, which is the card's (non-interactive) body text
     * and does nothing; tapping near the bottom instead is what the app
     * responds to.
     *
     * How far near the bottom is computed from the card's own height
     * rather than a fixed offset — confirmed live that a fixed one does
     * not generalize: an offset tuned against the shorter "IN PROGRESS"
     * card (height ~322pt) landed short of the CTA on the taller "READY
     * FOR HANDOFF" one (height ~482pt, with the driver-assigned progress
     * graphic and an estimated-pickup row the shorter card does not
     * have). This margin is measured up from the card's bottom edge
     * instead, which held for both.
     */
    private static final int ORDER_CARD_CTA_BOTTOM_MARGIN = 45;

    public ShopperHomePage waitForScreen() {

        waitForVisibility(ShopperHomePageLocators.ACTIVE_TAB);
        waitForVisibility(ShopperHomePageLocators.HISTORY_TAB);

        return this;
    }

    public ShopperHomePage openActiveTab() {

        click(ShopperHomePageLocators.ACTIVE_TAB);

        return this;
    }

    public ShopperHomePage openHistoryTab() {

        click(ShopperHomePageLocators.HISTORY_TAB);

        return this;
    }

    /**
     * @return the accessible names of every active order card currently on
     *         screen, each reading status, ETA, fulfilment type, customer
     *         first name, store and total.
     */
    public List<String> listActiveOrderCards() {

        List<String> cards = new ArrayList<>();

        for (WebElement card : DriverManager.getDriver()
                .findElements(ShopperHomePageLocators.ANY_ACTIVE_ORDER_CARD)) {

            String name = card.getAttribute("name");

            if (name != null && !name.isBlank()) {

                cards.add(name);
            }
        }

        return cards;
    }

    /**
     * Opens the active order card matching every one of the given
     * fragments — see
     * {@link ShopperHomePageLocators#orderCardContaining(String...)}.
     */
    public ShopperHomePage openOrderCardContaining(String... fragments) {

        tapCardCta(ShopperHomePageLocators.orderCardContaining(fragments));

        return this;
    }

    /**
     * Opens the order card that actually needs attention right now — the
     * entry point into the order-confirmation-and-approval flow (see
     * com.instamall.pages.shopper.orders.OrderDetailPage).
     *
     * Prefers an already-started "IN PROGRESS" run over the generic
     * locator: see ShopperHomePageLocators.IN_PROGRESS_CARD's own doc
     * comment for why a carried-over order can otherwise open its own
     * inert preview card instead.
     *
     * Gives that card a few seconds to render rather than checking this
     * instant: right after Home first loads, the order list itself can
     * still be fetching, and a zero-wait check here reads that gap as "no
     * in-progress card" and falls back to the ambiguous locator before
     * the real one ever appears.
     */
    public ShopperHomePage openNewOrderCard() {

        By cardLocator = WaitUtils.isVisible(ShopperHomePageLocators.IN_PROGRESS_CARD, Duration.ofSeconds(5))
                ? ShopperHomePageLocators.IN_PROGRESS_CARD
                : ShopperHomePageLocators.ANY_ACTIVE_ORDER_CARD;

        tapCardCta(cardLocator);

        return this;
    }

    /**
     * Taps "START HANDOFF" on the one card ready for it — see
     * ShopperHomePageLocators.READY_FOR_HANDOFF_CARD's own doc comment
     * for why this is not just another orderCardContaining() call.
     */
    public ShopperHomePage tapStartHandoff() {

        tapCardCta(ShopperHomePageLocators.READY_FOR_HANDOFF_CARD);

        return this;
    }

    private void tapCardCta(By cardLocator) {

        // A permission alert (e.g. App Tracking Transparency) can arrive
        // any time after Home loads — confirmed live, right around this
        // very tap — and would otherwise swallow it silently.
        SystemAlerts.grantAnyShowingNow();

        WebElement card = WaitUtils.waitForVisibility(cardLocator);

        // moveToElement's own offset is relative to the element's centre,
        // so half the card's height gets to its bottom edge; back off
        // ORDER_CARD_CTA_BOTTOM_MARGIN from there rather than the edge
        // itself, since the CTA has some height of its own.
        int offsetFromCentre = (card.getRect().getHeight() / 2) - ORDER_CARD_CTA_BOTTOM_MARGIN;

        new Actions(DriverManager.getDriver())
                .moveToElement(card, 0, offsetFromCentre)
                .click()
                .perform();
    }

    public ShopperHomePage openReturns() {

        click(ShopperHomePageLocators.TAB_RETURNS);

        return this;
    }

    public ShopperHomePage openProfile() {

        click(ShopperHomePageLocators.TAB_PROFILE);

        return this;
    }

    public ShopperHomePage openHomeTab() {

        click(ShopperHomePageLocators.TAB_HOME);

        return this;
    }

    public ShopperHomePage openShiftStatusOptions() {

        click(ShopperHomePageLocators.SHIFT_STATUS_TOGGLE);

        return this;
    }

    /**
     * Waits, without failing, for the dashboard to be up — for a caller
     * racing it against something else (a system alert, a different
     * screen) rather than asserting on it directly.
     */
    public static boolean isDisplayedNow() {

        return WaitUtils.isVisibleNow(ShopperHomePageLocators.ACTIVE_TAB);
    }

}
