package com.instamall.pages.bag;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.utils.CurrencyUtils;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ShoppingBagPage extends BasePage {

    /**
     * Enough removals for any bag a run could leave behind, and a stop if
     * Remove ever turns out not to remove anything.
     */
    private static final int REMOVAL_LIMIT = 20;

    /**
     * How far below an item's title its own pieces can sit. A row is
     * title, variant, price, stepper, then Save for later / Remove, about
     * 190pt tall; the next store group's header starts well past that.
     */
    private static final int ROW_HEIGHT = 200;

    /**
     * One item as the bag lays it out: the pieces are separate siblings
     * in the list, so they are gathered here by position.
     */
    private static final class Row {

        final String product;
        final String variant;
        final int top;
        final WebElement removeButton;
        final Optional<String> price;

        Row(String product, String variant, int top,
            WebElement removeButton, Optional<String> price) {

            this.product = product;
            this.variant = variant;
            this.top = top;
            this.removeButton = removeButton;
            this.price = price;
        }

        /**
         * The size, read off the end of "Vineyard Red · M". A one-size
         * product's line is just the colour, which the product page calls
         * "One Size".
         */
        String size() {

            int dot = variant.lastIndexOf(" · ");

            return dot < 0 ? "One Size" : variant.substring(dot + 3).trim();
        }

        boolean matches(String productName, String size) {

            String wanted = ShoppingBagPageLocators.longestPlainRun(productName);

            return product.toLowerCase().contains(wanted.toLowerCase())
                    && (size == null || size.isEmpty() || size().equalsIgnoreCase(size));
        }

        @Override
        public String toString() {

            return product + " " + variant;
        }
    }

    public ShoppingBagPage waitForScreen() {

        waitForVisibility(ShoppingBagPageLocators.TITLE);
        waitForVisibility(ShoppingBagPageLocators.CHECKOUT_BUTTON);

        return this;
    }

    /**
     * The bag's Subtotal — items only, before delivery, tax and fees. It
     * used to be printed on the Checkout button; now it is the first row
     * of the summary, and the amount beside the label is the one on the
     * same line.
     */
    public BigDecimal getCheckoutAmount() {

        // Presence, not visibility: with more than one store group the
        // summary sits below the fold, and the amount is read by attribute.
        Rectangle label = WaitUtils.waitForPresence(ShoppingBagPageLocators.SUBTOTAL_LABEL).getRect();

        return amounts().stream()
                .filter(amount -> Math.abs(amount.getRect().getY() - label.getY()) < 5)
                .findFirst()
                .map(amount -> CurrencyUtils.parse(amount.getAttribute("name")))
                .orElseThrow(() -> new NoSuchElementException(
                        "No amount on the Subtotal line of the bag"));
    }

    /**
     * Price of one variant, read off its own row.
     *
     * Size narrows the match without being required — a store group holds
     * everything bought from that store, so one product can appear under
     * several sizes.
     */
    public BigDecimal getItemPrice(String productName, String size) {

        Row row = rowFor(productName, size)
                .orElseThrow(() -> new NoSuchElementException(
                        "No bag row for \"" + productName + "\" in size " + size));

        return CurrencyUtils.parse(row.price.orElseThrow(() -> new NoSuchElementException(
                "The bag row for \"" + productName + "\" carries no price")));
    }

    /**
     * Whether one variant is in the bag.
     */
    public boolean hasItem(String productName, String size) {

        return rowFor(productName, size).isPresent();
    }

    /**
     * Every row the app has marked out of stock, as
     * "&lt;product&gt; &lt;colour&gt; · &lt;size&gt;".
     */
    public List<String> listOutOfStockItems() {

        List<String> items = new ArrayList<>();

        for (Row row : outOfStockRows()) {

            items.add(row.toString());
        }

        return items;
    }

    /**
     * Asked immediately, with no wait of its own — callers that need to
     * give it time poll it themselves.
     *
     * Worth knowing before using this: the app does not mark an unsellable
     * line while the bag is merely open. It validates stock when checkout
     * is tapped, and annotates the row only once it has refused to leave.
     * So this answers false on a bag that is about to be rejected, and
     * that is the app's behaviour rather than a fault here — see how the
     * checkout attempt races it.
     */
    public boolean hasOutOfStockItems() {

        return !DriverManager.getDriver()
                .findElements(ShoppingBagPageLocators.OUT_OF_STOCK_MESSAGE)
                .isEmpty();
    }

    /**
     * Waits for one variant's row to render.
     *
     * The bag paints its header and its checkout button before the rows
     * arrive, so waitForScreen alone does not mean the items are on
     * screen.
     */
    public ShoppingBagPage waitForItem(String productName, String size) {

        waitUntil(driver -> hasItem(productName, size) ? Boolean.TRUE : null);

        return this;
    }

    /**
     * Removes every out-of-stock row and returns what went, so a caller
     * can say what it cleared.
     *
     * One at a time, re-reading the bag after each: removing a row
     * re-lays out the group, which leaves references taken beforehand
     * pointing at the wrong element or at nothing.
     */
    public List<String> removeOutOfStockItems() {

        List<String> removed = new ArrayList<>();

        for (int attempt = 0; attempt < REMOVAL_LIMIT; attempt++) {

            List<Row> rows = outOfStockRows();

            if (rows.isEmpty()) {

                return removed;
            }

            Row row = rows.get(0);
            int before = rows.size();

            scrollIntoView(row.removeButton);
            row.removeButton.click();

            // Wait on the count, not on a fixed pause: if the tap missed
            // this is what says so, instead of the next removal silently
            // removing the same row again.
            waitUntil(driver -> outOfStockRows().size() < before);

            removed.add(row.toString());
        }

        throw new IllegalStateException(
                "Still finding out-of-stock rows after " + REMOVAL_LIMIT
                        + " removals — Remove is not clearing them");
    }

    /**
     * Removes one variant with its Remove button.
     */
    public ShoppingBagPage removeItem(String productName, String size) {

        Row row = rowFor(productName, size)
                .orElseThrow(() -> new NoSuchElementException(
                        "No bag row for \"" + productName + "\" in size " + size));

        scrollIntoView(row.removeButton);
        row.removeButton.click();

        waitUntil(driver -> hasItem(productName, size) ? null : Boolean.TRUE);

        return this;
    }

    public String getQuantity() {

        return getText(ShoppingBagPageLocators.QUANTITY);
    }

    /**
     * Taps the first "Review delivery &amp; checkout" on screen. Each
     * store group carries its own, so a bag holding several stores
     * should go through clickCheckoutFor instead.
     */
    public ShoppingBagPage clickCheckout() {

        click(ShoppingBagPageLocators.CHECKOUT_BUTTON);

        return this;
    }

    /**
     * Taps the checkout button of the store group holding one variant —
     * the first "Review delivery &amp; checkout" below its row.
     */
    public ShoppingBagPage clickCheckoutFor(String productName, String size) {

        Row row = rowFor(productName, size)
                .orElseThrow(() -> new NoSuchElementException(
                        "No bag row for \"" + productName + "\" in size " + size));

        WebElement button = firstBelow(
                DriverManager.getDriver().findElements(ShoppingBagPageLocators.CHECKOUT_BUTTON),
                row.top)
                .orElseThrow(() -> new NoSuchElementException(
                        "No checkout button under the store group holding \"" + productName + "\""));

        scrollIntoView(button);
        button.click();

        return this;
    }

    /**
     * Leaves the bag through the header chevron, back to whatever opened
     * it.
     */
    public ShoppingBagPage closeBag() {

        click(ShoppingBagPageLocators.CLOSE_BUTTON);

        return this;
    }

    // ---------- putting rows together ----------

    private Optional<Row> rowFor(String productName, String size) {

        return rows().stream()
                .filter(row -> row.matches(productName, size))
                .findFirst();
    }

    /**
     * Every item on screen, in list order.
     *
     * "Save for later" is the anchor: every row has exactly one, in the
     * item column. Above it in that column sit, in order, the title (a
     * text), the variant line (a Button — "Vineyard Red · M", or just the
     * colour on a one-size product) and the price; Remove is the button
     * on the same line as the anchor. Store-group headers are Buttons too,
     * but sit in the margin column and so are never taken for a variant.
     */
    private List<Row> rows() {

        List<WebElement> texts = elements(ShoppingBagPageLocators.ANY_TEXT);
        List<WebElement> buttons = elements(ShoppingBagPageLocators.ANY_ITEM_BUTTON);
        List<WebElement> amounts = amounts();
        List<WebElement> removes = elements(ShoppingBagPageLocators.REMOVE_BUTTON);

        List<Row> rows = new ArrayList<>();

        for (WebElement anchor : elements(ShoppingBagPageLocators.SAVE_FOR_LATER_BUTTON)) {

            Rectangle at = anchor.getRect();

            Optional<WebElement> variant = buttons.stream()
                    .filter(button -> sameColumn(button, at) && button.getRect().getY() < at.getY())
                    .max(Comparator.comparingInt(button -> button.getRect().getY()));

            Optional<WebElement> title = variant.flatMap(line -> texts.stream()
                    .filter(text -> sameColumn(text, at)
                            && text.getRect().getY() < line.getRect().getY())
                    .filter(text -> !String.valueOf(text.getAttribute("name")).startsWith("$"))
                    .max(Comparator.comparingInt(text -> text.getRect().getY())));

            Optional<WebElement> remove = removes.stream()
                    .filter(button -> Math.abs(button.getRect().getY() - at.getY()) < 8)
                    .findFirst();

            if (variant.isEmpty() || title.isEmpty() || remove.isEmpty()) {

                continue;
            }

            int variantY = variant.get().getRect().getY();

            Optional<String> price = amounts.stream()
                    .filter(amount -> sameColumn(amount, at)
                            && amount.getRect().getY() > variantY
                            && amount.getRect().getY() < at.getY())
                    .min(Comparator.comparingInt(amount -> amount.getRect().getY()))
                    .map(amount -> amount.getAttribute("name"));

            rows.add(new Row(title.get().getAttribute("name"), variant.get().getAttribute("name"),
                    title.get().getRect().getY(), remove.get(), price));
        }

        return rows;
    }

    /**
     * Rows with an out-of-stock message sitting inside them.
     */
    private List<Row> outOfStockRows() {

        List<WebElement> messages = elements(ShoppingBagPageLocators.OUT_OF_STOCK_MESSAGE);

        if (messages.isEmpty()) {

            return new ArrayList<>();
        }

        List<Row> flagged = new ArrayList<>();

        for (Row row : rows()) {

            boolean marked = messages.stream()
                    .map(message -> message.getRect().getY())
                    .anyMatch(y -> y >= row.top && y < row.top + ROW_HEIGHT);

            if (marked) {

                flagged.add(row);
            }
        }

        return flagged;
    }

    private List<WebElement> amounts() {

        return elements(ShoppingBagPageLocators.ANY_AMOUNT);
    }

    private static List<WebElement> elements(By locator) {

        return DriverManager.getDriver().findElements(locator);
    }

    private static boolean sameColumn(WebElement element, Rectangle reference) {

        return Math.abs(element.getRect().getX() - reference.getX()) < 5;
    }

    private static Optional<WebElement> firstBelow(List<WebElement> candidates, int y) {

        return candidates.stream()
                .filter(candidate -> candidate.getRect().getY() > y)
                .min(Comparator.comparingInt(candidate -> candidate.getRect().getY()));
    }

}
