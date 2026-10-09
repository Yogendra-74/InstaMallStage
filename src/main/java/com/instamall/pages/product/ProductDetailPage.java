package com.instamall.pages.product;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.pages.bag.ShoppingBagPageLocators;
import com.instamall.utils.CurrencyUtils;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The product page in either of its shapes — see the locators for the
 * two. Every size method asks which shape is on screen first, so callers
 * need not know how they got here.
 */
public class ProductDetailPage extends BasePage {

    private static final Duration PRODUCT_LOAD_TIMEOUT = Duration.ofSeconds(45);

    public ProductDetailPage waitForScreen() {

        WaitUtils.waitForVisibility(
                ProductDetailPageLocators.PRIMARY_ACTION_BUTTON, PRODUCT_LOAD_TIMEOUT);

        return this;
    }

    /**
     * Headline price. The PDP prints it without decimals ("$750") while
     * the bag prints "$750.00" — compare the parsed values, never the
     * strings.
     */
    public BigDecimal getPrice() {

        return CurrencyUtils.parse(getText(ProductDetailPageLocators.PRICE));
    }

    /**
     * @return the size line as the old size card read it — "Size M" once
     *         a size is chosen, "Size Select Size" while none is — so
     *         callers checking the selection keep working in both shapes.
     */
    public String getSizeCardText() {

        if (hasLegacySizeCard()) {

            return getText(ProductDetailPageLocators.SIZE_CARD_LEGACY);
        }

        String selected = getSelectedSize();

        return "Size " + (selected.isEmpty() ? "Select Size" : selected);
    }

    /**
     * @return the size currently chosen ("M", "One Size"), or an empty
     *         string while none is.
     */
    public String getSelectedSize() {

        if (hasLegacySizeCard()) {

            String value = getText(ProductDetailPageLocators.SIZE_CARD_LEGACY)
                    .replaceFirst("^Size\\s*", "").trim();

            return value.equalsIgnoreCase("Select Size") ? "" : value;
        }

        for (WebElement option : sizeButtons()) {

            if (isSelected(option)) {

                return option.getAttribute("name");
            }
        }

        return "";
    }

    /**
     * @return the colour line's text, e.g. "Color · Grain Beige" on the
     *         full page or "Color Grain Beige" on the compact sheet.
     */
    public String getColorCardText() {

        return getText(ProductDetailPageLocators.COLOR_CARD);
    }

    /**
     * Makes the sizes available to choose from: opens the Select Size
     * sheet on the compact shape, or brings the inline buttons on screen
     * on the full page.
     */
    public ProductDetailPage openSizeSheet() {

        if (hasLegacySizeCard()) {

            click(ProductDetailPageLocators.SIZE_CARD_LEGACY);

            // The sheet slides up over the page.
            settle(ProductDetailPageLocators.SIZE_SHEET_TITLE);

            return this;
        }

        revealSizes();

        return this;
    }

    /**
     * @return the sizes that can be picked right now, in the order shown.
     *         A size the store has run out of is left out.
     */
    public List<String> getAvailableSizes() {

        List<String> sizes = new ArrayList<>();

        if (WaitUtils.isVisibleNow(ProductDetailPageLocators.SIZE_SHEET_TITLE)) {

            // Rows read "M Only 1 left!", so only the leading token is the
            // size; the sheet's scrim and close control match too, unnamed.
            for (WebElement row : DriverManager.getDriver()
                    .findElements(ProductDetailPageLocators.SIZE_SHEET_OPTIONS)) {

                String label = row.getAttribute("name");

                if (label != null && !label.isBlank()) {

                    sizes.add(label.split(" ")[0]);
                }
            }

            return sizes;
        }

        for (WebElement option : sizeButtons()) {

            String label = option.getAttribute("name");

            if (label != null && !label.isBlank() && option.isEnabled()) {

                sizes.add(label);
            }
        }

        return sizes;
    }

