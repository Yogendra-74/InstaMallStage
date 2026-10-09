package com.instamall.pages.orders;

import com.instamall.base.BasePage;

public class OrdersListPage extends BasePage {

    public OrdersListPage waitForScreen() {

        waitForVisibility(OrdersListPageLocators.SCREEN_TITLE);

        return this;
    }

    /**
     * No-wait check: true once the named order's row is on screen right
     * now. An order never placed through this account — or not yet
     * synced — never will show up, however long this waited instead.
     */
    public boolean isOrderListed(String orderId) {

        return isDisplayedNow(OrdersListPageLocators.orderRow(orderId));
    }

    /**
     * No-wait check: true once the named order's row carries the given
     * status (e.g. "Delivered") right now — see
     * OrdersListPageLocators.orderRowWithStatus() for why this is a
     * CONTAINS match on the row's whole name rather than reading a
     * separate "Status" element.
     */
    public boolean hasStatus(String orderId, String status) {

        return isDisplayedNow(OrdersListPageLocators.orderRowWithStatus(orderId, status));
    }

    /**
     * @return the row's full accessible name, e.g. "IM-AE078 Delivered
     *         Burberry" — the status can be read off it rather than
     *         needing a separate locator per status.
     */
    public String getOrderRowText(String orderId) {

        return waitForVisibility(OrdersListPageLocators.orderRow(orderId))
                .getAttribute("name");
    }

}
