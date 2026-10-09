package com.instamall.pages.payment;

import com.instamall.base.BasePage;

import java.time.Duration;

public class PaymentSheetValidator extends BasePage {

    private static final Duration SHEET_TIMEOUT = Duration.ofSeconds(45);

    public boolean isDisplayed() {

        return isDisplayed(PaymentSheetLocators.PAY_BUTTON, SHEET_TIMEOUT);
    }

    /**
     * Open at this instant, without waiting — for racing the sheet
     * against the toast that says the delivery was refused.
     */
    public boolean isDisplayedNow() {

        return isDisplayedNow(PaymentSheetLocators.PAY_BUTTON);
    }

    /**
     * Guard rail: staging must never present a live payment sheet. If this
     * is false on a staging run, stop the test rather than pay for real.
     */
    public boolean isTestModeBadgeDisplayed() {

        return isDisplayed(PaymentSheetLocators.TEST_MODE_BADGE);
    }

    public boolean isSavedCardDisplayed(String lastFourDigits) {

        return isDisplayed(PaymentSheetLocators.savedCard(lastFourDigits));
    }

}