    /**
     * Picks a size: a row of the open Select Size sheet, or an inline
     * button, whichever the page has.
     */
    public ProductDetailPage chooseSize(String size) {

        if (WaitUtils.isVisibleNow(ProductDetailPageLocators.SIZE_SHEET_TITLE)) {

            click(ProductDetailPageLocators.sizeSheetOption(size));

            // Sheet dismisses and the card now reads "Size <size>".
            WaitUtils.waitForInvisibility(ProductDetailPageLocators.SIZE_SHEET_TITLE);

            return this;
        }

        WebElement option = revealSizes(ProductDetailPageLocators.sizeOption(size));

        option.click();

        waitUntil(driver -> isSelected(
                driver.findElement(ProductDetailPageLocators.sizeOption(size))));

        // Back to the top, so the title, brand and headline price the
        // caller checks next are on screen again.
        scrollIntoView(WaitUtils.waitForPresence(ProductDetailPageLocators.PRICE));

        return this;
    }

    /**
     * Closes the Select Size sheet without choosing anything — used when a
     * product turns out to have no stock and the flow moves to the next
     * one. Nothing to do on the full page, which has no sheet.
     */
    public ProductDetailPage closeSizeSheet() {

        if (WaitUtils.isVisibleNow(ProductDetailPageLocators.SIZE_SHEET_TITLE)) {

            click(ProductDetailPageLocators.SIZE_SHEET_CLOSE);
            WaitUtils.waitForInvisibility(ProductDetailPageLocators.SIZE_SHEET_TITLE);
        }

        return this;
    }

    /**
     * Takes the wanted size when it is in stock and the first size that
     * is otherwise — the catalogue runs on single units ("Only 1 left!"),
     * so insisting on one size makes a test fail for a stock reason rather
     * than a real one.
     *
     * @return the size actually selected, which callers should use for
     *         every assertion downstream.
     */
    public String selectSizePreferring(String preferredSize) {

        openSizeSheet();

        List<String> available = getAvailableSizes();

        if (available.isEmpty()) {

            throw new IllegalStateException(
                    "The product is offering no size that can be picked");
        }

        String chosen = available.contains(preferredSize)
                ? preferredSize
                : available.get(0);

        if (!chosen.equals(preferredSize)) {

            System.out.println(">>> Size " + preferredSize + " is out of stock; sizes on"
                    + " offer are " + available + " — taking " + chosen);
        }

        chooseSize(chosen);

        return chosen;
    }

    /**
     * Adds the item to the bag, unless it is in there already.
     *
     * The bag lives on the account, not on the device, so wiping app data
     * does not empty it. When the variant on screen is already in the bag
     * the app shows "View bag (n)" where Add to bag would be — waiting for
     * a button that is not coming is what makes a flow hang.
     *
     * @return true if the item was added by this call, false if it was
     *         already in the bag.
     */
    public boolean addToBagIfNeeded() {

        if (isDisplayed(ProductDetailPageLocators.VIEW_BAG)) {

            return false;
        }

        // Floats over SIMILAR ITEMS and slides with the page.
        settle(ProductDetailPageLocators.ADD_TO_BAG);
        click(ProductDetailPageLocators.ADD_TO_BAG);

        // Two confirmations, depending on the page's shape: the full page
        // turns the button into "View bag (n)", while the compact sheet
        // closes and opens the bag itself.
        waitUntil(driver -> isBagConfirmationShowing() ? Boolean.TRUE : null);

        return true;
    }

    /**
     * Whether the app has confirmed an add — the "View bag (n)" button, or
     * the bag screen it opens straight into from the compact sheet.
     */
    boolean isBagConfirmationShowing() {

        return WaitUtils.isVisibleNow(ProductDetailPageLocators.VIEW_BAG)
                || WaitUtils.isVisibleNow(ShoppingBagPageLocators.TITLE);
    }

