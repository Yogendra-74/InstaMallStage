package com.instamall.pages.search;

import com.instamall.base.BasePage;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class InStoreSearchPage extends BasePage {

    public InStoreSearchPage waitForScreen() {

        waitForVisibility(InStoreSearchPageLocators.TITLE);
        WaitUtils.waitForPresence(InStoreSearchPageLocators.SEARCH_FIELD);

        return this;
    }

    /**
     * Types the query character at a time and verifies it landed — see
     * {@link SearchPageLocators#SEARCH_FIELD} for the dropped-character
     * bug this works around.
     */
    public InStoreSearchPage search(String query) {

        typeInto(InStoreSearchPageLocators.SEARCH_FIELD, query);

        return this;
    }

    public InStoreSearchPage openCategory(String category) {

        click(InStoreSearchPageLocators.categoryChip(category));

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
                    "Could not fill the in-store search field correctly — it reads \""
                            + landed + "\" instead of \"" + text + "\"");
        }
    }

}
