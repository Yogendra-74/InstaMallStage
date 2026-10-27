package com.instamall.pages.shopper.orders;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.util.List;

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

    // No locator for the "Order number IM-XXXXX" line on purpose: the
    // Shopper app flows act on whichever order is first in the queue and
    // never read, compare or carry the order id. The screen is recognised
    // by its RUN PROGRESS header and its controls instead.

    public static final By STATUS_IN_PROGRESS =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'IN PROGRESS' AND accessible == 1"
            );

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

    /**
     * Every reason the sheet offers, in the order the app lists them —
     * so a caller can pick one (e.g. at random) without restating the
     * labels. Add to this list when the app adds a reason, and
     * {@link #reasonOption(String)} resolves any of them to its row.
     */
    public static final List<String> ALL_REASONS = List.of(
            REASON_OUT_OF_STOCK_SIZE,
            REASON_OUT_OF_STOCK_COLOR,
            REASON_OUT_OF_STOCK_ENTIRELY,
            REASON_DAMAGED_OR_QUALITY_ISSUE,
            REASON_WRONG_ITEM_IN_STORE_INVENTORY);

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

    // ---------- "Order processed" alert (after Validate and proceed with
    // every item in the run rejected — confirmed live: the order itself
    // is cancelled, same as ShopperHomePageLocators.NO_ACTIVE_ORDERS_MESSAGE
    // and ShopperHomePageLocators.orderCardContaining("ORDER REJECTED")
    // confirm from the Home screen afterward) ----------

    public static final By ORDER_PROCESSED_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Order processed' AND accessible == 1"
            );

    // The alert's OK, matched by name alone and NOT by element type.
    // Confirmed live: on a single-item reject it renders as an
    // XCUIElementTypeButton, but on an all-rejected multi-item order the
    // same OK comes through as an XCUIElementTypeOther, so pinning the type
    // (as this once did with 'XCUIElementTypeButton') times out on the
    // multi-item case. The alert is modal, so "OK" is unambiguous on screen
    // without the type.
    public static final By ORDER_PROCESSED_OK_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == 'OK' AND accessible == 1"
            );

    // ---------- "Got it" bottom sheet (opened by GOT_IT_BUTTON) ----------

    public static final By GOT_IT_SHEET_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Got it' AND accessible == 1"
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

    /**
     * The pencil icon beside the "Per Unit Price" on the "Got it" sheet —
     * tapping it opens the price field for editing (the keyboard comes up
     * over the sheet). Confirmed live from a page source dump: it is a
     * nameless, label-less 15x15 {@code XCUIElementTypeOther} sitting just
     * to the right of the "$6,100.00" value, so there is no name to match
     * on. It is anchored instead on the stable "Per Unit Price" label — the
     * first {@code XCUIElementTypeOther} that follows it in document order —
     * which is why this is an XPath rather than the predicate strings used
     * elsewhere in this file.
     */
    public static final By EDIT_PER_UNIT_PRICE_PENCIL =
            By.xpath("//XCUIElementTypeStaticText[@name='Per Unit Price']"
                    + "/following::XCUIElementTypeOther[1]");

    /**
     * The editable Per Unit Price field — the pencil above swaps the
     * static "$6,100.00" value for this {@code XCUIElementTypeTextField}
     * (its {@code value} reads e.g. "6100.", with no currency symbol or
     * thousands separators). Confirmed live from a page source dump to be
     * the only text field on the sheet, which is how it is matched: it
     * carries no name, label or placeholder.
     */
    public static final By EDIT_PER_UNIT_PRICE_FIELD =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField'"
            );

    // ---------- "Accept this item?" sheet ----------

    public static final By ACCEPT_ITEM_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Accept this item?' AND accessible == 1"
            );

    public static final By ACCEPT_ITEM_PROCEED_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Proceed' AND accessible == 1"
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
     * The confirm-payment button, whatever amount it carries. Matched on
     * the fixed part of its name only: the live total follows it
     * ("Confirm payment $1,345.64") and differs on every order, so a
     * locator built around the amount breaks on the next run. Read the
     * total off this button instead — see
     * OrderDetailPage.getConfirmPaymentAmount().
     */
    public static final By ANY_CONFIRM_PAYMENT_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Confirm payment $' AND accessible == 1"
            );

    // ---------- "Capture the receipt" screen (the app's own screen) ----------
    //
    // Confirmed against a page source dump taken from the live screen.
    // Opening this screen's system document scanner
    // (VNDocumentCameraViewController, a system component, not app UI,
    // confirmed separately against its own page source dump) fails
    // immediately on this Simulator with a concrete, unambiguous error:
    // "Scan failed — The operation couldn't be completed.
    // (AVFoundationErrorDomain error -11800.)" — Apple's own "no camera
    // device" error, reproduced twice (once automatically on opening the
    // system camera view, once from an explicit manual shutter tap).
    // REQUIRES A REAL DEVICE: no software workaround exists for this on a
    // Simulator with no camera hardware.

    public static final By RECEIPT_SCREEN_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Capture the receipt' AND accessible == 1"
            );

    // ---------- Receipt capture on the Simulator — confirmed live, step by step ----------
    //
    // With no camera the screen reads "Camera is not available on this
    // device." and "Take photo" does nothing, but the staging build
    // carries a bypass: "DEV · Sample receipt" swaps in a sample image
    // and moves straight on to the "Looks good?" review screen, which
    // offers RETAKE / USE PHOTO and a second bypass, "DEV · Upload
    // receipt (skip confirm)".

    public static final By DEV_SAMPLE_RECEIPT =
            AppiumBy.iOSNsPredicateString(
                    "name == 'DEV · Sample receipt' AND accessible == 1"
            );

    public static final By LOOKS_GOOD_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Looks good?' AND accessible == 1"
            );

    public static final By USE_PHOTO_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'USE PHOTO' AND accessible == 1"
            );

    // USE PHOTO opens "Submit receipt? / Upload this receipt to complete
    // and process the order." with Proceed and Cancel — confirmed live.
    public static final By SUBMIT_RECEIPT_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Submit receipt?' AND accessible == 1"
            );

    public static final By SUBMIT_RECEIPT_PROCEED =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name == 'Proceed' AND accessible == 1"
            );

    // Proceed ends on "Order processed / Receipt uploaded and order
    // submitted." with OK — the same title ORDER_PROCESSED_TITLE matches,
    // but here the OK is an Other, not the Button that alert carries.
    public static final By RECEIPT_PROCESSED_OK =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name == 'OK' AND accessible == 1"
            );

    // ---------- "Start handoff?" sheet (opened by ShopperHomePage.tapStartHandoff()) ----------

    public static final By START_HANDOFF_SHEET_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Start handoff?' AND accessible == 1"
            );

    public static final By START_HANDOFF_SHEET_PROCEED =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Proceed' AND accessible == 1"
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

    /**
     * The DEV bypass for that missing camera — the same shape
     * {@link #DEV_SAMPLE_RECEIPT} gives the receipt capture, confirmed
     * live: it swaps in a sample image, and the run goes on through
     * "Looks good?" -&gt; {@link #USE_PHOTO_BUTTON} -&gt; "Complete
     * handoff?" -&gt; Proceed -&gt; "Handoff complete" -&gt; OK.
     */
    public static final By DEV_SAMPLE_HANDOFF_PHOTO =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS 'Sample handoff photo' AND accessible == 1"
            );

    public static final By COMPLETE_HANDOFF_PROCEED =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name == 'Proceed' AND accessible == 1"
            );

    public static final By HANDOFF_COMPLETE_OK =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND name == 'OK' AND accessible == 1"
            );

}
