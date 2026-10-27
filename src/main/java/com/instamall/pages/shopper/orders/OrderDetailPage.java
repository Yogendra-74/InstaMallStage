package com.instamall.pages.shopper.orders;

import com.instamall.base.BasePage;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebDriverException;

import java.time.Duration;
import java.util.Map;

public class OrderDetailPage extends BasePage {

    // Enough swipes to bring a later item's "Couldn't get this item" button
    // up from below the fold, with the no-movement check stopping earlier
    // the moment the bottom of the run is reached.
    private static final int MAX_SCROLL_SWIPES = 12;

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

            return WaitUtils.isVisibleNow(OrderDetailPageLocators.RUN_PROGRESS_LABEL)
                            && WaitUtils.isVisibleNow(OrderDetailPageLocators.VALIDATE_AND_PROCEED_BUTTON)
                    ? Boolean.TRUE
                    : null;

        }, SCREEN_APPEARS_WITHIN);

        return this;
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

    /**
     * Drags the run up until a pending item's "GOT IT" button is on
     * screen, stopping as soon as one shows or the bottom is reached — the
     * accept-side counterpart to {@link #scrollToCouldNotGetItem()}.
     *
     * A multi-item run lists each item down a scroll view, and once the
     * ones above it have been approved the next item's "GOT IT" sits below
     * the fold; iOS leaves it out of the accessibility tree until it is
     * scrolled into view, so a plain click on it finds nothing. Called
     * before each item so {@link #clickGotIt()} always has a button to tap.
     * Harmless when one is already on screen: the loop exits on its first
     * pass without swiping.
     */
    public OrderDetailPage scrollToGotItButton() {

        for (int swipe = 0; swipe < MAX_SCROLL_SWIPES; swipe++) {

            if (isDisplayedNow(OrderDetailPageLocators.GOT_IT_BUTTON)) {

                return this;
            }

            String before = DriverManager.getDriver().getPageSource();

            DriverManager.getDriver().executeScript("mobile: swipe", Map.of("direction", "up"));

            if (DriverManager.getDriver().getPageSource().equals(before)) {

                // Nothing moved — the bottom of the run is reached.
                break;
            }
        }

        return this;
    }

    public OrderDetailPage couldNotGetItem() {

        click(OrderDetailPageLocators.COULD_NOT_GET_ITEM);

        return this;
    }

    /**
     * Drags the run up until a pending item's "Couldn't get this item"
     * button is on screen, stopping as soon as one shows or the bottom is
     * reached.
     *
     * A multi-item run lists each item down a scroll view, and once the
     * first has been handled the next one's button sits below the fold —
     * iOS leaves it out of the accessibility tree until it is scrolled
     * into view, so a plain click on it finds nothing. Called before each
     * item so {@link #couldNotGetItem()} always has a button to tap.
     * Harmless when one is already on screen: the loop exits on its first
     * pass without swiping.
     */
    public OrderDetailPage scrollToCouldNotGetItem() {

        for (int swipe = 0; swipe < MAX_SCROLL_SWIPES; swipe++) {

            if (isDisplayedNow(OrderDetailPageLocators.COULD_NOT_GET_ITEM)) {

                return this;
            }

            String before = DriverManager.getDriver().getPageSource();

            DriverManager.getDriver().executeScript("mobile: swipe", Map.of("direction", "up"));

            if (DriverManager.getDriver().getPageSource().equals(before)) {

                // Nothing moved — the bottom of the run is reached.
                break;
            }
        }

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

    /**
     * Taps the pencil beside the "Per Unit Price" on the "Got it" sheet,
     * which opens the price field for editing (the keyboard comes up).
     * Used by the price-edit flows to change the item's unit price.
     */
    public OrderDetailPage clickEditPricePencil() {

        click(OrderDetailPageLocators.EDIT_PER_UNIT_PRICE_PENCIL);

        return this;
    }

    /**
     * Reads the currently-written Per Unit Price from the editable field
     * the pencil opened, as a number. The field's value carries no
     * currency symbol or thousands separators (e.g. "6100."), but any
     * that appear are stripped before parsing so the caller always gets a
     * plain amount.
     */
    public double getEditablePerUnitPrice() {

        String written = getText(OrderDetailPageLocators.EDIT_PER_UNIT_PRICE_FIELD);

        return Double.parseDouble(written.replaceAll("[^0-9.]", ""));
    }

    /**
     * Replaces whatever is in the editable Per Unit Price field with the
     * given amount.
     */
    public OrderDetailPage setPerUnitPrice(String amount) {

        clear(OrderDetailPageLocators.EDIT_PER_UNIT_PRICE_FIELD);
        type(OrderDetailPageLocators.EDIT_PER_UNIT_PRICE_FIELD, amount);

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

    /**
     * How long "Validate and proceed" gets to become ready after the
     * "Accept this item?" sheet is proceeded.
     *
     * Confirmed live: proceeding on "Accept this item?" drops a loading
     * overlay while the backend records the acceptance, and only once it
     * clears does "Validate and proceed" come back enabled. That backend
     * step runs well past the 3s short timeout the plain isDisplayed()
     * checks poll with — the same long-running-backend pattern the payment
     * and receipt screens already give headroom for — so it is waited for
     * explicitly here rather than asserted on a 3s poll that fires while
     * the overlay is still up.
     */
    private static final Duration VALIDATE_READY_WITHIN = Duration.ofSeconds(60);

    /**
     * Waits out the post-"Accept this item?" loading overlay until
     * "Validate and proceed" is visible and clickable. Throws if it never
     * becomes ready within {@link #VALIDATE_READY_WITHIN}.
     */
    public OrderDetailPage waitForValidateAndProceedReady() {

        WaitUtils.waitForClickable(
                OrderDetailPageLocators.VALIDATE_AND_PROCEED_BUTTON, VALIDATE_READY_WITHIN);

        return this;
    }

    /**
     * Waits until the RUN PROGRESS header reads "{@code done} of
     * {@code total} done" — the per-item signal that accepting an item has
     * registered and the loading overlay the acceptance drops has cleared.
     *
     * In a multi-item run "Validate and proceed" only comes ready once
     * every item is accepted, so it cannot be the wait between items;
     * each item's acceptance instead advances this count by one, and that
     * is what is waited on before moving to the next item. Given the same
     * {@link #VALIDATE_READY_WITHIN} headroom as the validate button, since
     * it is the same backend step clearing in between.
     */
    public OrderDetailPage waitForRunProgress(int done, int total) {

        String expected = done + " of " + total + " done";

        WaitUtils.waitUntil(driver -> {

            var counts = driver.findElements(OrderDetailPageLocators.RUN_PROGRESS_COUNT);

            return !counts.isEmpty() && expected.equals(counts.get(0).getText())
                    ? Boolean.TRUE
                    : null;

        }, VALIDATE_READY_WITHIN);

        return this;
    }

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
     * Taps the sheet's confirm button, whatever total it carries. Callers
     * that want to report the amount read it with
     * {@link #getConfirmPaymentAmount()} first.
     */
    public OrderDetailPage confirmPayment() {

        click(OrderDetailPageLocators.ANY_CONFIRM_PAYMENT_BUTTON);

        return this;
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

    /**
     * The Simulator's way past "Capture the receipt": the capture button
     * is not usable without a camera, so the staging build's "DEV ·
     * Sample receipt" bypass is tapped instead. Confirmed live: it lands
     * on the "Looks good?" review screen.
     */
    public OrderDetailPage useSampleReceipt() {

        click(OrderDetailPageLocators.DEV_SAMPLE_RECEIPT);

        WaitUtils.waitForVisibility(OrderDetailPageLocators.LOOKS_GOOD_TITLE);

        return this;
    }

    /**
     * USE PHOTO on "Looks good?" — opens the "Submit receipt?" sheet.
     */
    public OrderDetailPage usePhoto() {

        click(OrderDetailPageLocators.USE_PHOTO_BUTTON);

        WaitUtils.waitForVisibility(OrderDetailPageLocators.SUBMIT_RECEIPT_TITLE);

        return this;
    }

    /**
     * Proceed on "Submit receipt?" — uploads the receipt and submits the
     * order, ending on the "Order processed" message.
     */
    public OrderDetailPage submitReceipt() {

        click(OrderDetailPageLocators.SUBMIT_RECEIPT_PROCEED);

        WaitUtils.waitForVisibility(OrderDetailPageLocators.ORDER_PROCESSED_TITLE);

        return this;
    }

    /**
     * OK on "Order processed / Receipt uploaded and order submitted." —
     * back to Home, where the order's card now reads WAITING FOR DRIVER.
     */
    public OrderDetailPage dismissReceiptProcessed() {

        click(OrderDetailPageLocators.RECEIPT_PROCESSED_OK);

        return this;
    }

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

    /** How many times "USE PHOTO" is tapped before the handoff is failed. */
    private static final int USE_PHOTO_ATTEMPTS = 2;

    /** How long each "USE PHOTO" tap is given to open "Complete handoff?". */
    private static final Duration HANDOFF_SHEET_WITHIN = Duration.ofSeconds(20);

    /**
     * Taps "USE PHOTO" on the handoff "Looks good?" screen, which opens the
     * "Complete handoff?" sheet.
     *
     * This one tap is flaky — confirmed live, it is occasionally swallowed
     * while the "Looks good?" screen is still animating in, and the step
     * then times out with the sheet never opening. So it alone is given up
     * to {@link #USE_PHOTO_ATTEMPTS} attempts, re-tapped only when the
     * "Complete handoff?" sheet has not come up, and the handoff is failed
     * if it still has not after the last attempt. The other handoff taps are
     * not retried — only this one, per instruction.
     */
    public OrderDetailPage useHandoffPhoto() {

        for (int attempt = 1; attempt <= USE_PHOTO_ATTEMPTS; attempt++) {

            if (attempt > 1) {

                System.out.println(">>> \"USE PHOTO\" did not open \"Complete handoff?\" — tap attempt "
                        + attempt + " of " + USE_PHOTO_ATTEMPTS);
            }

            try {

                WaitUtils.waitForClickable(
                        OrderDetailPageLocators.USE_PHOTO_BUTTON, HANDOFF_SHEET_WITHIN).click();

            } catch (WebDriverException notClickableYet) {

                // The button never became clickable this pass (the flaky
                // "Looks good?" render) — let the next attempt try again.
                System.out.println(">>> \"USE PHOTO\" was not clickable this pass ("
                        + notClickableYet.getMessage() + ")");
                continue;
            }

            if (WaitUtils.isVisible(
                    OrderDetailPageLocators.COMPLETE_HANDOFF_PROCEED, HANDOFF_SHEET_WITHIN)) {

                return this;
            }
        }

        throw new AssertionError("\"USE PHOTO\" did not open \"Complete handoff?\" after "
                + USE_PHOTO_ATTEMPTS + " taps");
    }

}
