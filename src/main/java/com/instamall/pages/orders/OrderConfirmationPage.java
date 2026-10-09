package com.instamall.pages.orders;

import com.instamall.base.BasePage;
import com.instamall.utils.CurrencyUtils;
import com.instamall.waits.WaitUtils;

import java.math.BigDecimal;
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
     * The total from the bottom bar. Scoped to that bar so the ordered
     * item's line price cannot be read instead.
     */
    public BigDecimal getTotal() {

        return CurrencyUtils.parse(getText(OrderConfirmationPageLocators.TOTAL_AMOUNT));
    }

}
