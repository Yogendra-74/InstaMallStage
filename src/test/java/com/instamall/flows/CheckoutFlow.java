package com.instamall.flows;

import com.instamall.config.ConfigReader;
import com.instamall.components.referral.ReferralPrompt;
import com.instamall.pages.bag.ShoppingBagPage;
import com.instamall.pages.checkout.CheckoutPage;
import com.instamall.pages.orders.OrderConfirmationPage;
import com.instamall.pages.orders.OrderConfirmationPageValidator;
import com.instamall.pages.payment.PaymentSheet;
import com.instamall.pages.payment.PaymentSheetValidator;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;

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

    /**
     * How long, after paying, to watch for either the referral sheet or the
     * order confirmation. Order creation is the slowest hop, so this matches
     * the confirmation screen's own creation window.
     */
    private static final Duration REFERRAL_OR_CONFIRMATION_WITHIN = Duration.ofSeconds(90);

    /** How many times to clear unsellable lines and ask again. */
    private static final int CHECKOUT_RETRIES = 3;

    /**
     * How many times each of the checkout buttons — "Review delivery &
     * checkout", "Get it Today" and "Pay" — is clicked before the order is
     * taken as not placed and the test failed. The one retry covers a tap
     * the app swallowed while a screen was still animating; a second miss
     * fails, per instruction, with no fallback to pickup or anything else.
     */
    private static final int MAX_CLICK_ATTEMPTS = 2;

    private final ShoppingBagPage bagPage = new ShoppingBagPage();
    private final CheckoutPage checkoutPage = new CheckoutPage();
    private final PaymentSheet paymentSheet = new PaymentSheet();
    private final PaymentSheetValidator paymentValidator = new PaymentSheetValidator();
    private final OrderConfirmationPage orderPage = new OrderConfirmationPage();
    private final OrderConfirmationPageValidator orderValidator = new OrderConfirmationPageValidator();

    private final Consumer<String> step;

    public CheckoutFlow(Consumer<String> step) {

        this.step = step;
    }

    /**
     * What placing the order turned out to do.
     */
    public static final class Placed {

        private final boolean pickup;

        private Placed(boolean pickup) {

            this.pickup = pickup;
        }

        public boolean isPickup() {

            return pickup;
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
     * Opens the checkout screen by tapping the bag's "Review delivery &
     * checkout" button.
     *
     * Per instruction the button is tapped at most {@link #MAX_CLICK_ATTEMPTS}
     * times: the one retry covers a tap the app swallowed while the screen
     * was still animating, and if the checkout screen has still not opened
     * after the second tap the test fails rather than trying anything else.
     * Unsellable lines are no longer cleared and re-asked — see
     * {@link #tryOpenCheckoutFromBag} for the stock-clearing form, which the
     * product-hunting journeys still use.
     *
     * @param product the item this run means to buy
     * @param size    its size, or the fixed size of a one-size product
     * @return an empty list — nothing is cleared here any more, kept only so
     *         existing call sites need no change
     */
    public List<String> openCheckoutFromBag(String product, String size) {

        clickToProceed("Review delivery & checkout",
                () -> bagPage.clickCheckoutFor(product, size),
                () -> checkoutPage.read().isOpen(),
                CHECKOUT_OPENS_WITHIN,
                "the checkout screen did not open");

        checkoutPage.awaitScreen();

        return new ArrayList<>();
    }

    /**
     * Opens the checkout screen by tapping a store group's already-expanded
     * "Review delivery & checkout" button, with the same capped retry as
     * {@link #openCheckoutFromBag}.
     *
     * For the multi-group journeys that expand the group and scroll the
     * button on screen themselves, then hand the tap off here so it gets the
     * same "tap up to {@link #MAX_CLICK_ATTEMPTS} times, else fail" handling.
     */
    public void reviewDeliveryAndCheckout() {

        clickToProceed("Review delivery & checkout",
                () -> bagPage.clickReviewDeliveryAndCheckout(),
                () -> checkoutPage.read().isOpen(),
                CHECKOUT_OPENS_WITHIN,
                "the checkout screen did not open");

        checkoutPage.awaitScreen();
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

        checkoutPage.awaitScreen();

        return BagOutcome.CHECKOUT_OPENED;
    }

    /**
     * Chooses priority delivery, checks the summary adds up, and places the
     * order by tapping "Get it Today".
     *
     * Per instruction the button is tapped at most {@link #MAX_CLICK_ATTEMPTS}
     * times and the test fails if the payment sheet has not opened after the
     * last tap — a refused delivery is no longer switched to pickup. (A store
     * group that opens already in collection mode still places as a pickup,
     * which is a different screen, not a fallback.)
     *
     * @param addressName the saved address this checkout should be
     *                    delivering to, e.g. "Work" — or null for a guest
     *                    checkout, which has no saved, named address to
     *                    check against (the simulated GPS fix resolves to
     *                    whatever street address is nearby instead); null
     *                    confirms a real address populated the row rather
     *                    than checking which one.
     */
    public Placed placeOrder(String addressName) {

        // One snapshot answers everything asked of the screen before the
        // Priority tap: the mode it opened in, whether the delivery form
        // is complete, and which address it is delivering to. Asked a
        // locator at a time this was some forty-five round trips — see
        // CheckoutPage.Screen. Safe for a caller that reached checkout by
        // its own route rather than through tryOpenCheckoutFromBag.
        CheckoutPage.Screen screen = checkoutPage.awaitScreen();

        // Which mode the screen opened in decides everything below. A store
        // group already set for collection opens straight on "Pickup from",
        // with no delivery section, no Priority option and no "Switch to
        // pickup" to look for — so it goes directly to choosing a slot.
        if (screen.isPickupMode()) {

            step.accept("Checkout opened for collection — choosing a slot directly");

            return placeAsPickup();
        }

        step.accept("Checkout opened for delivery");

        Assert.assertTrue(screen.isDeliveryFormComplete(),
                "Checkout is not showing the delivery section with both Priority and Schedule");

        if (addressName != null) {

            Assert.assertTrue(screen.isDeliveringTo(addressName),
                    "Checkout is not delivering to \"" + addressName + "\" — address row reads \""
                            + screen.addressRow() + "\"");

        } else {

            Assert.assertNotNull(screen.addressRow(),
                    "Checkout is not showing any delivery address");
        }

        step.accept("Choose priority delivery");
        checkoutPage.selectPriorityDelivery();

        // The second snapshot, once the quote has refreshed: the bottom
        // button's copy, which confirms the switch took, and the summary.
        screen = checkoutPage.awaitSummary();

        Assert.assertTrue(screen.placeOrderLabel() != null
                        && screen.placeOrderLabel().startsWith("Get it"),
                "Place-order button still reads \"" + screen.placeOrderLabel()
                        + "\" after choosing Priority");

        // Tap "Get it Today" up to MAX_CLICK_ATTEMPTS times, failing if the
        // payment sheet has not come up after the last attempt. Per
        // instruction a refused delivery is not switched to pickup — a
        // missed placement just fails.
        clickToProceed("Get it Today",
                () -> checkoutPage.placeOrder(),
                () -> paymentValidator.isDisplayedNow(),
                PAYMENT_SHEET_APPEARS_WITHIN,
                "the payment sheet did not open");

        return new Placed(false);
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

        // Lets the pickup summary settle before the order is placed — a
        // pickup carries no delivery fee, so the total the app shows here
        // is not the delivery quote taken earlier.
        checkoutPage.awaitSummary();

        step.accept("Place order as a pickup");
        checkoutPage.placeOrder();

        return new Placed(true);
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

        // No assertion that a card is already saved: the sheet handles
        // both states, paying with the saved card when one is there and
        // typing the test card in when it is not.
        //
        // Tap "Pay" up to MAX_CLICK_ATTEMPTS times. The card is entered on
        // the first attempt (payWith); a second attempt, if the first did
        // not take, only re-taps Pay, since the card is already in. The
        // "order is coming up" signal is the referral sheet or the order
        // confirmation — order creation is the slowest hop, so each attempt
        // is given the full creation window before it counts as a miss,
        // which also keeps a slow-but-successful charge from being paid a
        // second time. If neither shows after the second tap, the order was
        // not placed and the test fails.
        step.accept("Pay");
        paymentSheet.payWith(ConfigReader.get("testCardNumber"),
                ConfigReader.get("testCardExpiry"),
                ConfigReader.get("testCardCvc"));

        if (!awaitCondition(this::orderIsComingUp, REFERRAL_OR_CONFIRMATION_WITHIN)) {

            step.accept("Pay (attempt 2 of " + MAX_CLICK_ATTEMPTS + ")");
            paymentSheet.pay();

            Assert.assertTrue(
                    awaitCondition(this::orderIsComingUp, REFERRAL_OR_CONFIRMATION_WITHIN),
                    "\"Pay\" was tapped " + MAX_CLICK_ATTEMPTS + " times but the order"
                            + " confirmation never came up — the order was not placed");
        }

        // While the referral sheet is up the confirmation behind it reports
        // visible == false, so clear it before reading the order. It does
        // not always appear — isShowingNow() returns at once when it is
        // absent, rather than sitting out a timeout.
        if (ReferralPrompt.isShowingNow() && ReferralPrompt.dismissIfShowing()) {

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

    /**
     * Whether the order is on its way up after "Pay" — the referral sheet
     * the app throws over a fresh order, or the order confirmation itself.
     */
    private boolean orderIsComingUp() {

        return ReferralPrompt.isShowingNow() || orderValidator.isDisplayedNow();
    }

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
                            checkoutPage.read().isOpen()
                                    ? CheckoutOutcome.OPENED
                                    : bagPage.hasOutOfStockItems()
                                            ? CheckoutOutcome.STOCK_REFUSED
                                            : null,
                    CHECKOUT_OPENS_WITHIN);

        } catch (TimeoutException neitherAppeared) {

            return CheckoutOutcome.STOCK_REFUSED;
        }
    }

    // ---------- clicking a checkout button, with a capped retry ----------

    /**
     * Clicks a checkout button up to {@link #MAX_CLICK_ATTEMPTS} times,
     * stopping as soon as the step it drives has visibly taken, and failing
     * the test when it has not after the last attempt. The one retry covers
     * a tap the app swallowed while a screen was still animating; a second
     * miss is taken as the order not going through, per instruction — there
     * is no fallback.
     *
     * @param buttonName what to call the button in the log and the failure
     * @param click      taps the button
     * @param proceeded  true once the step the button drives has taken
     * @param within     how long each attempt waits for {@code proceeded}
     * @param failure    what did not happen, for the failure message
     */
    private void clickToProceed(String buttonName, Runnable click,
                                java.util.function.BooleanSupplier proceeded,
                                Duration within, String failure) {

        for (int attempt = 1; attempt <= MAX_CLICK_ATTEMPTS; attempt++) {

            step.accept(attempt == 1
                    ? buttonName
                    : buttonName + " (attempt " + attempt + " of " + MAX_CLICK_ATTEMPTS + ")");

            click.run();

            if (awaitCondition(proceeded, within)) {

                return;
            }
        }

        Assert.fail("\"" + buttonName + "\" was tapped " + MAX_CLICK_ATTEMPTS
                + " times but " + failure + " — the order was not placed");
    }

    /**
     * Waits up to {@code within} for a condition to hold, returning whether
     * it did rather than throwing when it does not.
     */
    private boolean awaitCondition(java.util.function.BooleanSupplier condition, Duration within) {

        try {

            return WaitUtils.waitUntil(driver -> condition.getAsBoolean() ? Boolean.TRUE : null, within);

        } catch (TimeoutException neverHappened) {

            return false;
        }
    }

}