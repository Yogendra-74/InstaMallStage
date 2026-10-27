package com.instamall.pages.orders;

import com.instamall.base.BasePage;
import com.instamall.waits.WaitUtils;

import java.time.Duration;

public class OrderConfirmationPage extends BasePage {

    // Payment authorisation plus order creation — the slowest hop in the
    // whole flow.
    private static final Duration ORDER_CREATION_TIMEOUT = Duration.ofSeconds(90);

    public OrderConfirmationPage waitForScreen() {

        WaitUtils.waitForVisibility(
                OrderConfirmationPageLocators.ORDER_ID, ORDER_CREATION_TIMEOUT);

        return this;
    }

    /**
     * @return the order reference without the "ORDER " prefix, e.g.
     *         "IM-0THZ8".
     */
    public String getOrderId() {

        String title = getText(OrderConfirmationPageLocators.ORDER_ID);

        return title.replaceFirst("(?i)^order:?\\s*", "").trim();
    }

    /**
     * Taps the order ID button to copy the complete order ID to the
     * clipboard.
     */
    public void copyOrderId() {

        click(OrderConfirmationPageLocators.ORDER_ID);
    }

}
