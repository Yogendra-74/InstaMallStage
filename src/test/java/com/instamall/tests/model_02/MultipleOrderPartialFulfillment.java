package com.instamall.tests.model_02;

import com.instamall.components.location.LocationServicesPrompt;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.config.ConfigReader;
import com.instamall.driver.AppSession;
import com.instamall.driver.DriverManager;
import com.instamall.factory.DriverFactory;
import com.instamall.flows.CheckoutFlow;
import com.instamall.pages.bag.ShoppingBagPage;
import com.instamall.pages.bag.ShoppingBagPageValidator;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.home.HomePageValidator;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.malls.MallDetailPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.pages.product.ProductDetailPage;
import com.instamall.pages.product.ProductDetailPageValidator;
import com.instamall.pages.shopper.home.ShopperHomePage;
import com.instamall.pages.shopper.login.ShopperLoginPage;
import com.instamall.pages.shopper.login.ShopperLoginPageLocators;
import com.instamall.pages.shopper.orders.OrderDetailPage;
import com.instamall.pages.shopper.orders.OrderDetailPageLocators;
import com.instamall.pages.shopper.orders.OrderDetailPageValidator;
import com.instamall.pages.store.StorePage;
import com.instamall.waits.WaitUtils;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A partially-fulfilled order: three items bought from one store, but the
 * shopper only finds two of them in the boutique — the third is marked
 * "Couldn't get this item" and refunded, while the other two are accepted
 * and the order proceeds with just those.
 *
 *   Customer app (place one order with 3 items from one store) -&gt;
 *   Shopper app (Got it on the 1st and 2nd items, Couldn't get this item
 *   on the 3rd -&gt; reason -&gt; issue its refund -&gt; Validate and proceed once
 *   all three are accounted for -&gt; Confirm payment, for the 2 found items
 *   only)
 *
 * Unlike {@link ItemNotFoundCancellationComplete}, which places a
 * single-item order so rejecting its only item cancels the order outright,
 * this one places three — rejecting one of three leaves two still to
 * fulfil, so "Validate and proceed" leads to Confirm payment rather than
 * an "Order processed / no items to pick up" cancellation. That contrast
 * is the point of this test: a partial reject does not have to mean a
 * fully rejected order.
 *
 * Stops at Confirm payment, same as {@link ItemPriceDecreaseComplete} and
 * {@link ItemPriceIncreaseComplete} — this build's "Capture the receipt"
 * step was found live to never complete (RETAKE / USE PHOTO / both DEV
 * shortcuts all leave the order stuck "IN PROGRESS" indefinitely, even
 * after waiting several minutes and across multiple sessions — see those
 * two classes' own doc comments for the full account). Nothing about this
 * scenario needs that screen: both the "found" items' Approved status and
 * the "not found" item's Rejected status are already settled by the time
 * Confirm payment is reached.
 *
 * Does not extend BaseTest/ShopperBaseTest: needs the Customer app for
 * Phase 1 and the Shopper app for Phase 2, same reasoning as
 * SingleOrderComplete's own doc comment.
 *
 * NOTE: this pays with a staging virtual card (Customer app) and runs a
 * real staging transaction, same as SingleOrderComplete/MultipleOrderComplete.
 */
public class MultipleOrderPartialFulfillment {

    /** Three items bought, so losing one still leaves two to fulfil. */
    private static final int ITEMS_PER_ORDER = 3;

    /** The last item presented in the run is the one the shopper can't find. */
    private static final int ITEMS_FOUND = ITEMS_PER_ORDER - 1;

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    // AppSession.clearAppData() does not just wipe local storage — it
    // runs a real uninstall-then-reinstall of the app (confirmed against
    // the simulator's own system log: a full uninstall/reinstall cycle,
    // ~20s on its own, then another ~15-20s before the relaunched app is
    // actually interactive), all of which happens before onboarding ever
    // paints. 45s left too little headroom for that plus the guest tap
    // and Home's own network-bound load, and failed intermittently under
    // load as a result.
    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(90);
    private static final Duration LOCATION_ENTRY_WITHIN = Duration.ofSeconds(15);

    @Test(description = "Place one order with 3 items from the same store, then as the Shopper"
            + " find 2 of them (Got it) and mark the 3rd \"Couldn't get this item\" — confirm the"
            + " order proceeds to Confirm payment for the 2 found items rather than being"
            + " cancelled outright, since two items are still being fulfilled")
    public void partialOrderProceedsWithItemsFound() {

        String orderId = phase1_placeMultiItemOrderFromCustomerApp();

        phase2_findSomeItemsFromShopperApp();
    }

    // ==================== Phase 1: Customer app — place a 3-item order ====================

    private String phase1_placeMultiItemOrderFromCustomerApp() {

        step("PHASE 1: Customer app — place one order with " + ITEMS_PER_ORDER
                + " items from one store");

        IOSDriver driver = DriverFactory.initializeDriver(false, ConfigReader.get("appPath"));
        DriverManager.setDriver(driver);

        try {

            AppSession.clearAppData();
            AppSession.grantLocationAndNotifications();
            AppSession.setSimulatedLocation(MIAMI_LATITUDE, MIAMI_LONGITUDE);

            String bundleId = ConfigReader.get("bundleId");
            driver.terminateApp(bundleId);
            driver.activateApp(bundleId);

            OnboardingPage onboardingPage = new OnboardingPage();
            OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();
            LoginPage loginPage = new LoginPage();
            EmailLoginPage emailLoginPage = new EmailLoginPage();
            HomePage homePage = new HomePage();
            HomePageValidator homeValidator = new HomePageValidator();
            StorePage storePage = new StorePage();
            MallDetailPage mallPage = new MallDetailPage();
            ProductDetailPage productPage = new ProductDetailPage();
            ProductDetailPageValidator productValidator = new ProductDetailPageValidator();
            ShoppingBagPage bagPage = new ShoppingBagPage();
            ShoppingBagPageValidator bagValidator = new ShoppingBagPageValidator();

            String email = ConfigReader.get("testEmail");
            String password = ConfigReader.get("testPassword");
            String addressName = ConfigReader.get("deliveryAddressName");
            String size = ConfigReader.get("productSize");
            String cardLast4 = ConfigReader.get("savedCardLast4");

            // ---------- Onboarding ----------

            boolean onboardingShown = awaitUntil(() -> {

                        SystemAlerts.grantAnyShowingNow();

                        return onboardingValidator.isLoginButtonDisplayedNow()
                                || homeValidator.isHomeTabDisplayedNow();
                    },
                    APP_FIRST_SCREEN_WITHIN, "the app's first screen");

            Assert.assertTrue(onboardingShown, "Neither onboarding nor home showed after a fresh relaunch");

            if (onboardingValidator.isLoginButtonDisplayedNow()) {

                step("Onboarding -> Make it personal -> Login with email");
                onboardingPage.clickSignUp();
                loginPage.waitForScreen().clickContinueWithEmail();

                emailLoginPage.waitForScreen();
                step("Sign in as " + email);
                emailLoginPage.enterEmail(email).enterPassword(password).clickNext();

            } else {

                step("A session is already active — skipping sign in");
            }

            // ---------- Home ----------

            homePage.waitForScreen();
            step("Home screen");

            // ---------- Store ----------

            step("Explore Malls -> Miami Design District -> Burberry");
            homePage.openMallCard("Miami Design District");

            mallPage.waitForScreen().openStore("Burberry");

            String brand = storePage.waitForScreen().getBrandName();
            step("Store: " + brand);

            // ---------- Add ITEMS_PER_ORDER products from this one store ----------

            List<String> tiles = storePage.listVisibleProductTiles();
            Assert.assertFalse(tiles.isEmpty(), "No product tiles on the " + brand + " grid");

            List<CheckoutFlow.ItemExpectation> items = new ArrayList<>();

            for (String tile : tiles) {

                if (items.size() == ITEMS_PER_ORDER) {

                    break;
                }

                String name = tileNameWithoutBrandAndPrice(tile, brand);

                storePage.openProductTile(tile);
                productPage.waitForScreen();

                if (!productValidator.isSizeCardDisplayed()) {

                    productPage.goBack();
                    storePage.waitForScreen();
                    continue;
                }

                String candidateSize;

                if (!productValidator.isSizeCardEnabled()) {

                    candidateSize = productPage.getSelectedSize();

                    if (candidateSize.isEmpty()) {

                        productPage.goBack();
                        storePage.waitForScreen();
                        continue;
                    }

                } else {

                    List<String> sizes = productPage.openSizeSheet().getAvailableSizes();

                    if (sizes.isEmpty()) {

                        productPage.closeSizeSheet().goBack();
                        storePage.waitForScreen();
                        continue;
                    }

                    candidateSize = sizes.contains(size) ? size : sizes.get(0);
                    productPage.chooseSize(candidateSize);
                }

                String color = colorFrom(productPage.getColorCardText());

                step("Add to bag (" + (items.size() + 1) + "/" + ITEMS_PER_ORDER + "): "
                        + name + " (" + candidateSize + ", " + color + ")");
                productPage.addToBagIfNeeded();

                Assert.assertTrue(productValidator.isViewBagDisplayed(),
                        "No VIEW BAG pill after adding \"" + name + "\" to bag — the item was not added");

                items.add(new CheckoutFlow.ItemExpectation(name, candidateSize, color));

                productPage.goBack();
                storePage.waitForScreen();
            }

            Assert.assertEquals(items.size(), ITEMS_PER_ORDER,
                    "Could only add " + items.size() + " of " + ITEMS_PER_ORDER + " items from the "
                            + brand + " grid — not enough sellable/sized products on it to run this test");

            storePage.clickViewBag();

            bagPage.waitForScreen();

            for (CheckoutFlow.ItemExpectation item : items) {

                bagPage.waitForItem(item.product(), item.size());
            }

            Assert.assertTrue(bagValidator.isStoreGroupDisplayed(brand), "Bag is missing the " + brand + " group");

            for (CheckoutFlow.ItemExpectation item : items) {

                Assert.assertTrue(bagPage.hasItem(item.product(), item.size()),
                        "Bag has no row for \"" + item.product() + "\" in " + item.size());
            }

            // ---------- Checkout, payment, confirmation — shared with every other journey ----------

            CheckoutFlow.ItemExpectation first = items.get(0);

            CheckoutFlow checkout = new CheckoutFlow(this::step);
            checkout.openCheckoutFromBag(first.product(), first.size());

            CheckoutFlow.Placed placed = checkout.placeOrder(addressName);
            String placedOrderId = checkout.payAndVerifyOrder(placed, cardLast4, brand, items);

            step("Order placed: " + placedOrderId + " (" + items.size() + " items)");

            return normalizeOrderId(placedOrderId);

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 2: Shopper app — find 2 of the 3 items ====================

    private void phase2_findSomeItemsFromShopperApp() {

        step("PHASE 2: Shopper app — Got it on " + ITEMS_FOUND + " item(s), \"Couldn't get this"
                + " item\" on the rest, for the first order in the queue");

        IOSDriver driver = DriverFactory.initializeDriver(
                false, ConfigReader.get("shopperAppPath"), List.of("-mockDocumentScan"));
        DriverManager.setDriver(driver);

        try {

            awaitShopperFirstScreen();

            ShopperLoginPage loginPage = new ShopperLoginPage();
            ShopperHomePage homePage = new ShopperHomePage();
            OrderDetailPage orderPage = new OrderDetailPage();
            OrderDetailPageValidator orderValidator = new OrderDetailPageValidator();

            step("Log in as " + ConfigReader.get("shopperEmail"));
            loginPage.waitForScreen()
                     .enterEmail(ConfigReader.get("shopperEmail"))
                     .enterPassword(ConfigReader.get("shopperPassword"))
                     .clickLogIn();

            homePage.waitForScreen();
            step("On Home");

            homePage.openNewOrderCard();
            orderPage.waitForScreen();

            step("Order detail open — run progress: " + orderPage.getRunProgressCount());

            Assert.assertTrue(orderValidator.isDisplayed(),
                    "Order detail screen did not open from the Home card");

            // Walks the run item by item: the first ITEMS_FOUND get Got it / Accept
            // this item, and the last one gets Couldn't get this item / a refund —
            // same shape as MultipleOrderComplete's accept-everything loop, except
            // this one's last pass takes the opposite branch on purpose.
            for (int itemNumber = 1; itemNumber <= ITEMS_PER_ORDER; itemNumber++) {

                Assert.assertFalse(orderValidator.isValidateAndProceedDisplayed(),
                        "\"Validate and proceed\" showed up after only " + (itemNumber - 1)
                                + " of " + ITEMS_PER_ORDER + " items were accounted for");

                if (itemNumber <= ITEMS_FOUND) {

                    step("Item " + itemNumber + "/" + ITEMS_PER_ORDER + " — Got it ("
                            + orderPage.getRunProgressCount() + ")");

                    Assert.assertTrue(orderValidator.isGotItButtonDisplayedNow(),
                            "\"GOT IT\" is not displayed for item " + itemNumber);

                    orderPage.clickGotIt();

                    orderPage.waitForGotItSheet();
                    Assert.assertTrue(orderValidator.isGotItSheetDisplayed(),
                            "The \"Got it\" item-details sheet did not open for item " + itemNumber);

                    orderPage.confirmGotItSheet();

                    orderPage.waitForAcceptItemSheet();
                    Assert.assertTrue(orderValidator.isAcceptItemSheetDisplayed(),
                            "The \"Accept this item?\" sheet did not open for item " + itemNumber);

                    orderPage.proceedAcceptingItem();

                    Assert.assertTrue(orderValidator.isItemApproved(),
                            "Item " + itemNumber + " does not show \"Approved\" after accepting it");

                } else {

                    step("Item " + itemNumber + "/" + ITEMS_PER_ORDER + " — Couldn't get this item ("
                            + orderPage.getRunProgressCount() + ")");

                    orderPage.couldNotGetItem();

                    orderPage.waitForCouldNotGetSheet();
                    Assert.assertTrue(orderValidator.isCouldNotGetSheetDisplayed(),
                            "The \"Couldn't get\" reason sheet did not open for item " + itemNumber);

                    step("Reason: " + OrderDetailPageLocators.REASON_OUT_OF_STOCK_ENTIRELY);
                    orderPage.chooseCouldNotGetReason(OrderDetailPageLocators.REASON_OUT_OF_STOCK_ENTIRELY);

                    String refundLabel = orderPage.getIssueRefundButtonLabel();
                    step("Issue refund: " + refundLabel);
                    orderPage.clickIssueRefund();

                    orderPage.waitForIssueRefundConfirmation();
                    Assert.assertTrue(orderValidator.isIssueRefundConfirmationDisplayed(),
                            "The \"Issue refund?\" confirmation did not open for item " + itemNumber);

                    orderPage.confirmIssueRefund();

                    Assert.assertTrue(orderValidator.isItemRejected(),
                            "Item " + itemNumber + " does not show \"Rejected\" after the refund was issued");
                }
            }

            step("All " + ITEMS_PER_ORDER + " items accounted for — run progress: "
                    + orderPage.getRunProgressCount());

            // ---------- Final assertion: this is what makes the whole scenario pass
            // or fail — Phase 1 placing the order and the per-item Got it / Couldn't
            // get actions above are only preconditions. A partial reject (one of
            // three) must still leave "Validate and proceed" leading somewhere —
            // Confirm payment for the items actually found — rather than the
            // whole-order cancellation ItemNotFoundCancellationComplete's single-item
            // order hits instead. ----------
            Assert.assertTrue(orderValidator.isValidateAndProceedDisplayed(),
                    "\"Validate and proceed\" is not displayed once all " + ITEMS_PER_ORDER
                            + " items are accounted for");

            step("Validate and proceed");
            orderPage.validateAndProceed();

            orderPage.waitForConfirmPaymentSheet();
            Assert.assertTrue(orderValidator.isConfirmPaymentSheetDisplayed(),
                    "The \"Confirm payment\" sheet did not open — a partial reject (one of "
                            + ITEMS_PER_ORDER + ") should still lead here for the "
                            + ITEMS_FOUND + " item(s) actually found, not an order-cancelled outcome");

            step("Confirm payment — for the " + ITEMS_FOUND
                    + " item(s) found, not all " + ITEMS_PER_ORDER + " ordered");
            orderPage.confirmPayment();

            step("SUCCESS: the order proceeded with " + ITEMS_FOUND + " of "
                    + ITEMS_PER_ORDER + " items found and accepted, the remaining item was"
                    + " refunded as not found, and Confirm payment opened for the items actually"
                    + " picked up — stopping here, same \"Capture the receipt\" gap as"
                    + " ItemPriceDecreaseComplete/ItemPriceIncreaseComplete.");

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Shared helpers ====================

    private void awaitShopperFirstScreen() {

        awaitUntil(() -> {

                    SystemAlerts.grantAnyShowingNow();

                    return WaitUtils.isVisibleNow(ShopperLoginPageLocators.LOG_IN_BUTTON)
                            || ShopperHomePage.isDisplayedNow();

                }, APP_FIRST_SCREEN_WITHIN,
                "the Shopper app's first screen (login or dashboard)");
    }

    private boolean awaitUntil(java.util.function.BooleanSupplier condition, Duration timeout, String description) {

        try {

            return WaitUtils.waitUntil(driver -> condition.getAsBoolean() ? Boolean.TRUE : null, timeout);

        } catch (TimeoutException neverHappened) {

            System.out.println(">>> Timed out waiting for " + description);
            return false;
        }
    }

    /**
     * Both "payAndVerifyOrder()" (Customer app) and "getOrderId()"
     * (Shopper app) read the same id off their own screen, but the
     * Customer app's can carry a leading "#" the Shopper app's never
     * does — confirmed live. Compare the two on equal footing rather than
     * assuming either one's formatting.
     */
    private String normalizeOrderId(String orderId) {

        return orderId.replaceFirst("^#", "").trim();
    }

    private String tileNameWithoutBrandAndPrice(String tileName, String brand) {

        int brandAt = tileName.lastIndexOf(" " + brand + " $");

        return brandAt < 0 ? tileName : tileName.substring(0, brandAt);
    }

    private String colorFrom(String colorCardText) {

        return colorCardText.replaceFirst("^Color\\s*·?\\s*", "").trim();
    }

    private void step(String description) {

        System.out.println(">>> STEP: " + description);
    }

}
