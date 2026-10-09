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
import com.instamall.pages.location.SelectLocationSheet;
import com.instamall.pages.location.SelectLocationSheetValidator;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.pages.product.ProductDetailPage;
import com.instamall.pages.product.ProductDetailPageValidator;
import com.instamall.pages.shopper.home.ShopperHomePage;
import com.instamall.pages.shopper.home.ShopperHomePageValidator;
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
 * A multi-item order that the shopper cannot fulfil at all: every item
 * bought from the one store is marked "Couldn't get this item", and —
 * unlike {@link MultipleOrderPartialFulfillment}, where losing one of
 * three still leaves two to fulfil — rejecting all of them cancels the
 * order outright, the same way {@link ItemNotFoundCancellationComplete}'s
 * single-item order does.
 *
 *   Customer app (place one order with 2 items from one store) -&gt;
 *   Shopper app (Couldn't get this item -&gt; reason -&gt; issue refund, for
 *   every item in the run) -&gt; "Validate and proceed" leads straight to
 *   "Order processed — Order submitted with no items to pick up." rather
 *   than Confirm payment -&gt; Shopper app Home (order gone from Active,
 *   "ORDER REJECTED" in History)
 *
 * This is the multi-item generalisation of
 * {@link ItemNotFoundCancellationComplete}'s single-item case, and the
 * mirror image of {@link MultipleOrderPartialFulfillment}'s partial-reject
 * case: the deciding factor for whether an order survives a reject is not
 * "is this a multi-item order" but "is there anything left approved" —
 * zero approved items cancels the order regardless of how many it
 * started with.
 *
 * No Confirm payment, receipt capture, Delivery Portal, or handoff phase
 * here on purpose — a fully rejected order never reaches any of those,
 * same reasoning as ItemNotFoundCancellationComplete's own doc comment.
 *
 * Does not extend BaseTest/ShopperBaseTest: needs the Customer app for
 * Phase 1 and the Shopper app for Phase 2, same reasoning as
 * SingleOrderComplete's own doc comment.
 *
 * NOTE: this pays with a staging virtual card (Customer app) and runs a
 * real staging transaction, same as SingleOrderComplete/MultipleOrderComplete;
 * Phase 2 then refunds it in full through the Shopper app's own "Couldn't
 * get" flow, once per item.
 */
public class MultipleOrderNoItemsFoundComplete {

    /** Two items bought, and the shopper finds neither. */
    private static final int ITEMS_PER_ORDER = 2;

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(45);
    private static final Duration LOCATION_ENTRY_WITHIN = Duration.ofSeconds(15);

    @Test(description = "Place one order with 2 items from the same store, then as the Shopper"
            + " mark every item \"Couldn't get this item\" — confirm the order is cancelled"
            + " outright (gone from Active, \"ORDER REJECTED\" in History) rather than"
            + " reaching Confirm payment, since nothing in the run was ever approved")
    public void noItemsFoundCancelsTheOrder() {

        String orderId = phase1_placeMultiItemOrderFromCustomerApp();

        phase2_rejectEveryItemFromShopperApp(orderId);
    }

    // ==================== Phase 1: Customer app — place a multi-item order ====================

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
            SelectLocationSheet locationSheet = new SelectLocationSheet();
            SelectLocationSheetValidator locationValidator = new SelectLocationSheetValidator();
            StorePage storePage = new StorePage();
            ProductDetailPage productPage = new ProductDetailPage();
            ProductDetailPageValidator productValidator = new ProductDetailPageValidator();
            ShoppingBagPage bagPage = new ShoppingBagPage();
            ShoppingBagPageValidator bagValidator = new ShoppingBagPageValidator();

            String email = ConfigReader.get("testEmail");
            String password = ConfigReader.get("testPassword");
            String addressName = ConfigReader.get("deliveryAddressName");
            String size = ConfigReader.get("productSize");
            String cardLast4 = ConfigReader.get("savedCardLast4");

            // ---------- Sign in ----------

            boolean onboardingShown = awaitUntil(() ->
                    onboardingValidator.isLoginButtonDisplayedNow() || homeValidator.isHomeTabDisplayedNow(),
                    APP_FIRST_SCREEN_WITHIN, "the app's first screen");

            Assert.assertTrue(onboardingShown, "Neither onboarding nor home showed after a fresh relaunch");

            if (onboardingValidator.isLoginButtonDisplayedNow()) {

                step("Onboarding -> Login -> email");
                onboardingPage.clickLogin();
                loginPage.waitForScreen().clickContinueWithEmail();

                emailLoginPage.waitForScreen();
                step("Sign in as " + email);
                emailLoginPage.enterEmail(email).enterPassword(password).clickNext();

            } else {

                step("A session is already active — skipping sign in");
            }

            SystemAlerts.allowLocationWhileUsingApp();
            SystemAlerts.allowNotifications();

