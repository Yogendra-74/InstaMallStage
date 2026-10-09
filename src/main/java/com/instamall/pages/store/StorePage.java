package com.instamall.pages.store;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class StorePage extends BasePage {

    // The product grid loads over the network.
    private static final Duration CATALOG_TIMEOUT = Duration.ofSeconds(45);

    public StorePage waitForScreen(String storeName) {

        WaitUtils.waitForVisibility(
                StorePageLocators.searchInStore(storeName), CATALOG_TIMEOUT);

        return this;
    }

    /**
     * Readiness gate for whichever store was opened, without needing to
     * know the brand up front.
     */
    public StorePage waitForScreen() {

        WaitUtils.waitForVisibility(StorePageLocators.SEARCH_BOX, CATALOG_TIMEOUT);

        return this;
    }

    /**
     * @return the brand this store page belongs to, read off the search
     *         box ("Search Burberry" -> "Burberry").
     */
    public String getBrandName() {

        String searchLabel = getText(StorePageLocators.SEARCH_BOX);

        return searchLabel.replaceFirst("^Search\\s+", "").trim();
    }

    /**
     * @return the accessible names of the product tiles currently on
     *         screen, in grid order. Each reads
     *         "&lt;product&gt; &lt;brand&gt; $&lt;price&gt;".
     */
    public List<String> listVisibleProductTiles() {

        WaitUtils.waitForVisibility(StorePageLocators.ANY_PRODUCT_TILE, CATALOG_TIMEOUT);

        List<String> tiles = new ArrayList<>();

        for (WebElement tile : DriverManager.getDriver()
                .findElements(StorePageLocators.ANY_PRODUCT_TILE)) {

            String name = tile.getAttribute("name");

            if (name != null && !name.isBlank()) {

                tiles.add(name);
            }
        }

        return tiles;
    }

    /**
     * Opens a tile by its full accessible name, which is unique even when
     * two products share a display name at different prices.
     */
    public StorePage openProductTile(String tileName) {

        settle(StorePageLocators.productTile(tileName));

        click(StorePageLocators.productTile(tileName));

        return this;
    }

    public StorePage openProduct(String productName) {

        // Grid images stream in and shift the tiles; let the tile settle
        // so the tap opens the intended product.
        settle(StorePageLocators.productCard(productName));

        click(StorePageLocators.productCard(productName));

        return this;
    }

    public StorePage searchInStore(String storeName, String query) {

        type(StorePageLocators.searchInStore(storeName), query);

        return this;
    }

    /**
     * @param occurrence 1-based, in the order the departments render
     *                   (MEN first, then WOMEN).
     */
    public StorePage openViewAll(int occurrence) {

        click(StorePageLocators.viewAll(occurrence));

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

}
