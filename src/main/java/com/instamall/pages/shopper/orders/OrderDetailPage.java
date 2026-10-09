package com.instamall.pages.shopper.orders;

import com.instamall.base.BasePage;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public class OrderDetailPage extends BasePage {

    /**
     * A permission alert (e.g. App Tracking Transparency) can arrive
     * after Home has already loaded — during or right after opening the
     * order card — well past ShopperLoginPage's own alert-polling, which
     * only covers login itself. Confirmed live: left unanswered, it sits
     * over this screen and a plain waitForVisibility() times out never
     * knowing why. Granted here the same way, rather than assumed away.
     */
    private static final Duration SCREEN_APPEARS_WITHIN = Duration.ofSeconds(20);

    public OrderDetailPage waitForScreen() {

        WaitUtils.waitUntil(driver -> {

            SystemAlerts.grantAnyShowingNow();

            return WaitUtils.isVisibleNow(OrderDetailPageLocators.ORDER_NUMBER_BUTTON)
                            && WaitUtils.isVisibleNow(OrderDetailPageLocators.VALIDATE_AND_PROCEED_BUTTON)
                    ? Boolean.TRUE
                    : null;

        }, SCREEN_APPEARS_WITHIN);

        return this;
    }

    /**
     * @return the order id, read off "Order number IM-XXXXX".
     */
    public String getOrderId() {

        return getText(OrderDetailPageLocators.ORDER_NUMBER_BUTTON)
                .replaceFirst("^Order number\\s*", "").trim();
    }

    /**
     * @return the run progress count, e.g. "0 of 1 done" / "1 of 1 done".
     */
    public String getRunProgressCount() {

        return getText(OrderDetailPageLocators.RUN_PROGRESS_COUNT);
    }

    // ---------- Step 4: confirm the item was found ----------

    public OrderDetailPage clickGotIt() {

        click(OrderDetailPageLocators.GOT_IT_BUTTON);

        return this;
    }

    public OrderDetailPage couldNotGetItem() {

        click(OrderDetailPageLocators.COULD_NOT_GET_ITEM);

        return this;
    }

    // ---------- "Couldn't get" reason sheet (opened by couldNotGetItem()) ----------

    public OrderDetailPage waitForCouldNotGetSheet() {

        settle(OrderDetailPageLocators.COULD_NOT_GET_SHEET_TITLE);

        return this;
    }

    /**
     * @param reasonLabel one of OrderDetailPageLocators'
     *                    {@code REASON_*} constants.
     */
    public OrderDetailPage chooseCouldNotGetReason(String reasonLabel) {

        click(OrderDetailPageLocators.reasonOption(reasonLabel));

        return this;
    }

    /**
     * Reads the amount off the sheet's own refund button rather than
     * assuming one, the same way getConfirmPaymentAmount() reads its own
     * sheet's live total — e.g. "ISSUE REFUND · $3,350.00 TO SUDHANSHU".
     */
    public String getIssueRefundButtonLabel() {

        return getText(OrderDetailPageLocators.ISSUE_REFUND_BUTTON);
    }

    public OrderDetailPage clickIssueRefund() {

        click(OrderDetailPageLocators.ISSUE_REFUND_BUTTON);

        return this;
    }

    // ---------- "Issue refund?" confirmation (opened by clickIssueRefund()) ----------

    public OrderDetailPage waitForIssueRefundConfirmation() {

        settle(OrderDetailPageLocators.ISSUE_REFUND_CONFIRM_TITLE);

        return this;
    }

    public OrderDetailPage confirmIssueRefund() {

        click(OrderDetailPageLocators.ISSUE_REFUND_CONFIRM_PROCEED);

        return this;
    }

    // ---------- "Order processed" alert — confirmed live: shown once
    // every item of the run has been rejected, and cancels the order
    // itself (see OrderDetailPageLocators' own doc comment). ----------

    public OrderDetailPage waitForOrderProcessedAlert() {

        settle(OrderDetailPageLocators.ORDER_PROCESSED_TITLE);

        return this;
    }

    public OrderDetailPage dismissOrderProcessedAlert() {

        click(OrderDetailPageLocators.ORDER_PROCESSED_OK_BUTTON);

        return this;
    }

    // ---------- Step 5: the "Got it" sheet, then "Accept this item?" ----------

    public OrderDetailPage waitForGotItSheet() {

        settle(OrderDetailPageLocators.GOT_IT_SHEET_TITLE);

        return this;
    }

    public OrderDetailPage confirmGotItSheet() {

        click(OrderDetailPageLocators.GOT_IT_SHEET_CONFIRM_BUTTON);

        return this;
    }

    // ---------- Price edit, from the "Got it" sheet (opened via
    // OrderDetailPageLocators.PRICE_EDIT_ICON's own doc comment) ----------

    /**
     * Turns the Per Unit Price value editable. Must be called before
     * {@link #getPerUnitPrice()} or {@link #setPerUnitPrice(String)} —
     * the value is a plain StaticText until this is tapped.
     */
    public OrderDetailPage openPriceEditor() {

        click(OrderDetailPageLocators.PRICE_EDIT_ICON);

        return this;
    }

    /**
     * @return the per-unit price field's current value, e.g. "725.00" —
     *         openPriceEditor() must have already been called.
     */
    public String getPerUnitPrice() {

        return WaitUtils.waitForPresence(OrderDetailPageLocators.PRICE_EDIT_FIELD).getText();
    }

    /**
     * Overwrites the per-unit price — openPriceEditor() must have
     * already been called. Confirmed live: a lower price here credits
     * the difference to the customer's InstaMall Wallet immediately,
     * and the Confirm Payment sheet's total drops accordingly (see
     * PRICE_EDIT_ICON's own doc comment).
     *
     * Verified then retyped character by character on a mismatch rather
     * than trusting a single bulk sendKeys — the same flakiness
     * EmailLoginPage/ShopperLoginPage's own typeInto() guards against
     * elsewhere in this suite, and a price is exactly the kind of value
     * a dropped character would corrupt silently (e.g. "725.00" landing
     * as "75.00").
     */
    public OrderDetailPage setPerUnitPrice(String newPrice) {

        WebElement field = WaitUtils.waitForPresence(OrderDetailPageLocators.PRICE_EDIT_FIELD);

        field.clear();
        field.sendKeys(newPrice);

        if (newPrice.equals(field.getText())) {

            return this;
        }

        WebElement retryField = WaitUtils.waitForPresence(OrderDetailPageLocators.PRICE_EDIT_FIELD);

        retryField.clear();

        for (char digit : newPrice.toCharArray()) {

            retryField.sendKeys(String.valueOf(digit));
        }

        return this;
    }

    public OrderDetailPage waitForAcceptItemSheet() {

        settle(OrderDetailPageLocators.ACCEPT_ITEM_TITLE);

        return this;
    }

    public OrderDetailPage proceedAcceptingItem() {

        click(OrderDetailPageLocators.ACCEPT_ITEM_PROCEED_BUTTON);

        return this;
    }

    // ---------- Step 6: validate the run and proceed to payment ----------

    public OrderDetailPage validateAndProceed() {

        click(OrderDetailPageLocators.VALIDATE_AND_PROCEED_BUTTON);

        return this;
    }

    // ---------- Step 7: confirm payment ----------

    public OrderDetailPage waitForConfirmPaymentSheet() {

        settle(OrderDetailPageLocators.CONFIRM_PAYMENT_TITLE);

        return this;
    }

    /**
     * @return the payable amount read off the sheet's own confirm
     *         button, e.g. "1,345.64" from "Confirm payment $1,345.64" —
     *         the button's name embeds the live total, so this reads it
     *         rather than assuming a fixed one.
     */
    public String getConfirmPaymentAmount() {

        String label = getText(OrderDetailPageLocators.ANY_CONFIRM_PAYMENT_BUTTON);

        return label.replaceFirst("^Confirm payment \\$", "").trim();
    }

    public OrderDetailPage confirmPayment(String amount) {

        click(OrderDetailPageLocators.confirmPaymentButton(amount));

        return this;
    }

    /**
     * Reads the amount off the sheet and taps its confirm button — the
     * usual way to call this, since the total is not known beforehand.
     */
    public OrderDetailPage confirmPayment() {

        return confirmPayment(getConfirmPaymentAmount());
    }

    // ---------- Step 8: "Capture the receipt" — see the locators' own
    // doc comment. REQUIRES A REAL DEVICE; not drivable on Simulator. ----------

    /**
     * Confirming payment is a real charge against the staging payment
     * gateway, not a local navigation — seen live to take well past the
     * default 20s wait on this backend (the checkout side of the house
     * shows the same pattern: placing an order there runs 30-65s).
     * Polls with SystemAlerts.grantAnyShowingNow() for the same reason as
     * waitForScreen() above — a permission alert landing between the tap
     * and this screen would otherwise sit there until the plain wait
     * times out with no idea why.
     */
    private static final Duration RECEIPT_SCREEN_APPEARS_WITHIN = Duration.ofSeconds(60);

    public OrderDetailPage waitForReceiptCaptureScreen() {

        WaitUtils.waitUntil(driver -> {

            SystemAlerts.grantAnyShowingNow();

            return WaitUtils.isVisibleNow(OrderDetailPageLocators.RECEIPT_SCREEN_TITLE)
                    ? Boolean.TRUE
                    : null;

        }, RECEIPT_SCREEN_APPEARS_WITHIN);

        return this;
    }

    // ---------- Start handoff (once a driver is assigned) — opened by
    // ShopperHomePage.tapStartHandoff() ----------

    public OrderDetailPage waitForStartHandoffSheet() {

        settle(OrderDetailPageLocators.START_HANDOFF_SHEET_TITLE);

        return this;
    }

    public OrderDetailPage proceedStartHandoff() {

        click(OrderDetailPageLocators.START_HANDOFF_SHEET_PROCEED);

        return this;
    }

    // ---------- "Capture handoff photo" — see the locators' own doc
    // comment. REQUIRES A REAL DEVICE; not drivable on Simulator. ----------

    /**
     * Polls with SystemAlerts.grantAnyShowingNow() the same way
     * waitForScreen()/waitForReceiptCaptureScreen() do — a permission
     * alert landing between the sheet's Proceed and this screen would
     * otherwise sit there until a plain wait times out with no idea why.
     */
    public OrderDetailPage waitForHandoffPhotoScreen() {

        WaitUtils.waitUntil(driver -> {

            SystemAlerts.grantAnyShowingNow();

            return WaitUtils.isVisibleNow(OrderDetailPageLocators.HANDOFF_PHOTO_HEADER)
                    ? Boolean.TRUE
                    : null;

        }, SCREEN_APPEARS_WITHIN);

        return this;
    }

}
