package com.instamall.pages.payment;

import com.instamall.base.BasePage;
import com.instamall.utils.CurrencyUtils;
import com.instamall.waits.WaitUtils;

import java.math.BigDecimal;
import java.time.Duration;

public class PaymentSheet extends BasePage {

    private static final Duration SHEET_TIMEOUT = Duration.ofSeconds(45);

    public PaymentSheet waitForScreen() {

        WaitUtils.waitForVisibility(PaymentSheetLocators.PAY_BUTTON, SHEET_TIMEOUT);

        return this;
    }

    /**
     * Final payable amount, read off the pay button ("Pay $962.49").
     * This is the number the order confirmation must agree with.
     */
    public BigDecimal getPayableAmount() {

        return CurrencyUtils.parse(getText(PaymentSheetLocators.PAY_BUTTON));
    }

    public PaymentSheet selectSavedCard(String lastFourDigits) {

        click(PaymentSheetLocators.savedCard(lastFourDigits));

        return this;
    }

    /**
     * Pays with whichever method is currently selected. On staging this
     * charges the test card, so every run creates a real staging order.
     */
    public PaymentSheet pay() {

        settle(PaymentSheetLocators.PAY_BUTTON);

        click(PaymentSheetLocators.PAY_BUTTON);

        return this;
    }

    public PaymentSheet payWithSavedCard(String lastFourDigits) {

        selectSavedCard(lastFourDigits);

        return pay();
    }

    public PaymentSheet close() {

        click(PaymentSheetLocators.CLOSE_BUTTON);

        return this;
    }

}
