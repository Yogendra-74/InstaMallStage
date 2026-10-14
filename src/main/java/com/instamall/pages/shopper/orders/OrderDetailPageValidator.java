package com.instamall.pages.shopper.orders;

import com.instamall.base.BasePage;

public class OrderDetailPageValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(OrderDetailPageLocators.RUN_PROGRESS_LABEL);
    }

    public boolean isStoreDisplayed(String brandUppercase) {

        return isDisplayed(OrderDetailPageLocators.storeName(brandUppercase));
    }

    public boolean isProductTitleDisplayed(String productName) {

        return isDisplayed(OrderDetailPageLocators.productTitle(productName));
    }

    /**
     * No-wait check, for branching on whichever state the order is
     * already in (e.g. re-running against an order carried over from a
     * previous pass, already past Got it / Accept this item?) rather
     * than assuming every run starts from a freshly opened item.
     */
    public boolean isGotItButtonDisplayedNow() {

        return isDisplayedNow(OrderDetailPageLocators.GOT_IT_BUTTON);
    }

    /**
     * True once GOT_IT_BUTTON has been answered and the item shows
     * "Approved" instead.
     */
    public boolean isItemApproved() {

        return isDisplayed(OrderDetailPageLocators.ITEM_STATUS_APPROVED);
    }

    public boolean isValidateAndProceedDisplayed() {

        return isDisplayed(OrderDetailPageLocators.VALIDATE_AND_PROCEED_BUTTON);
    }

    /**
     * True once a "Couldn't get" item's refund has gone through and the
     * item shows "Rejected" instead — the opposite-outcome twin of
     * {@link #isItemApproved()}.
     */
    public boolean isItemRejected() {

        return isDisplayed(OrderDetailPageLocators.ITEM_STATUS_REJECTED);
    }

    public boolean isCouldNotGetSheetDisplayed() {

        return isDisplayed(OrderDetailPageLocators.COULD_NOT_GET_SHEET_TITLE);
    }

    public boolean isIssueRefundConfirmationDisplayed() {

        return isDisplayed(OrderDetailPageLocators.ISSUE_REFUND_CONFIRM_TITLE);
    }

    public boolean isOrderProcessedAlertDisplayed() {

        return isDisplayed(OrderDetailPageLocators.ORDER_PROCESSED_TITLE);
    }

    public boolean isGotItSheetDisplayed() {

        return isDisplayed(OrderDetailPageLocators.GOT_IT_SHEET_TITLE);
    }


    public boolean isAcceptItemSheetDisplayed() {

        return isDisplayed(OrderDetailPageLocators.ACCEPT_ITEM_TITLE);
    }

    public boolean isConfirmPaymentSheetDisplayed() {

        return isDisplayed(OrderDetailPageLocators.CONFIRM_PAYMENT_TITLE);
    }

    public boolean isVirtualCardDisplayed() {

        return isDisplayed(OrderDetailPageLocators.VIRTUAL_CARD);
    }

    public boolean isReceiptCaptureScreenDisplayed() {

        return isDisplayed(OrderDetailPageLocators.RECEIPT_SCREEN_TITLE);
    }

    public boolean isStartHandoffSheetDisplayed() {

        return isDisplayed(OrderDetailPageLocators.START_HANDOFF_SHEET_TITLE);
    }

    public boolean isHandoffPhotoScreenDisplayed() {

        return isDisplayed(OrderDetailPageLocators.HANDOFF_PHOTO_HEADER);
    }

    /**
     * True once the Simulator's camera-less message is up on the handoff
     * photo screen — same underlying gap as the receipt-capture "Scan
     * failed" alert, see
     * OrderDetailPageLocators.CAMERA_NOT_AVAILABLE_MESSAGE's own doc
     * comment.
     */
    public boolean isCameraNotAvailableMessageDisplayed() {

        return isDisplayed(OrderDetailPageLocators.CAMERA_NOT_AVAILABLE_MESSAGE);
    }

}
