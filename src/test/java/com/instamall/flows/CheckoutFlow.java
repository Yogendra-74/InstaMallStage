package com.instamall.flows;

import com.instamall.components.delivery.DeliveryUnavailablePrompt;
import com.instamall.components.referral.ReferralPrompt;
import com.instamall.pages.bag.ShoppingBagPage;
import com.instamall.pages.checkout.CheckoutPage;
import com.instamall.pages.checkout.CheckoutPageValidator;
import com.instamall.pages.orders.OrderConfirmationPage;
import com.instamall.pages.orders.OrderConfirmationPageValidator;
import com.instamall.pages.payment.PaymentSheet;
import com.instamall.pages.payment.PaymentSheetValidator;
import com.instamall.utils.CurrencyUtils;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The checkout half of a purchase, from tapping Checkout in the bag to a
 * verified order confirmation.
 *
 * Shared rather than copied. Every awkward thing this handles was learnt
 * from a failing run — that stock is only settled when Checkout is tapped,
 * that a refused delivery has to become a pickup, that the pickup total is
 * not the delivery total, that a collection order tracks differently on the
 * confirmation screen. A second copy of it would start drifting from the
 * first the moment any of that changed again.
 *
 * Steps are reported through the caller's own logger, so a journey's log
 * reads as one sequence rather than two.
 */
public final class CheckoutFlow {

    /** How long the checkout screen gets before the bag is read as refusing. */
    private static final Duration CHECKOUT_OPENS_WITHIN = Duration.ofSeconds(20);

    /** How long the payment sheet gets before the flow tries pickup. */
    private static final Duration PAYMENT_SHEET_APPEARS_WITHIN = Duration.ofSeconds(25);

    /** How many times to clear unsellable lines and ask again. */
    private static final int CHECKOUT_RETRIES = 3;

    private final ShoppingBagPage bagPage = new ShoppingBagPage();
    private final CheckoutPage checkoutPage = new CheckoutPage();
    private final CheckoutPageValidator checkoutValidator = new CheckoutPageValidator();
    private final PaymentSheet paymentSheet = new PaymentSheet();
    private final PaymentSheetValidator paymentValidator = new PaymentSheetValidator();
    private final OrderConfirmationPage orderPage = new OrderConfirmationPage();
    private final OrderConfirmationPageValidator orderValidator = new OrderConfirmationPageValidator();

    private final Consumer<String> step;

    public CheckoutFlow(Consumer<String> step) {

        this.step = step;
    }

    /**
     * What placing the order turned out to do, and what it will cost.
     */
    public static final class Placed {

        private final boolean pickup;
        private final BigDecimal total;

        private Placed(boolean pickup, BigDecimal total) {

            this.pickup = pickup;
            this.total = total;
        }

        public boolean isPickup() {

            return pickup;
        }

        public BigDecimal total() {

            return total;
        }
    }

    /**
     * One line item an order confirmation is expected to echo back —
     * product, size and colour, each read the same way a single-item
     * journey already reads them off the product page.
     */
    public static final class ItemExpectation {

        private final String product;
        private final String size;
        private final String color;

        public ItemExpectation(String product, String size, String color) {

            this.product = product;
            this.size = size;
            this.color = color;
        }

        public String product() {

            return product;
        }

        public String size() {

            return size;
        }

        public String color() {

            return color;
        }
    }

    /**
     * Takes the bag through to the checkout screen.
     *
     * Stock is settled here, not in the bag: tapping Checkout is what makes
     * the app look, and a line it can no longer sell is marked only once
     * that attempt has been refused. So there is nothing to find
     * beforehand, however long the bag is watched — what the screen does
     * instead is stay where it is, with the button still reporting itself
     * enabled. The two outcomes are taken as a race, whatever the refusal
     * named is deleted, and the question is asked again.
     *
     * @param product the item this run means to buy, which must survive
     * @param size    its size, or the fixed size of a one-size product
     * @return the unsellable lines deleted along the way
     */
    public List<String> openCheckoutFromBag(String product, String size) {

        List<String> cleared = new ArrayList<>();

        BagOutcome outcome = tryOpenCheckoutFromBag(product, size, cleared);

        Assert.assertSame(outcome, BagOutcome.CHECKOUT_OPENED,
                "\"" + product + "\" (" + size + ") is what the store could not sell,"
                        + " so this run has nothing left to order");

        return cleared;
    }

