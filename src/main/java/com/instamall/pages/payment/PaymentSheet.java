package com.instamall.pages.payment;

import com.instamall.base.BasePage;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;

import java.time.Duration;

public class PaymentSheet extends BasePage {

    private static final Duration SHEET_TIMEOUT = Duration.ofSeconds(45);

    public PaymentSheet waitForScreen() {

        WaitUtils.waitForVisibility(PaymentSheetLocators.PAY_BUTTON, SHEET_TIMEOUT);

        return this;
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

    /**
     * Pays, whichever state the sheet is in.
     *
     * Confirmed live, both ways round. With a card on the account the
     * sheet shows it under "Saved" with an Edit pencil beside it, already
     * selected — so there is nothing to fill in and the pay button is
     * tapped straight away. With no card it opens on the card form
     * instead, and the number, expiry and CVC have to be typed first.
     * The Edit pencil is what tells the two apart.
     */
    public PaymentSheet payWith(String cardNumber, String expiry, String cvc) {

        if (isDisplayedNow(PaymentSheetLocators.EDIT_SAVED_CARD)) {

            System.out.println(">>> A card is already saved — paying with it");

        } else {

            System.out.println(">>> No saved card — entering the test card");

            typeCardDetail(PaymentSheetLocators.CARD_NUMBER_FIELD, cardNumber);
            typeCardDetail(PaymentSheetLocators.CARD_EXPIRY_FIELD, expiry);
            typeCardDetail(PaymentSheetLocators.CARD_CVC_FIELD, cvc);
        }

        return pay();
    }

    /**
     * One character at a time: a bulk sendKeys drops characters in these
     * fields — confirmed live, "Automation addres" arriving as "Aation
     * addres" on a field of the same kind — and the element is refetched
     * between characters because the form re-renders as it formats what
     * has been typed so far.
     */
    private void typeCardDetail(By field, String value) {

        WaitUtils.waitForVisibility(field).click();

        for (char character : value.toCharArray()) {

            WaitUtils.waitForPresence(field).sendKeys(String.valueOf(character));
        }
    }

    public PaymentSheet close() {

        click(PaymentSheetLocators.CLOSE_BUTTON);

        return this;
    }

}
