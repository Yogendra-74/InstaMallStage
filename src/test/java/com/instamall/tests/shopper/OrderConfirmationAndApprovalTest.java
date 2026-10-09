package com.instamall.tests.shopper;

import com.instamall.base.ShopperBaseTest;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.config.ConfigReader;
import com.instamall.pages.shopper.home.ShopperHomePage;
import com.instamall.pages.shopper.login.ShopperLoginPage;
import com.instamall.pages.shopper.orders.OrderDetailPage;
import com.instamall.pages.shopper.orders.OrderDetailPageValidator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Order confirmation and approval journey on the Shopper app, on staging:
 *
 *   log in -> home ("view order") -> confirm each item found (Got it ->
 *   Accept this item? -> Proceed) -> validate and proceed -> confirm
 *   payment -> capture the receipt
 *
 * The positive flow, as specified:
 *   1. Install (if needed) and launch the app.
 *   2. Log in — lands on Home.
 *   3. Open the order from its card.
 *   4. GOT IT, to confirm the item was found.
 *   5. GOT IT again on the confirmation sheet, then Proceed on
 *      "Accept this item?".
 *   6. Validate and proceed, to place the order.
 *   7. Confirm payment.
 *   8-10. Capture and submit the receipt photo, then proceed and return
 *      home.
 *   11-13. Once a driver is assigned, Start Handoff -> Proceed -> capture
 *      and submit a handoff photo -> proceed and return home.
 *
 * STOPS AFTER STEP 7. Steps 8 onward depend on
 * VNDocumentCameraViewController, Apple's own document-scanner component
 * — confirmed against a page source dump to be system UI, not this app's
 * own. It has no non-camera fallback, and the iOS Simulator has no camera
 * hardware for it to capture from (this machine also has no Simulator.app
 * GUI installed, so even the usual "route the Mac's webcam through as a
 * virtual camera" workaround is unavailable here). Steps 8-13 — Keep,
 * the tick to proceed, Use photo, the final Proceed/OK, and the
 * Start Handoff leg that follows once a driver is assigned — need a real
 * device to map and automate.
 *
 * NOTE: this pays with a staging virtual card, so every run against a
 * fresh order is a real staging transaction.
 */
public class OrderConfirmationAndApprovalTest extends ShopperBaseTest {

    private final ShopperLoginPage loginPage = new ShopperLoginPage();
    private final ShopperHomePage homePage = new ShopperHomePage();
    private final OrderDetailPage orderPage = new OrderDetailPage();
    private final OrderDetailPageValidator orderValidator = new OrderDetailPageValidator();

    /**
     * Tells a build of the app that checks for it to hand back a bundled
     * sample image instead of opening the real document scanner — the
     * Simulator has no camera for VNDocumentCameraViewController to
     * capture from (confirmed: it fails immediately with
     * AVFoundationErrorDomain -11800, "no camera device"). A build that
     * does not check for this argument ignores it, so this is harmless
     * ahead of that support landing.
     */
    @Override
    protected List<String> processArguments() {

        return List.of("-mockDocumentScan");
    }

    @Test(description = "Confirm and approve a new order through to Confirm payment")
    public void confirmAndApproveOrderThroughPayment() {

        step("Log in as " + ConfigReader.get("shopperEmail"));
        loginPage.waitForScreen()
                 .enterEmail(ConfigReader.get("shopperEmail"))
                 .enterPassword(ConfigReader.get("shopperPassword"))
                 .clickLogIn();

        // clickLogIn() already grants whatever permission prompts the
        // login itself raises (notifications, location, App Tracking
        // Transparency) — see ShopperLoginPage's own doc comment.

        homePage.waitForScreen();
        step("On Home");

        // ---------- 3. Open the order ----------

        step("Open the new order's card");
        homePage.openNewOrderCard();

        orderPage.waitForScreen();
        String orderId = orderPage.getOrderId();
        step("Order detail open: " + orderId);

        Assert.assertTrue(orderValidator.isDisplayed(),
                "Order detail screen did not open from the Home card");

        // ---------- 4-5. Confirm the item was found, then accept it ----------

        // Re-running against an order carried over from a previous pass
        // can land here already past this point (item already
        // "Approved") — only drive Got it / Accept this item? when the
        // order is still waiting on it.
        if (orderValidator.isGotItButtonDisplayedNow()) {

            step("GOT IT");
            orderPage.clickGotIt();

            orderPage.waitForGotItSheet();
            Assert.assertTrue(orderValidator.isGotItSheetDisplayed(),
                    "The \"Got it\" item-details sheet did not open");

            step("Confirm the \"Got it\" sheet");
            orderPage.confirmGotItSheet();

            orderPage.waitForAcceptItemSheet();
            Assert.assertTrue(orderValidator.isAcceptItemSheetDisplayed(),
                    "The \"Accept this item?\" sheet did not open");

            step("Proceed on \"Accept this item?\"");
            orderPage.proceedAcceptingItem();

            Assert.assertTrue(orderValidator.isItemApproved(),
                    "Item does not show \"Approved\" after accepting it");

            step("Item approved");

        } else {

            step("Item already approved from a previous pass — skipping Got it / Accept this item?");
        }

        // ---------- 6. Validate and proceed ----------

        Assert.assertTrue(orderValidator.isValidateAndProceedDisplayed(),
                "\"Validate and proceed\" is not displayed once the item is approved");

        step("Validate and proceed");
        orderPage.validateAndProceed();

        // ---------- 7. Confirm payment ----------

        orderPage.waitForConfirmPaymentSheet();
        Assert.assertTrue(orderValidator.isConfirmPaymentSheetDisplayed(),
                "The \"Confirm payment\" sheet did not open");

        Assert.assertTrue(orderValidator.isVirtualCardDisplayed(),
                "The virtual card is not shown on the \"Confirm payment\" sheet");

        String amount = orderPage.getConfirmPaymentAmount();
        step("Confirm payment $" + amount);
        orderPage.confirmPayment(amount);

        // ---------- 8. Capture the receipt — reachability check only ----------

        // Confirms the flow reaches the receipt-capture screen; does not
        // attempt to drive it further. See this class's own doc comment
        // and OrderDetailPageLocators' for why.
        orderPage.waitForReceiptCaptureScreen();
        Assert.assertTrue(orderValidator.isReceiptCaptureScreenDisplayed(),
                "\"Capture the receipt\" did not open after confirming payment");

        // The camera prompt fires once this screen appears — granted here
        // rather than denied, though granting it does not itself work
        // around the Simulator's lack of camera hardware below.
        if (SystemAlerts.allowCamera()) {

            step("Allowed camera access");
        }

        step("Reached \"Capture the receipt\" — stopping here; needs a real device"
                + " to go further (see class doc comment)");
    }

    private void step(String description) {

        System.out.println(">>> STEP: " + description);
    }

}