    /**
     * What came of trying to leave the bag.
     */
    public enum BagOutcome {

        /** The checkout screen opened. */
        CHECKOUT_OPENED,

        /** The line this run meant to buy is the one the store cannot sell. */
        OURS_UNSELLABLE
    }

    /**
     * The same attempt, reporting rather than deciding.
     *
     * A journey that can go back and choose a different product wants to do
     * that when its own item turns out to be unsellable; one that cannot
     * wants to fail. So the outcome is handed back instead of asserted on
     * here — see openCheckoutFromBag for the failing form.
     *
     * @param clearedInto collects the unsellable lines deleted along the way
     */
    public BagOutcome tryOpenCheckoutFromBag(String product, String size,
                                             List<String> clearedInto) {

        step.accept("Checkout");
        // Each store group checks out on its own now, so it has to be the
        // button under the group holding what this run means to buy.
        bagPage.clickCheckoutFor(product, size);

        for (int attempt = 1; awaitCheckoutOrStockRefusal() == CheckoutOutcome.STOCK_REFUSED; attempt++) {

            Assert.assertTrue(attempt <= CHECKOUT_RETRIES,
                    "The bag still refuses to check out after " + CHECKOUT_RETRIES
                            + " attempts at clearing what it cannot sell");

            step.accept("Checkout refused — unsellable: " + bagPage.listOutOfStockItems());

            List<String> justCleared = bagPage.removeOutOfStockItems();
            clearedInto.addAll(justCleared);

            step.accept("Deleted " + justCleared.size() + " unsellable line(s) from the bag");

            // Our own variant can be the one that went — stock can run out
            // between choosing a size and reaching the bag.
            if (!bagPage.hasItem(product, size)) {

                return BagOutcome.OURS_UNSELLABLE;
            }

            step.accept("Checkout again");
            bagPage.clickCheckoutFor(product, size);
        }

        checkoutPage.waitForScreen();

        return BagOutcome.CHECKOUT_OPENED;
    }

    /**
     * Chooses priority delivery, checks the summary adds up, places the
     * order, and falls back to collection when the delivery is refused.
     */
    public Placed placeOrder(String addressName) {

        // Safe for a caller that reached checkout by its own route rather
        // than through tryOpenCheckoutFromBag.
        checkoutPage.waitForScreen();

        // Which mode the screen opened in decides everything below. A store
        // group already set for collection opens straight on "Pickup from",
        // with no delivery section, no Priority option and no "Switch to
        // pickup" to look for — so it goes directly to choosing a slot.
        // Reading the mode first is what avoids hunting for controls that
        // are not there and then concluding the screen is broken.
        if (checkoutValidator.isInPickupMode()) {

            step.accept("Checkout opened for collection — choosing a slot directly");

            return placeAsPickup();
        }

        step.accept("Checkout opened for delivery");
        checkoutPage.waitForDeliveryForm();

        Assert.assertTrue(checkoutValidator.isDeliverToSectionDisplayed(),
                "Checkout is not showing the delivery section");

        Assert.assertTrue(checkoutValidator.areDeliveryOptionsDisplayed(),
                "Checkout is not offering both Priority and Schedule");

        Assert.assertTrue(checkoutValidator.isAddressDisplayed(addressName),
                "Checkout is not delivering to \"" + addressName + "\"");

        step.accept("Choose priority delivery");
        checkoutPage.selectPriorityDelivery();

        // The screen opens on Schedule, and the bottom button carries the
        // chosen speed — so this is what confirms the switch took.
        Assert.assertTrue(checkoutPage.getPlaceOrderLabel().startsWith("Get it"),
                "Place-order button still reads \"" + checkoutPage.getPlaceOrderLabel()
                        + "\" after choosing Priority");

        // The tip is left at the app's default. Its chips are below the
        // fold and unreachable without scrolling, and the default already
        // puts a Tip line in the summary — which is what the sum needs.
        Assert.assertTrue(checkoutValidator.areAllChargeRowsPresent(),
                "Checkout summary is missing one of Subtotal / Delivery / Estimated tax"
                        + " / Service fee / Tip / Total");

        BigDecimal total = checkoutPage.getTotal();

        assertSameAmount(checkoutPage.sumOfCharges(), total,
                "Individual charges do not add up to the checkout total");

        step.accept("Place order (total " + total + ")");
        checkoutPage.placeOrder();

        boolean pickup = awaitOrderOutcome() == OrderOutcome.DELIVERY_REFUSED;

        if (pickup) {

            DeliveryUnavailablePrompt.waitUntilGone();

            step.accept("Order did not go through as a delivery — switching to pickup");
            checkoutPage.switchToPickup();

            return placeAsPickup();
        }

        return new Placed(false, total);
    }

