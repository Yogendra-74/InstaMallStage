package com.instamall.pages.store;

import com.instamall.base.BasePage;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.driver.DriverManager;
import com.instamall.utils.ScreenSnapshot;
import com.instamall.utils.ScreenSnapshot.Item;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class StorePage extends BasePage {

    // The product grid loads over the network.
    private static final Duration CATALOG_TIMEOUT = Duration.ofSeconds(45);

    /**
     * Readiness gate for whichever store was opened, without needing to
     * know the brand up front.
     *
     * Polls for a stray permission alert while waiting, same discipline
     * as HomePage's and MallDetailPage's own waits — a delayed location/
     * notifications alert landing on this transition instead of an
     * earlier one would otherwise sit unanswered for the whole timeout
     * with nothing retrying it. A no-op everywhere this screen is
     * already reached with every alert pre-granted or already drained.
     */
    public StorePage waitForScreen() {

        WaitUtils.waitUntil(driver -> {

            SystemAlerts.grantAnyShowingNow();

            return WaitUtils.isVisibleNow(StorePageLocators.STORE_NAME)
                    && WaitUtils.isVisibleNow(StorePageLocators.ANY_PRODUCT_TILE)
                    ? Boolean.TRUE : null;

        }, CATALOG_TIMEOUT);

        return this;
    }

    /**
     * @return the brand this store page belongs to, read off the header.
     */
    public String getBrandName() {

        return getText(StorePageLocators.STORE_NAME).trim();
    }

    /**
     * @return the accessible names of the product tiles currently on
     *         screen, in grid order. Each reads
     *         "&lt;product&gt; &lt;brand&gt; $&lt;price&gt;".
     */
    public List<String> listVisibleProductTiles() {

        // No wait for a tile here: every caller reaches this straight after
        // waitForScreen() (or waitForGrid()), both of which already gate on
        // ANY_PRODUCT_TILE being visible, so a second wait only pays for one
        // more accessibility-tree snapshot before the grid can be read —
        // the single dead lookup in the store-to-product hop. The snapshot
        // below reads whatever is on the grid; an empty result is the
        // caller's to assert on, exactly as before.
        //
        // Read off one page-source snapshot rather than asking each tile
        // for its name in turn. A grid carries about twenty of them, and
        // at roughly 250ms per round trip that was about five seconds
        // spent before the first tile could even be opened; the snapshot
        // costs one call of about 130ms however many tiles there are.
        List<String> tiles = new ArrayList<>();

        for (Item tile : ScreenSnapshot.capture().where(StorePage::isProductTile)) {

            tiles.add(tile.name);
        }

        return tiles;
    }

    /**
     * One product tile, as against the containers that hold them.
     *
     * A tile's name carries exactly one price — "Cotton Pants Prada
     * $1,850". The wrappers around the grid repeat every tile's name in
     * one long string, so they carry a price for each, and the screen's
     * outermost element carries the whole page. Counting the prices is
     * what tells a tile from its container, and matters because a
     * snapshot keeps those containers where a predicate search dropped
     * them.
     */
    private static boolean isProductTile(Item item) {

        if (item.name == null || item.name.isBlank()) {

            return false;
        }

        return item.name.chars().filter(character -> character == '$').count() == 1;
    }

    /**
     * Opens a tile by its full accessible name, which is unique even when
     * two products share a display name at different prices.
     */
    public StorePage openProductTile(String tileName) {

        // A tile already on screen is tapped where it is — no scrolling.
        // Only a tile still off-screen (further down the grid) is scrolled
        // to first, since the visible-only productTile locator can never
        // match it. The scroll is deliberately gated on the tile NOT being
        // visible: the same product is listed twice — under its department
        // and again under ALL PRODUCTS — so scrolling unconditionally can
        // drag the grid all the way down to the off-screen copy even when a
        // copy is already in view, which is exactly the stray scrolling to
        // avoid.
        if (!WaitUtils.isVisibleNow(StorePageLocators.productTile(tileName))) {

            scrollIntoView(WaitUtils.waitForPresence(StorePageLocators.productTilePresent(tileName)));
        }

        settle(StorePageLocators.productTile(tileName));

        click(StorePageLocators.productTile(tileName));

        return this;
    }

    /**
     * The "View bag (n)" pill the app shows on this screen after an item
     * is added.
     */
    public StorePage clickViewBag() {

        WaitUtils.waitForVisibility(StorePageLocators.VIEW_BAG);

        click(StorePageLocators.VIEW_BAG);

        return this;
    }

    public StorePage goBack() {

        click(StorePageLocators.BACK_BUTTON);

        return this;
    }

    /**
     * Confirms the store grid is showing again after returning from a
     * product page.
     *
     * Unlike {@link #waitForScreen()}, this does not require the store
     * header: STORE_NAME is pinned to a fixed top band (see its own doc
     * comment), so a grid that came back scrolled down — after a product
     * opened below the fold — would never satisfy it without scrolling back
     * up first. The grid's priced tiles are a position-independent signal
     * that we are on the store and not the product page, which is all this
     * needs to confirm.
     */
    public StorePage waitForGrid() {

        WaitUtils.waitUntil(driver -> {

            SystemAlerts.grantAnyShowingNow();

            return WaitUtils.isVisibleNow(StorePageLocators.ANY_PRODUCT_TILE) ? Boolean.TRUE : null;

        }, CATALOG_TIMEOUT);

        return this;
    }

    /**
     * Whether the store grid's priced tiles are on screen right now, with no
     * waiting — the position-independent "we are on the store grid and not
     * the product page" check that {@link #waitForGrid()} waits on.
     */
    public boolean isGridShowingNow() {

        return WaitUtils.isVisibleNow(StorePageLocators.ANY_PRODUCT_TILE);
    }

    /**
     * Gives the grid a short budget to appear after a back tap, so a caller
     * retrying a swallowed tap does not re-tap the instant the grid is still
     * animating in. Returns whether the grid showed within that budget.
     */
    public boolean settleForGrid() {

        return WaitUtils.isVisible(StorePageLocators.ANY_PRODUCT_TILE, GRID_SETTLE_TIMEOUT);
    }

    private static final Duration GRID_SETTLE_TIMEOUT = Duration.ofSeconds(10);

}
