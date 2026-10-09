package com.instamall.pages.portal;

import com.instamall.base.PortalBasePage;
import com.instamall.driver.PortalDriverManager;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.NoSuchElementException;

public class PortalOrdersPage extends PortalBasePage {

    /**
     * Clicks the left-nav "Orders" entry — usable from any portal screen,
     * not just this one; kept here since every other Orders-related
     * locator and method already lives on this page object.
     */
    public PortalOrdersPage openOrdersFromSidebar() {

        click(PortalOrdersPageLocators.SIDEBAR_ORDERS_LINK);

        return this;
    }

    public PortalOrdersPage openActiveTab() {

        click(PortalOrdersPageLocators.ACTIVE_TAB);

        return this;
    }

    public PortalOrdersPage openHistoryTab() {

        click(PortalOrdersPageLocators.HISTORY_TAB);

        return this;
    }

    /**
     * Searches the Active tab's box — confirmed live that this does NOT
     * appear to index this app's own order number (e.g. "IM-48575"
     * returned "No Records Found"); see PortalOrdersPageLocators' own doc
     * comment. Prefer opening the single matching card directly when
     * there is only one, over relying on this to find it by that number.
     */
    public PortalOrdersPage searchActiveOrders(String query) {

        type(PortalOrdersPageLocators.ACTIVE_SEARCH_INPUT, query);
        click(PortalOrdersPageLocators.SEARCH_BUTTON);

        return this;
    }

    /**
     * All order ids currently listed in the History tab's table, in row
     * order (includes "-exception-N" suffixed rows for retried orders).
     */
    public List<String> getAllOrderIds() {

        waitForVisibility(PortalOrdersPageLocators.ORDER_ID_LINKS_LIST);

        return PortalDriverManager.getDriver()
                .findElements(PortalOrdersPageLocators.ORDER_ID_LINKS_LIST)
                .stream()
                .map(WebElement::getText)
                .toList();
    }

    /**
     * Finds the row in the History tab's table whose order id ends with
     * the given suffix and clicks it — navigates in place (unlike
     * openFirstOrderCard()'s new-tab link) to that order's detail page.
     */
    public PortalOrdersPage openOrderEndingWith(String suffix) {

        waitForVisibility(PortalOrdersPageLocators.ORDER_ID_LINKS_LIST);

        WebElement match = PortalDriverManager.getDriver()
                .findElements(PortalOrdersPageLocators.ORDER_ID_LINKS_LIST)
                .stream()
                .filter(link -> link.getText().endsWith(suffix))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No order id ending with '" + suffix + "' found on this page"));

        match.click();

        return this;
    }

    public boolean isNoRecordsFoundShowing() {

        return isDisplayedNow(PortalOrdersPageLocators.NO_RECORDS_FOUND);
    }

    /**
     * Opens the first (only, in every case mapped so far) order card's
     * detail page and switches this page's driver to it — the link
     * itself opens in a new tab (target="_blank") rather than navigating
     * in place, so without this, subsequent calls would still be acting
     * on the Orders list tab, not the order detail one.
     *
     * @return the new tab's window handle, for switching back later via
     *         {@code PortalDriverManager.getDriver().switchTo().window(handle)}
     */
    public String openFirstOrderCard() {

        List<String> before = List.copyOf(PortalDriverManager.getDriver().getWindowHandles());

        click(PortalOrdersPageLocators.ORDER_ID_LINK);

        List<String> after = PortalDriverManager.getDriver().getWindowHandles().stream().toList();

        String newHandle = after.stream()
                .filter(handle -> !before.contains(handle))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Clicking the order id link did not open a new tab"));

        PortalDriverManager.getDriver().switchTo().window(newHandle);

        return newHandle;
    }

}