    /**
     * Chooses a collection slot and places the order.
     *
     * Reached two ways: a store group that was already set for collection,
     * where the screen opened this way and there was never anything to
     * switch, and a delivery that the app refused. Both end up here, and
     * both need the total read fresh — a pickup carries no delivery fee, so
     * a delivery quote taken beforehand no longer stands and the payment
     * sheet would be checked against a figure the app has moved on from.
     */
    private Placed placeAsPickup() {

        step.accept("Pickup slot: " + checkoutPage.selectFirstTimeSlot());

        // Nothing is being delivered, so there is no fee for it — which is
        // also what confirms a switch actually took.
        Assert.assertFalse(checkoutValidator.isChargeRowPresent("Delivery"),
                "A collection order should carry no Delivery charge");

        // No Tip here on purpose: the header above that chip row reads "Tip
        // to Your Delivery Driver" (see CheckoutPageLocators.TIP_HEADER),
        // and a pickup has no delivery driver — the app drops the whole tip
        // section rather than leaving it for a courier that never comes.
        // getCharges() already reads whichever rows are actually present,
        // so summing and totalling still work with this one gone.
        for (String row : new String[] {"Subtotal", "Estimated tax", "Service fee", "Total"}) {

            Assert.assertTrue(checkoutValidator.isChargeRowPresent(row),
                    "Pickup summary is missing its " + row + " row");
        }

        BigDecimal total = checkoutPage.getTotal();

        assertSameAmount(checkoutPage.sumOfCharges(), total,
                "Individual charges do not add up to the pickup total");

        step.accept("Place order as a pickup (total " + total + ")");
        checkoutPage.placeOrder();

        return new Placed(true, total);
    }

    /**
     * Pays with the staging card and verifies the order that comes back.
     *
     * @param color the colour the order should echo back, or null to leave
     *              the variant line unchecked — a journey that never read a
     *              colour has nothing to assert it against
     * @return the order id
     */
    public String payAndVerifyOrder(Placed placed,
                                    String cardLast4,
                                    String brand,
                                    String product,
                                    String size,
                                    String color) {

        return payAndVerifyOrder(placed, cardLast4, brand,
                List.of(new ItemExpectation(product, size, color)));
    }

