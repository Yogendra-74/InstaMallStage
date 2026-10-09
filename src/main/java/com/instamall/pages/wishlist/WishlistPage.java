package com.instamall.pages.wishlist;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WishlistPage extends BasePage {

    /**
     * How far below a card's centre its MOVE TO BAG strip sits.
     *
     * The card is 370 tall and names its contents in render order, ending
     * with the button — so the strip is the bottom band. Stopping short of
     * the half-height keeps the tap inside both the button and the card.
     */
    private static final int MOVE_TO_BAG_OFFSET_Y = 155;

    public WishlistPage waitForScreen() {

        waitForVisibility(WishlistPageLocators.TITLE);

        return this;
    }

    public WishlistPage openProductsTab() {

        click(WishlistPageLocators.PRODUCTS_TAB);

        return this;
    }

    /**
     * Saved cards in the order the grid lays them out — top row first, and
     * left before right within a row.
     *
     * Reading order matters here: the app puts the most recently saved
     * product first, which is the thing a caller wants to check.
     */
    public List<String> listSavedProducts() {

        return savedCards().stream()
                .map(card -> card.getAttribute("name"))
                .collect(Collectors.toList());
    }

    /**
     * The most recently saved card's name, if anything is saved.
     */
    public Optional<String> firstSavedProduct() {

        return savedCards().stream()
                .findFirst()
                .map(card -> card.getAttribute("name"));
    }

    /**
     * Moves one saved product into the bag.
     *
     * The button has no element of its own — iOS collapses the card into a
     * single accessibility element — so it is tapped by offset from the
     * card's centre. An accessibility id on the button would replace this
     * with a plain locator.
     *
     * Lands on the product's own page, where the size is chosen and the
     * add is confirmed.
     */
    public WishlistPage moveToBag(String productName) {

        WebElement card = waitForVisibility(WishlistPageLocators.card(productName));

        new Actions(DriverManager.getDriver())
                .moveToElement(card, 0, MOVE_TO_BAG_OFFSET_Y)
                .click()
                .perform();

        return this;
    }

    /**
     * Waits a short while for a moved product to leave the wishlist.
     *
     * Moving an item to the bag should take it out of the wishlist — the
     * count in the header drops with it. Reported rather than asserted, so
     * a caller can say so and carry on: an item that lingers is worth
     * knowing about, but it is not worth abandoning a checkout over.
     *
     * @return true if the product is no longer saved
     */
    public boolean waitUntilNoLongerSaved(String productName, Duration within) {

        try {

            return WaitUtils.waitForInvisibility(
                    WishlistPageLocators.card(productName), within);

        } catch (TimeoutException stillThere) {

            return false;
        }
    }

    /**
     * How many products the header says are saved.
     *
     * While the count is changing the app shows the old and the new value
     * at once, so the largest is taken as the one still standing.
     */
    /**
     * The saved count of the pane on screen.
     *
     * Both panes carry a count — Products on screen, Brands parked off to
     * the right — and the off-screen one is in the tree too. Taking the
     * larger of the two, as this once did, handed back the Brands total
     * whenever it was the bigger number, and saving a product then looked
     * like no change at all. So only a count that is actually visible is
     * read, and the off-screen ones are a fallback for a pane still
     * sliding in.
     */
    public int savedCount() {

        List<WebElement> counts = DriverManager.getDriver()
                .findElements(WishlistPageLocators.SAVED_COUNT);

        if (counts.isEmpty()) {

            throw new NoSuchElementException("The wishlist is not showing a saved count");
        }

        List<WebElement> onScreen = counts.stream()
                .filter(WebElement::isDisplayed)
                .collect(Collectors.toList());

        return (onScreen.isEmpty() ? counts : onScreen).stream()
                .map(element -> element.getAttribute("name"))
                .filter(java.util.Objects::nonNull)
                .mapToInt(WishlistPage::leadingNumber)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No number in the saved count"));
    }

    private static int leadingNumber(String text) {

        String digits = text.replaceAll("^\\D*(\\d+).*$", "$1");

        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    /**
     * Cards in grid order: by row, then across.
     */
    private List<WebElement> savedCards() {

        return DriverManager.getDriver()
                .findElements(WishlistPageLocators.ANY_CARD)
                .stream()
                .sorted(Comparator
                        .comparingInt((WebElement card) -> card.getLocation().getY())
                        .thenComparingInt(card -> card.getLocation().getX()))
                .collect(Collectors.toList());
    }
}
