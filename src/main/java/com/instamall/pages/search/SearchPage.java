package com.instamall.pages.search;

import com.instamall.base.BasePage;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class SearchPage extends BasePage {

    public SearchPage waitForScreen() {

        waitForVisibility(SearchPageLocators.TITLE);
        WaitUtils.waitForPresence(SearchPageLocators.SEARCH_FIELD);

        return this;
    }

    /**
     * Types the query character at a time and verifies it landed — a bulk
     * sendKeys was confirmed to drop characters here, the same as every
     * other text field in this app ("Ema Savahl Couture" landed as "Eavahl
     * Couture").
     */
    public SearchPage search(String query) {

        typeInto(SearchPageLocators.SEARCH_FIELD, query);

        return this;
    }

    public SearchPage openMallsTab() {

        click(SearchPageLocators.MALLS_TAB);

        return this;
    }

    /**
     * Switches to the Stores tab — required before searching for a store
     * by name, since the screen opens on Products and a store name search
     * there comes back "No pieces found".
     */
    public SearchPage openStoresTab() {

        click(SearchPageLocators.STORES_TAB);

        return this;
    }

    public SearchPage openProductsTab() {

        click(SearchPageLocators.PRODUCTS_TAB);

        return this;
    }

    /**
     * Opens a Stores-tab result by its exact row text — see
     * {@link SearchPageLocators#storeResult(String, String)} for why this
     * has to be an exact match rather than a partial one.
     */
    public SearchPage openStoreResult(String storeName, String mallName) {

        By result = SearchPageLocators.storeResult(storeName, mallName);

        settle(result);
        click(result);

        return this;
    }

    /**
     * Opens a Stores-tab result by store name alone — see
     * {@link SearchPageLocators#storeResultContaining(String)}.
     */
    public SearchPage openStoreResultContaining(String storeName) {

        By result = SearchPageLocators.storeResultContaining(storeName);

        settle(result);
        click(result);

        return this;
    }

    private void typeInto(By locator, String text) {

        WebElement field = WaitUtils.waitForPresence(locator);

        field.clear();

        for (char character : text.toCharArray()) {

            field.sendKeys(String.valueOf(character));
        }

        String landed = field.getAttribute("value");

        if (!text.equals(landed)) {

            throw new IllegalStateException(
                    "Could not fill the search field correctly — it reads \""
                            + landed + "\" instead of \"" + text + "\"");
        }
    }

}