    /**
     * Same as the single-item form, but for a bag that checked out several
     * variants from the same store as one order — each is asserted on the
     * confirmation screen in turn rather than just the one.
     */
    public String payAndVerifyOrder(Placed placed,
                                    String cardLast4,
                                    String brand,
                                    List<ItemExpectation> items) {

        paymentSheet.waitForScreen();

        Assert.assertTrue(paymentValidator.isTestModeBadgeDisplayed(),
                "Payment sheet is not in TEST mode — refusing to pay with a live card");

        Assert.assertTrue(paymentValidator.isSavedCardDisplayed(cardLast4),
                "Saved card ending " + cardLast4 + " is not offered");

        BigDecimal payable = paymentSheet.getPayableAmount();

        assertSameAmount(payable, placed.total(),
                "Payment sheet is charging a different amount than checkout quoted");

        step.accept("Pay " + payable + " with card ending " + cardLast4);
        paymentSheet.payWithSavedCard(cardLast4);

        // The app celebrates a placed order with a referral sheet, and while
        // that is up the confirmation behind it reports visible == false.
        if (ReferralPrompt.dismissIfShowing()) {

            step.accept("Dismissed the referral sheet");
        }

        orderPage.waitForScreen();

        String orderId = orderPage.getOrderId();
        step.accept("Order placed: " + orderId);

        Assert.assertTrue(orderId.matches("#?IM-[A-Z0-9]+"),
                "Order id \"" + orderId + "\" does not look like IM-XXXXX");

        // A fresh order opens on "Order received" with a status line under
        // it. The stage tracker (PLACED / SHOPPING / …) the old screen
        // carried is not on the redesigned one, so it is no longer checked.
        if (placed.isPickup()) {

            Assert.assertTrue(orderValidator.isStatusOrderPlaced(),
                    "A freshly placed pickup order should read Order received");

        } else {

            Assert.assertTrue(orderValidator.isStatusInProgress(),
                    "A freshly placed order should read Order received");
        }

        Assert.assertTrue(orderValidator.isStatusMessageDisplayed(),
                "Order screen is not showing its status line (\"Your order is …\")");

        Assert.assertTrue(orderValidator.isStoreDisplayed(brand),
                "Order does not name the store " + brand);

        for (ItemExpectation item : items) {

            Assert.assertTrue(orderValidator.isOrderedItemDisplayed(item.product()),
                    "Order does not list \"" + item.product() + "\"");

            if (item.color() != null) {

                Assert.assertTrue(
                        orderValidator.isOrderedVariantDisplayed(item.size(), item.color(), 1),
                        "Order does not echo back Size: " + item.size() + " | Color: "
                                + item.color() + " | Qty: 1 for \"" + item.product() + "\"");
            }
        }

        assertSameAmount(orderPage.getTotal(), payable,
                "Order total does not match the amount paid");

        if (placed.isPickup()) {

            Assert.assertTrue(orderValidator.isPickupDetailsDisplayed(),
                    "Order is not showing where it is to be collected from");

        } else {

            Assert.assertTrue(orderValidator.isDeliveryDetailsDisplayed(),
                    "Order is not showing the delivery details");
        }

        return orderId;
    }

    // ---------- outcomes that have to be raced ----------

    private enum CheckoutOutcome { OPENED, STOCK_REFUSED }

    private enum OrderOutcome { PAYMENT_SHEET, DELIVERY_REFUSED }

    /**
     * Watches for the checkout screen and the bag's refusal at once. A
     * refusal has no error of its own, so what gets watched is the
     * out-of-stock line the refusal writes onto the offending row.
     *
     * Reaching the deadline with neither is read as refused, the screen
     * plainly not having opened — and on a bag that is in fact clean the
     * removal that follows finds nothing and costs one more tap.
     */
    private CheckoutOutcome awaitCheckoutOrStockRefusal() {

        try {

            return WaitUtils.waitUntil(driver ->
                            checkoutValidator.isDisplayedNow()
                                    ? CheckoutOutcome.OPENED
                                    : bagPage.hasOutOfStockItems()
                                            ? CheckoutOutcome.STOCK_REFUSED
                                            : null,
                    CHECKOUT_OPENS_WITHIN);

        } catch (TimeoutException neitherAppeared) {

            return CheckoutOutcome.STOCK_REFUSED;
        }
    }

    /**
     * Watches for the payment sheet and the delivery-refused toast at once,
     * so a refusal is not learnt by sitting out the sheet's whole deadline
     * — by which time the toast has cleared itself unseen.
     */
    private OrderOutcome awaitOrderOutcome() {

        try {

            return WaitUtils.waitUntil(driver ->
                            paymentValidator.isDisplayedNow()
                                    ? OrderOutcome.PAYMENT_SHEET
                                    : DeliveryUnavailablePrompt.isShowingNow()
                                            ? OrderOutcome.DELIVERY_REFUSED
                                            : null,
                    PAYMENT_SHEET_APPEARS_WITHIN);

        } catch (TimeoutException neitherAppeared) {

            return OrderOutcome.DELIVERY_REFUSED;
        }
    }

    /**
     * BigDecimal.equals() is scale sensitive and this app mixes scales —
     * the product page prints "$750" where the bag prints "$750.00".
     */
    private void assertSameAmount(BigDecimal actual, BigDecimal expected, String message) {

        Assert.assertTrue(CurrencyUtils.equalAmounts(actual, expected),
                message + " — expected " + expected + " but found " + actual);
    }
}