            // ---------- Delivery location ----------

            step("Select delivery location: " + addressName);

            boolean promptShown = awaitUntil(LocationServicesPrompt::isShowingNow,
                    LOCATION_ENTRY_WITHIN, "the Enable Location sheet (falls back to the header chip)");

            if (promptShown) {

                LocationServicesPrompt.selectLocationNow();

            } else {

                homePage.waitForScreen();

                if (!(homeValidator.isLocationSelected(addressName) && !homePage.listVisibleStoreCards().isEmpty())) {

                    homePage.openLocationSelector();

                } else {

                    step("Already at \"" + addressName + "\" with the malls listed — nothing to select");
                    locationSheet = null;
                }
            }

            if (locationSheet != null) {

                locationSheet.waitUntilVisible().waitForScreen();

                Assert.assertTrue(locationValidator.isSavedAddressDisplayed(addressName),
                        "Saved address \"" + addressName + "\" is not listed in the location sheet");

                locationSheet.chooseSavedAddress(addressName);
            }

            // ---------- Home ----------

            homePage.waitForScreen();
            step("Home screen");

            Assert.assertTrue(homeValidator.isLocationSelected(addressName),
                    "Home header does not show \"" + addressName + "\" after selecting it");

            // ---------- Store ----------

            List<String> storeCards = homePage.listVisibleStoreCards();
            Assert.assertFalse(storeCards.isEmpty(), "No store cards on the home feed");

            String storeCard = storeCards.get(0);
            homePage.openStoreCard(storeCard);

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

    // ==================== Phase 2: Shopper app — reject every item, confirm the order cancels ====================

    private void phase2_rejectEveryItemFromShopperApp(String orderId) {

        step("PHASE 2: Shopper app — mark every item of order " + orderId
                + " \"Couldn't get this item\" and issue its refund");

        IOSDriver driver = DriverFactory.initializeDriver(
                false, ConfigReader.get("shopperAppPath"), List.of("-mockDocumentScan"));
        DriverManager.setDriver(driver);

        try {

            awaitShopperFirstScreen();

            ShopperLoginPage loginPage = new ShopperLoginPage();
            ShopperHomePage homePage = new ShopperHomePage();
            ShopperHomePageValidator homeValidator = new ShopperHomePageValidator();
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

            String shopperOrderId = normalizeOrderId(orderPage.getOrderId());
            step("Order detail open: " + shopperOrderId + " — run progress: " + orderPage.getRunProgressCount());

            Assert.assertEquals(shopperOrderId, orderId,
                    "The order opened on the Shopper app does not match the one Phase 1 placed");

            Assert.assertTrue(orderValidator.isDisplayed(),
                    "Order detail screen did not open from the Home card");

            // Every item of the run gets "Couldn't get this item" — none of
            // ITEMS_PER_ORDER are ever accepted, unlike
            // MultipleOrderPartialFulfillment's mixed loop.
            for (int itemNumber = 1; itemNumber <= ITEMS_PER_ORDER; itemNumber++) {

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

            step("All " + ITEMS_PER_ORDER + " items rejected — run progress: "
                    + orderPage.getRunProgressCount());

            Assert.assertTrue(orderValidator.isValidateAndProceedDisplayed(),
                    "\"Validate and proceed\" is not displayed once every item is accounted for");

            step("Validate and proceed");
            orderPage.validateAndProceed();

            orderPage.waitForOrderProcessedAlert();
            Assert.assertTrue(orderValidator.isOrderProcessedAlertDisplayed(),
                    "The \"Order processed\" alert did not open — with every item in the run"
                            + " rejected, this is what confirms the order itself was cancelled"
                            + " rather than proceeding to Confirm payment for any of it");

            step("OK");
            orderPage.dismissOrderProcessedAlert();

            homePage.waitForScreen();

            // ---------- Final assertion: this is what makes the whole scenario pass
            // or fail — Phase 1 placing the order and rejecting every item above are
            // only preconditions; the scenario only counts as a genuinely cancelled
            // order once Active shows none left. ----------
            step("Final assertion: Active shows \"No active orders.\"");
            Assert.assertTrue(homeValidator.isNoActiveOrdersDisplayed(),
                    "Order " + orderId + " still shows under Active — rejecting every item"
                            + " did not cancel the order");

            homePage.openHistoryTab();
            Assert.assertTrue(homeValidator.isOrderCardDisplayed("ORDER REJECTED"),
                    "No \"ORDER REJECTED\" card found in History after the order was cancelled");

            step("SUCCESS: order " + orderId + " was placed with " + ITEMS_PER_ORDER + " items,"
                    + " none of which the shopper could find, every item's refund was issued,"
                    + " and the order itself was cancelled end to end — confirming zero approved"
                    + " items cancels an order regardless of how many it started with.");

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
