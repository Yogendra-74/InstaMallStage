package com.instamall.pages.bag;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.utils.ScreenSnapshot;
import com.instamall.utils.ScreenSnapshot.Item;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

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
     * How this build names the pieces of one bag row: every element
     * carries the same "bag_item_&lt;id&gt;_" prefix and a suffix saying
     * what it is, with the words a shopper reads in its label. Grouping
     * on that id is what rows() does, where it used to line pieces up by
     * their position on screen.
     */
    private static final String ITEM_PREFIX = "bag_item_";
    private static final String NAME_SUFFIX = "_name_text";
    private static final String VARIANT_SUFFIX = "_variant_button";
    private static final String REMOVE_SUFFIX = "_remove_button";

    // How the variant line spells itself: "Size: 43 Color: Chino Grey".
    private static final String SIZE_PREFIX = "Size:";
    private static final String COLOR_PREFIX = "Color:";

    /**
     * One item as the bag lays it out: the pieces are separate siblings
     * in the list, so they are gathered here by position.
     */
    private static final class Row {

        final String product;
        final String variant;
        final int top;
        final int removeButtonY;

        Row(String product, String variant, int top, int removeButtonY) {

            this.product = product;
            this.variant = variant;
            this.top = top;
            this.removeButtonY = removeButtonY;
        }

        /**
         * The size out of the variant line. This build writes it as
         * "Size: 43 Color: Chino Grey"; a product with nothing to choose
         * carries no "Size:" part at all, which the product page calls
         * "One Size".
         */
        String size() {

            int start = variant.indexOf(SIZE_PREFIX);

            if (start < 0) {

                return "One Size";
            }

            String afterSize = variant.substring(start + SIZE_PREFIX.length());
            int colour = afterSize.indexOf(COLOR_PREFIX);

            return (colour < 0 ? afterSize : afterSize.substring(0, colour)).trim();
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

            // The row was read off a snapshot, so the button is looked up
            // now, right before it is tapped: the one Remove on its line.
            WebElement removeButton = DriverManager.getDriver()
                    .findElements(ShoppingBagPageLocators.REMOVE_BUTTON).stream()
                    .filter(button -> Math.abs(button.getRect().getY() - row.removeButtonY) < 8)
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException(
                            "No Remove button on the line of " + row));

            scrollIntoView(removeButton);
            removeButton.click();

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
     * Taps the checkout button of the store group holding one variant —
     * the first "Review delivery &amp; checkout" below its row.
     *
     * The row's position and the button below it come from one snapshot;
     * the only per-element call left is finding the button to tap, by
     * its position. Confirmed in the Appium log: this screen used to
     * spend ~20s between "VIEW BAG" and this tap on about eighty
     * rect/name round trips — rows() built every row of the bag twice
     * over, one element attribute at a time — before the tap ever
     * landed. See ScreenSnapshot.
     */
    public ShoppingBagPage clickCheckoutFor(String productName, String size) {

        ScreenSnapshot screen = ScreenSnapshot.capture();

        Row row = rowFor(screen, productName, size)
                .orElseThrow(() -> new NoSuchElementException(
                        "No bag row for \"" + productName + "\" in size " + size));

        Item checkout = screen.where(item -> item.named(ShoppingBagPageLocators.CHECKOUT_BUTTON_TEXT)
                        && item.y > row.top)
                .stream()
                .min(Comparator.comparingInt(item -> item.y))
                .orElseThrow(() -> new NoSuchElementException(
                        "No checkout button under the store group holding \"" + productName + "\""));

        WebElement button = DriverManager.getDriver()
                .findElements(ShoppingBagPageLocators.CHECKOUT_BUTTON).stream()
                .filter(candidate -> Math.abs(candidate.getRect().getY() - checkout.y) < 8)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "The checkout button under \"" + productName + "\" moved before it could be tapped"));

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

        return rowFor(ScreenSnapshot.capture(), productName, size);
    }

    private Optional<Row> rowFor(ScreenSnapshot screen, String productName, String size) {

        return rows(screen).stream()
                .filter(row -> row.matches(productName, size))
                .findFirst();
    }

    /**
     * Every item on screen, in list order, read off one snapshot.
     *
     * Each row's pieces share a "bag_item_&lt;id&gt;_" prefix in their
     * accessible name, so a row is assembled by that id rather than by
     * lining elements up in the same column — which is what this did
     * before the app put test ids on them, and what broke when the names
     * a shopper reads moved from name to label.
     */
    private List<Row> rows(ScreenSnapshot screen) {

        List<Row> rows = new ArrayList<>();

        for (Item title : screen.where(item -> item.name != null
                && item.name.startsWith(ITEM_PREFIX)
                && item.name.endsWith(NAME_SUFFIX))) {

            String id = title.name.substring(ITEM_PREFIX.length(),
                    title.name.length() - NAME_SUFFIX.length());

            Optional<Item> variant = pieceOf(screen, id, VARIANT_SUFFIX);
            Optional<Item> remove = pieceOf(screen, id, REMOVE_SUFFIX);

            if (variant.isEmpty() || remove.isEmpty()) {

                continue;
            }

            rows.add(new Row(title.label, variant.get().label,
                    title.y, remove.get().y));
        }

        rows.sort(Comparator.comparingInt(row -> row.top));

        return rows;
    }

    /**
     * One named piece of the row with this id.
     */
    private Optional<Item> pieceOf(ScreenSnapshot screen, String id, String suffix) {

        return screen.where(item -> (ITEM_PREFIX + id + suffix).equals(item.name))
                .stream()
                .findFirst();
    }

    /**
     * Rows with an out-of-stock message sitting inside them.
     */
    private List<Row> outOfStockRows() {

        ScreenSnapshot screen = ScreenSnapshot.capture();

        List<Item> messages = screen.where(item -> item.isStaticText()
                && item.nameContainsIgnoreCase(ShoppingBagPageLocators.OUT_OF_STOCK_TEXT));

        if (messages.isEmpty()) {

            return new ArrayList<>();
        }

        List<Row> flagged = new ArrayList<>();

        for (Row row : rows(screen)) {

            boolean marked = messages.stream()
                    .anyMatch(message -> message.y >= row.top && message.y < row.top + ROW_HEIGHT);

            if (marked) {

                flagged.add(row);
            }
        }

        return flagged;
    }

}
