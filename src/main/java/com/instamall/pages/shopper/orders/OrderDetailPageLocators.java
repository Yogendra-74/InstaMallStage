package com.instamall.pages.shopper.orders;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * The order-confirmation-and-approval journey, from the order detail
 * screen (opened via ShopperHomePage.openNewOrderCard()) through Got
 * it -> Accept this item? -> Confirm payment -> Capture the receipt.
 * Verified against page source dumps taken from the live screens.
 *
 * The last stage, receipt capture, is Apple's own
 * VNDocumentCameraViewController (a system component, not app UI) and
 * cannot be driven to completion on the iOS Simulator — there is no
 * camera hardware for it to capture from, and unlike a photo-library
 * picker it has no non-camera fallback. Its locators are still captured
 * below for when this is run against a real device.
 */
public final class OrderDetailPageLocators {

    private OrderDetailPageLocators() {

    }

    // ---------- Order detail screen ----------

    public static final By BACK_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == 'BACK' AND accessible == 1"
            );

    public static final By OPEN_CHAT =
            AppiumBy.accessibilityId("Open chat");

    /**
     * Reads "Order number IM-XXXXX" — the id can be read off this same
     * element rather than needing a separate locator per order.
     */
    public static final By ORDER_NUMBER_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Order number' AND accessible == 1"
            );

    public static final By FULFILMENT_TYPE_BADGE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'DELIVERY' AND accessible == 1"
            );

    public static final By CUSTOMER_INFO_TOGGLE =
            AppiumBy.accessibilityId("Customer info");

    public static final By STATUS_IN_PROGRESS =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'IN PROGRESS' AND accessible == 1"
            );

    public static final By STORE_LOCATION_BUTTON =
            AppiumBy.accessibilityId("Store location");

    public static By storeName(String brandUppercase) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + brandUppercase + "' AND accessible == 1"
        );
    }

    public static final By RUN_PROGRESS_LABEL =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'RUN PROGRESS' AND accessible == 1"
            );

    /**
     * "0 of 1 done" / "1 of 1 done" — read this rather than matching an
     * exact count, since the run can hold more than one item.
     */
    public static final By RUN_PROGRESS_COUNT =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name ENDSWITH ' done' AND accessible == 1"
            );

    public static final By VIEW_PRODUCT_BUTTON =
            AppiumBy.accessibilityId("VIEW PRODUCT");

    public static By productTitle(String productName) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == '" + productName + "' AND accessible == 1"
        );
    }

    public static final By GOT_IT_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'GOT IT' AND accessible == 1"
            );

    /**
     * "Couldn't get this item" — matched on a fragment without the
     * apostrophe rather than the full string, since iOS may render it as
     * either a straight or curly quote.
     */
    public static final By COULD_NOT_GET_ITEM =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS 'get this item' AND accessible == 1"
            );

    public static final By VALIDATE_AND_PROCEED_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Validate and proceed' AND accessible == 1"
            );

    /**
     * Once GOT_IT_BUTTON is answered, this replaces it with an "Approved"
     * pill under a "STATUS" header.
     */
    public static final By ITEM_STATUS_APPROVED =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Approved' AND accessible == 1"
            );

    /**
     * Once COULD_NOT_GET_ITEM's sheet has issued its refund, this replaces
     * GOT_IT_BUTTON with a "Rejected" pill under the same "STATUS" header
     * ITEM_STATUS_APPROVED uses for the opposite outcome.
     */
    public static final By ITEM_STATUS_REJECTED =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Rejected' AND accessible == 1"
            );

    // ---------- "Couldn't get" reason sheet (opened by COULD_NOT_GET_ITEM) ----------

    /**
     * The sheet's own small header, "Couldn't get" — matched by a prefix
     * that stops short of the apostrophe (same reasoning as
     * COULD_NOT_GET_ITEM) and an "ends with 'get'" check, since
     * COULD_NOT_GET_ITEM's own longer "Couldn't get this item" also
     * begins the same way and can still be in the tree behind this sheet.
     */
    public static final By COULD_NOT_GET_SHEET_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Couldn' AND name ENDSWITH 'get' AND accessible == 1"
            );

    /**
     * The five fixed reasons the sheet offers, exactly as the app labels
     * them — confirmed against the live screen. Reasons are not scoped to
     * a product line (the sheet is for one item at a time already), so
     * these are plain constants rather than built per call.
     */
    public static final String REASON_OUT_OF_STOCK_SIZE = "Out of stock — size";
    public static final String REASON_OUT_OF_STOCK_COLOR = "Out of stock — color";
    public static final String REASON_OUT_OF_STOCK_ENTIRELY = "Out of stock entirely";
    public static final String REASON_DAMAGED_OR_QUALITY_ISSUE = "Damaged / quality issue";
    public static final String REASON_WRONG_ITEM_IN_STORE_INVENTORY = "Wrong item in store inventory";

    public static By reasonOption(String reasonLabel) {

        return AppiumBy.iOSNsPredicateString(
                "name == '" + reasonLabel + "' AND accessible == 1"
        );
    }

    /**
     * "ISSUE REFUND · $3,350.00 TO SUDHANSHU" — the button's own
     * accessible name embeds the live amount and the customer's first
     * name, the same way {@link #ANY_CONFIRM_PAYMENT_BUTTON}'s own doc
     * comment explains for Confirm payment, so this is matched as a
     * prefix rather than built from a known amount.
     */
    public static final By ISSUE_REFUND_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'ISSUE REFUND' AND accessible == 1"
            );

    // ---------- "Issue refund?" confirmation (opened by ISSUE_REFUND_BUTTON) ----------

    public static final By ISSUE_REFUND_CONFIRM_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Issue refund?' AND accessible == 1"
            );

    public static final By ISSUE_REFUND_CONFIRM_PROCEED =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Proceed' AND accessible == 1"
            );

    public static final By ISSUE_REFUND_CONFIRM_CANCEL =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Cancel' AND accessible == 1"
            );

    // ---------- "Order processed" alert (after Validate and proceed with
    // every item in the run rejected — confirmed live: the order itself
    // is cancelled, same as ShopperHomePageLocators.NO_ACTIVE_ORDERS_MESSAGE
    // and ShopperHomePageLocators.orderCardContaining("ORDER REJECTED")
    // confirm from the Home screen afterward) ----------

    public static final By ORDER_PROCESSED_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Order processed' AND accessible == 1"
            );

    public static final By ORDER_PROCESSED_MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Order submitted with no items to pick up.' AND accessible == 1"
            );

    public static final By ORDER_PROCESSED_OK_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'OK' AND accessible == 1"
            );

    // ---------- "Got it" bottom sheet (opened by GOT_IT_BUTTON) ----------

    public static final By GOT_IT_SHEET_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Got it' AND accessible == 1"
            );

    /**
     * "BURBERRY · For Duel" — brand plus the customer's first name.
     */
    public static final By GOT_IT_SHEET_SUBTITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name CONTAINS[c] '· For ' AND accessible == 1"
            );

    public static final By PER_UNIT_PRICE_LABEL =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Per Unit Price' AND accessible == 1"
            );

    /**
     * The pencil beside the Per Unit Price value — confirmed live: a
     * small, unnamed 16x16 element (iOS renders it an Other, not a
     * Button). Tapping it turns the price from a plain StaticText into
     * an editable XCUIElementTypeTextField in place; nothing else on
     * this sheet shares that exact size, so geometry alone disambiguates
     * it without needing an accessible name the app does not give it.
     *
     * Confirmed live, from InstaMall's own in-app notification copy,
     * what editing this does: a lower price here credits the difference
     * to the customer's InstaMall Wallet immediately (push notification:
     * "The price of an item has decreased. The reduced amount will be
     * credited to your InstaMall Wallet.") and the Confirm Payment sheet
     * (ANY_CONFIRM_PAYMENT_BUTTON) reflects the new, lower total. A
     * higher price was not confirmed live the same way (see
     * ItemPriceIncreaseComplete's own doc comment for why) — per
     * instruction, the opposite assumption (the customer is not charged
     * more) is encoded the same way without that live confirmation.
     */
    public static final By PRICE_EDIT_ICON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND accessible == 1"
                            + " AND rect.width == 16 AND rect.height == 16"
            );

    /**
     * The Per Unit Price value once {@link #PRICE_EDIT_ICON} has turned
     * it editable, e.g. "725.00" — reused for both reading the current
     * price and overwriting it.
     */
    public static final By PRICE_EDIT_FIELD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField'"
            );

    /**
     * The sheet's own confirm button — SAME accessible name as
     * {@link #GOT_IT_BUTTON} on the page behind it ("GOT IT"), but a
     * different element type (Other here, Button there). Disambiguate by
     * type, not by name alone.
     */
    public static final By GOT_IT_SHEET_CONFIRM_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name == 'GOT IT' AND accessible == 1"
            );

    // ---------- "Accept this item?" sheet ----------

    public static final By ACCEPT_ITEM_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Accept this item?' AND accessible == 1"
            );

    public static final By ACCEPT_ITEM_SUBTITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Confirm you picked up this item.' AND accessible == 1"
            );

    /**
     * "Quantity: 1 of 1".
     */
    public static final By ACCEPT_ITEM_QUANTITY =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Quantity:' AND accessible == 1"
            );

    public static final By ACCEPT_ITEM_PROCEED_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Proceed' AND accessible == 1"
            );

    public static final By ACCEPT_ITEM_CANCEL_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Cancel' AND accessible == 1"
            );

    // ---------- "Confirm payment" sheet (opened by VALIDATE_AND_PROCEED_BUTTON) ----------

    public static final By CONFIRM_PAYMENT_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Confirm payment' AND accessible == 1"
            );

    /**
     * The virtual card shown, e.g. "VIRTUAL, INSTAMALL, •••• •••• ••••
     * 0294" — the last four digits can be read off this element rather
     * than needing a separate locator.
     */
    public static final By VIRTUAL_CARD =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'VIRTUAL, INSTAMALL' AND accessible == 1"
            );

    /**
     * "Confirm payment $&lt;total&gt;" — the button's accessible name
     * embeds the live total, so build this fresh each time rather than
     * caching it; the total is not known until this sheet is on screen.
     */
    public static By confirmPaymentButton(String amount) {

        return AppiumBy.iOSNsPredicateString(
                "name == 'Confirm payment $" + amount + "' AND accessible == 1"
        );
    }

    /**
     * Every candidate for the confirm-payment button, whatever amount it
     * carries — for reading the live total off the screen before it is
     * tapped, the same shape as CheckoutPageLocators.PLACE_ORDER_BUTTON.
     */
    public static final By ANY_CONFIRM_PAYMENT_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Confirm payment $' AND accessible == 1"
            );

    public static final By CONFIRM_PAYMENT_CANCEL_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Cancel' AND accessible == 1"
            );

    // ---------- "Capture the receipt" screen (the app's own screen) ----------
    //
    // Confirmed against a page source dump taken from the live screen.
    // Its own SCAN_RECEIPT_BUTTON opens Apple's VNDocumentCameraViewController
    // (a system component, not app UI, confirmed separately against its own
    // page source dump — see the system-camera locators below), which on
    // this Simulator fails immediately with a concrete, unambiguous error:
    // "Scan failed — The operation couldn't be completed.
    // (AVFoundationErrorDomain error -11800.)" — Apple's own "no camera
    // device" error, reproduced twice (once automatically on opening the
    // system camera view, once from an explicit manual shutter tap).
    // REQUIRES A REAL DEVICE: no software workaround exists for this on a
    // Simulator with no camera hardware.

    public static final By RECEIPT_SCREEN_HEADER =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS 'RECEIPT ·' AND accessible == 1"
            );

    public static final By RECEIPT_SCREEN_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Capture the receipt' AND accessible == 1"
            );

    public static final By RECEIPT_SCREEN_INSTRUCTIONS =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Position the receipt on a flat surface — edges are"
                            + " detected and cropped automatically.' AND accessible == 1"
            );

    /**
     * Opens the system document scanner (VNDocumentCameraViewController).
     * An {@code XCUIElementTypeOther}, not a Button — iOS renders it as a
     * plain accessible container here.
     */
    public static final By SCAN_RECEIPT_BUTTON =
            AppiumBy.accessibilityId("Scan receipt");

    /**
     * The app's own alert once VNDocumentCameraViewController reports back
     * a capture failure — reliably reproduced on the Simulator (no camera
     * device), not expected on a real device with a working camera.
     */
    public static final By SCAN_FAILED_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Scan failed' AND accessible == 1"
            );

    public static final By SCAN_FAILED_MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS 'AVFoundationErrorDomain' AND accessible == 1"
            );

    public static final By SCAN_FAILED_OK_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'OK' AND accessible == 1"
            );

    // ---------- "Capture the receipt" — the system camera view (VNDocumentCameraViewController) ----------
    //
    // Apple's own document scanner, confirmed against its own page source
    // dump to be system UI (springboard-level), not this app's own — same
    // gap as above. Its locators are still captured below for when this is
    // run against a real device: this same screen presented itself once
    // automatically right after CONFIRM_PAYMENT, then again on a manual
    // SCAN_RECEIPT_BUTTON tap, both times over a solid-black feed with no
    // camera preview.

    public static final By SYSTEM_CAMERA_CLOSE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Cancel' AND accessible == 1"
            );

    public static final By SHOW_FILTER_SETTINGS =
            AppiumBy.accessibilityId("Show filter settings");

    public static final By AUTOMATIC_CAPTURE_TOGGLE =
            AppiumBy.accessibilityId("Automatic document capture enabled");

    /**
     * The shutter. Its accessible name is "Scan document", not "Shutter"
     * — the visible "Shutter" label under it is a separate, non-
     * interactive caption.
     */
    public static final By SCAN_DOCUMENT_BUTTON =
            AppiumBy.accessibilityId("Scan document");

    public static final By POSITION_DOCUMENT_PROMPT =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Position the document in view.' AND accessible == 1"
            );

    // ---------- Beyond receipt capture — NOT YET CAPTURED (blocked by the camera gap above) ----------
    //
    // Per the requested flow, still to be mapped once reachable (real
    // device): "Keep" after the first image, a checkmark/tick to proceed,
    // "Use photo" to submit the receipt, "Proceed" to process the order,
    // "OK" to return home.

    // ---------- "Start handoff?" sheet (opened by ShopperHomePage.tapStartHandoff()) ----------

    public static final By START_HANDOFF_SHEET_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Start handoff?' AND accessible == 1"
            );

    public static final By START_HANDOFF_SHEET_PROCEED =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Proceed' AND accessible == 1"
            );

    public static final By START_HANDOFF_SHEET_CANCEL =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Cancel' AND accessible == 1"
            );

    // ---------- "Capture handoff photo" screen ----------
    //
    // The app's own camera screen (not Apple's VNDocumentCameraViewController
    // this time — confirmed against a page source dump: a plain custom
    // shutter UI, no system chrome), reached from the "Start handoff?"
    // sheet's Proceed. Same underlying gap as the receipt capture screen
    // though: the Simulator has no camera hardware, and this app is
    // explicit about it rather than erroring obliquely — it shows
    // CAMERA_NOT_AVAILABLE_MESSAGE outright instead of presenting a dead
    // shutter.

    public static final By HANDOFF_PHOTO_HEADER =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Capture handoff photo' AND accessible == 1"
            );

    // HANDOFF_PHOTO_CLOSE (the "X" top right of this screen) is NOT YET
    // CAPTURED — its accessible name was not confirmed against a page
    // source dump before the session ended. Dump one before relying on a
    // guessed locator for it.

    /**
     * "Camera is not available on this device." — confirmed live,
     * reliably reproduced on the Simulator. REQUIRES A REAL DEVICE past
     * this point, same as receipt capture.
     */
    public static final By CAMERA_NOT_AVAILABLE_MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS 'Camera is not available' AND accessible == 1"
            );

}
