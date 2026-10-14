package com.instamall.pages.store;

import com.instamall.base.BasePage;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.driver.DriverManager;
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