    /**
     * Waits for the product page to settle into one of its two states.
     *
     * A product the store can sell carries Add to bag (or View bag when it
     * is in the bag already). One it cannot carries "Temporarily
     * unavailable" instead, with no size card and no button anywhere — so
     * waiting for the button there pays the whole timeout and then looks
     * like a page that never loaded. Both are watched together and the
     * first to appear decides.
     *
     * @return true if the product can be bought
     */
    public boolean waitUntilBuyableOrUnavailable() {

        // Deliberately not a Boolean condition: FluentWait reads a false
        // Boolean as "not satisfied yet" and would poll on to the timeout
        // rather than reporting the unavailable answer.
        return waitUntil(driver -> {

            if (WaitUtils.isVisibleNow(ProductDetailPageLocators.PRIMARY_ACTION_BUTTON)) {

                return Sellability.BUYABLE;
            }

            return WaitUtils.isVisibleNow(ProductDetailPageLocators.TEMPORARILY_UNAVAILABLE)
                    ? Sellability.UNAVAILABLE
                    : null;
        }) == Sellability.BUYABLE;
    }

    private enum Sellability { BUYABLE, UNAVAILABLE }

    /**
     * Saves the product to the wishlist with the heart in the header.
     *
     * The heart is unnamed, so it is found by where it sits: the header is
     * the strip holding the back chevron, and the heart is the first of
     * its unnamed, accessible, visible children once the chevron itself is
     * left out (the share icon is the second).
     */
    public ProductDetailPage tapWishlistHeart() {

        WebElement header = DriverManager.getDriver()
                .findElements(ProductDetailPageLocators.HEADER)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No header on the product page to find the heart in"));

        WebElement chevron = DriverManager.getDriver()
                .findElement(ProductDetailPageLocators.BACK_BUTTON);

        List<WebElement> icons = header
                .findElements(ProductDetailPageLocators.HEADER_CHILDREN)
                .stream()
                .filter(child -> child.getAttribute("name") == null
                        || child.getAttribute("name").isEmpty())
                // The header's own background is an unnamed, visible child
                // too — full width, not accessible — and would be taken for
                // the first icon otherwise.
                .filter(child -> "true".equals(child.getAttribute("accessible")))
                .filter(WebElement::isDisplayed)
                .filter(child -> !child.getRect().equals(chevron.getRect()))
                .collect(Collectors.toList());

        if (icons.isEmpty()) {

            throw new NoSuchElementException(
                    "The product header exposed no unnamed icons, so the heart"
                            + " cannot be placed");
        }

        icons.get(0).click();

        return this;
    }

    public ProductDetailPage clickViewBag() {

        // Nothing to tap when the compact sheet has already opened the bag.
        if (WaitUtils.isVisibleNow(ShoppingBagPageLocators.TITLE)) {

            return this;
        }

        WaitUtils.waitForVisibility(ProductDetailPageLocators.VIEW_BAG);

        click(ProductDetailPageLocators.VIEW_BAG);

        return this;
    }

    public ProductDetailPage goBack() {

        click(ProductDetailPageLocators.BACK_BUTTON);

        return this;
    }

    // ---------- which shape is on screen ----------

    /**
     * Whether the page is the compact sheet, with a "Size …" card rather
     * than inline buttons.
     */
    boolean hasLegacySizeCard() {

        return WaitUtils.isVisibleNow(ProductDetailPageLocators.SIZE_CARD_LEGACY);
    }

    /**
     * Brings the size buttons on screen. On a product with a long title
     * they start out under the fixed Add to bag bar, where a tap cannot
     * reach them and WDA reports them as not visible.
     */
    private WebElement revealSizes() {

        return revealSizes(ProductDetailPageLocators.SIZE_OPTIONS);
    }

    private WebElement revealSizes(By sizeLocator) {

        WebElement option = WaitUtils.waitForPresence(sizeLocator);

        scrollIntoView(option);

        return option;
    }

    private List<WebElement> sizeButtons() {

        return DriverManager.getDriver()
                .findElements(ProductDetailPageLocators.SIZE_OPTIONS);
    }

    static boolean isSelected(WebElement sizeButton) {

        return "1".equals(sizeButton.getAttribute("value"));
    }

}
