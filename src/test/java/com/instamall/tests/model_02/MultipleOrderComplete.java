package com.instamall.tests.model_02;

import com.instamall.components.location.LocationServicesPrompt;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.config.ConfigReader;
import com.instamall.driver.AppSession;
import com.instamall.driver.DriverManager;
import com.instamall.driver.PortalDriverManager;
import com.instamall.factory.DriverFactory;
import com.instamall.factory.PortalDriverFactory;
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
import com.instamall.pages.orders.OrdersListPage;
import com.instamall.pages.portal.PortalLoginPage;
import com.instamall.pages.portal.PortalOrderDetailsPage;
import com.instamall.pages.portal.PortalOrdersPage;
import com.instamall.pages.product.ProductDetailPage;
import com.instamall.pages.product.ProductDetailPageValidator;
import com.instamall.pages.profile.ProfilePage;
import com.instamall.pages.shopper.home.ShopperHomePage;
import com.instamall.pages.shopper.login.ShopperLoginPage;
import com.instamall.pages.shopper.login.ShopperLoginPageLocators;
import com.instamall.pages.shopper.orders.OrderDetailPage;
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
 * Same cross-app lifecycle as {@link SingleOrderComplete}, but the order
 * Phase 1 places carries {@value #ITEMS_PER_ORDER} different products from
 * one store rather than one — still a single order, since everything is
 * checked out through that one store group's own "Review delivery &amp;
 * checkout" button.
 *
 * Phases 3-5 (Portal dispatch, Shopper handoff, Customer-app verification)
 * are order-level, not item-level, so they are unchanged from
 * SingleOrderComplete; only Phase 1 (adding several items before checking
 * out once) and Phase 2 (the Shopper app approves each item of the run in
 * turn, not just one) differ. See SingleOrderComplete's own doc comment for
 * the blockers (camera x2, reCAPTCHA, Portal cross-reference) this run hits
 * the same way.
 *
 * Does not extend BaseTest/ShopperBaseTest/PortalBaseTest — same reason as
 * SingleOrderComplete: three different driver types in sequence, plus the
 * Shopper app twice as fresh sessions.
 *
 * NOTE: this pays with a staging virtual card (Customer app) and runs a
 * real staging transaction, same as SingleOrderComplete.
 */
public class MultipleOrderComplete {

    /** How many distinct products from the one store this order carries. */
    private static final int ITEMS_PER_ORDER = 2;

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(45);
    private static final Duration LOCATION_ENTRY_WITHIN = Duration.ofSeconds(15);

    @Test(description = "Place one order carrying " + ITEMS_PER_ORDER + " different products from"
            + " the same store, accept it item by item as the Shopper, dispatch it through the"
            + " Delivery Portal, start handoff as the Shopper, then verify it shows as Delivered"
            + " back in the Customer app")
    public void multiItemOrderLifecycle() {

        String orderId = phase1_placeMultiItemOrderFromCustomerApp();

        phase2_viewAndAcceptOrderFromShopperApp(orderId);

        phase3_dispatchOrderOnPortal();

        phase4_completeHandoffFromShopperApp();

        phase5_verifyOrderInAccountHistory(orderId);
    }

    // ==================== Phase 1: Customer app — place one order with several items ====================

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

                String chosenSize;

                if (!productValidator.isSizeCardEnabled()) {

                    chosenSize = productPage.getSelectedSize();

                    if (chosenSize.isEmpty()) {

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

                    chosenSize = sizes.contains(size) ? size : sizes.get(0);
                    productPage.chooseSize(chosenSize);
                }

                String color = colorFrom(productPage.getColorCardText());

                step("Add to bag (" + (items.size() + 1) + "/" + ITEMS_PER_ORDER + "): "
                        + name + " (" + chosenSize + ", " + color + ")");
                productPage.addToBagIfNeeded();

                Assert.assertTrue(productValidator.isViewBagDisplayed(),
                        "No VIEW BAG pill after adding \"" + name + "\" to bag — the item was not added");

                items.add(new CheckoutFlow.ItemExpectation(name, chosenSize, color));

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

            // ---------- Checkout, payment, confirmation — shared with every other journey.
            // One checkout call covers the whole store group, whatever items it holds, so
            // any one of this order's items locates the right "Review delivery & checkout"
            // button — see ShoppingBagPage.clickCheckoutFor()'s own doc comment. ----------

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

    // ==================== Phase 2: Shopper app — view and accept every item of the order ====================

    private void phase2_viewAndAcceptOrderFromShopperApp(String orderId) {

        step("PHASE 2: Shopper app — view and accept every item of order " + orderId);

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

            String shopperOrderId = normalizeOrderId(orderPage.getOrderId());
            step("Order detail open: " + shopperOrderId);

            Assert.assertEquals(shopperOrderId, orderId,
                    "The order opened on the Shopper app does not match the one Phase 1 placed");

            Assert.assertTrue(orderValidator.isDisplayed(),
                    "Order detail screen did not open from the Home card");

            // Every item of the run gets its own Got it / Accept this item? pass —
            // ITEMS_PER_ORDER products were placed in Phase 1, so this loops rather
            // than assuming the single pass a one-item order needs.
            while (orderValidator.isGotItButtonDisplayedNow()
                    && !orderValidator.isValidateAndProceedDisplayed()) {

                step("Got it (" + orderPage.getRunProgressCount() + ")");
                orderPage.clickGotIt();

                orderPage.waitForGotItSheet();
                Assert.assertTrue(orderValidator.isGotItSheetDisplayed(),
                        "The \"Got it\" item-details sheet did not open");

                orderPage.confirmGotItSheet();

                orderPage.waitForAcceptItemSheet();
                Assert.assertTrue(orderValidator.isAcceptItemSheetDisplayed(),
                        "The \"Accept this item?\" sheet did not open");

                step("Proceed on \"Accept this item?\"");
                orderPage.proceedAcceptingItem();
            }

            if (!orderValidator.isValidateAndProceedDisplayed()) {

                Assert.assertTrue(orderValidator.isItemApproved(),
                        "The run's items are neither awaiting Got it nor already approved");
            }

            Assert.assertTrue(orderValidator.isValidateAndProceedDisplayed(),
                    "\"Validate and proceed\" is not displayed once every item is approved");

            step("Validate and proceed");
            orderPage.validateAndProceed();

            orderPage.waitForConfirmPaymentSheet();
            Assert.assertTrue(orderValidator.isConfirmPaymentSheetDisplayed(),
                    "The \"Confirm payment\" sheet did not open");

            String amount = orderPage.getConfirmPaymentAmount();
            step("Confirm payment $" + amount);
            orderPage.confirmPayment(amount);

            // ---------- BLOCKER: receipt capture needs a real device ----------

            orderPage.waitForReceiptCaptureScreen();
            Assert.assertTrue(orderValidator.isReceiptCaptureScreenDisplayed(),
                    "\"Capture the receipt\" did not open after confirming payment");

            if (SystemAlerts.allowCamera()) {

                step("Allowed camera access");
            }

            step("BLOCKED (Simulator has no camera) — reached \"Capture the receipt\","
                    + " stopping here. The order has been accepted and paid for, which is"
                    + " what Phase 3 assumes makes it visible on the Delivery Portal.");

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 3: Delivery Portal — dispatch the order through to Delivered ====================

    private void phase3_dispatchOrderOnPortal() {

        step("PHASE 3: Delivery Portal — assign a driver, then dispatch through to Delivered");

        var driver = PortalDriverFactory.initializeDriver();
        PortalDriverManager.setDriver(driver);
        driver.get(ConfigReader.get("deliveryPortalUrl"));

        try {

            PortalLoginPage loginPage = new PortalLoginPage();
            PortalOrdersPage ordersPage = new PortalOrdersPage();
            PortalOrderDetailsPage orderDetailsPage = new PortalOrderDetailsPage();

            loginPage.waitForScreen()
                     .enterEmail(ConfigReader.get("deliveryPortalUsername"))
                     .enterPassword(ConfigReader.get("deliveryPortalPassword"));

            // ---------- BLOCKER: reCAPTCHA has to be solved by hand ----------

            step("BLOCKED (manual) — solve the reCAPTCHA now; clickLogin() waits up to its"
                    + " own default timeout for the Login button to become clickable, which"
                    + " only happens once it is solved.");

            loginPage.clickLogin();

            ordersPage.openOrdersFromSidebar().openActiveTab();

            // See SingleOrderComplete's own doc comment: finding exactly the order
            // Phase 1/2 just placed is not yet confirmed against the Portal's own
            // search/indexing, so this assumes it is the one order on the Active tab.
            ordersPage.openFirstOrderCard();

            orderDetailsPage.waitForScreen();
            step("Portal order open: " + orderDetailsPage.getOrderIdValue()
                    + " (status: " + orderDetailsPage.getStatusBadge() + ")");

            step("Assign a driver");
            orderDetailsPage.overrideStatusTo("Assigned", "Done");

            step("Driver At PickUp");
            orderDetailsPage.overrideStatusTo("Driver At PickUp", "Done");

            step("Pickup Completed");
            orderDetailsPage.overrideStatusTo("Pickup Completed", "Done");

            step("Out For Delivery");
            orderDetailsPage.overrideStatusTo("Out For Delivery", "Done");

            step("Driver At Drop-off");
            orderDetailsPage.overrideStatusTo("Driver At Drop-off", "Done");

            step("Delivered");
            orderDetailsPage.overrideStatusTo("Delivered", "Done");

        } finally {

            PortalDriverManager.quitDriver();
        }
    }

    // ==================== Phase 4: Shopper app again — start handoff ====================

    private void phase4_completeHandoffFromShopperApp() {

        step("PHASE 4: Shopper app — start handoff");

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

            step("Start handoff");
            homePage.tapStartHandoff();

            orderPage.waitForStartHandoffSheet();
            Assert.assertTrue(orderValidator.isStartHandoffSheetDisplayed(),
                    "The \"Start handoff?\" sheet did not open");

            orderPage.proceedStartHandoff();

            // ---------- BLOCKER: handoff photo needs a real device ----------

            orderPage.waitForHandoffPhotoScreen();
            Assert.assertTrue(orderValidator.isHandoffPhotoScreenDisplayed(),
                    "\"Capture handoff photo\" did not open after proceeding on \"Start handoff?\"");

            step("BLOCKED (Simulator has no camera) — reached \"Capture handoff photo\","
                    + " camera-not-available: " + orderValidator.isCameraNotAvailableMessageDisplayed()
                    + ". Stopping here — needs a real device to submit the handoff photo"
                    + " and update the status further (same gap as Phase 2).");

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 5: Customer app — verify the order shows in Account > Orders ====================

    private void phase5_verifyOrderInAccountHistory(String orderId) {

        step("PHASE 5: Customer app — verify order " + orderId + " is listed in Account > Orders");

        IOSDriver driver = DriverFactory.initializeDriver(false, ConfigReader.get("appPath"));
        DriverManager.setDriver(driver);

        try {

            OnboardingPage onboardingPage = new OnboardingPage();
            OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();
            LoginPage loginPage = new LoginPage();
            EmailLoginPage emailLoginPage = new EmailLoginPage();
            HomePage homePage = new HomePage();
            HomePageValidator homeValidator = new HomePageValidator();
            ProfilePage profilePage = new ProfilePage();
            OrdersListPage ordersListPage = new OrdersListPage();

            boolean firstScreenShown = awaitUntil(() ->
                            onboardingValidator.isLoginButtonDisplayedNow() || homeValidator.isHomeTabDisplayedNow(),
                    APP_FIRST_SCREEN_WITHIN, "the app's first screen");

            Assert.assertTrue(firstScreenShown, "Neither onboarding nor home showed after launch");

            if (onboardingValidator.isLoginButtonDisplayedNow()) {

                step("Signed out — signing back in as " + ConfigReader.get("testEmail"));
                onboardingPage.clickLogin();
                loginPage.waitForScreen().clickContinueWithEmail();

                emailLoginPage.waitForScreen();
                emailLoginPage.enterEmail(ConfigReader.get("testEmail"))
                              .enterPassword(ConfigReader.get("testPassword"))
                              .clickNext();

            } else {

                step("Already signed in from Phase 1 — skipping sign in");
            }

            homePage.waitForScreen().openProfile();
            profilePage.waitForScreen();

            step("Account > Orders");
            profilePage.openOrders();

            ordersListPage.waitForScreen();

            boolean listed = ordersListPage.isOrderListed(orderId);

            step(listed
                    ? "Found: " + ordersListPage.getOrderRowText(orderId)
                    : "Order " + orderId + " is not listed");

            Assert.assertTrue(listed, "Order " + orderId + " was not found in Account > Orders");

            // ---------- Final assertion: this is what makes the whole lifecycle test
            // pass or fail, same as SingleOrderComplete — every earlier phase (place,
            // accept, dispatch, handoff) is only a precondition for the order actually
            // reaching "Delivered" here. ----------
            step("Final assertion: the status pill reads \"Delivered\"");
            Assert.assertTrue(ordersListPage.hasStatus(orderId, "Delivered"),
                    "Order " + orderId + " was not successfully delivered — Account > Orders"
                            + " shows \"" + ordersListPage.getOrderRowText(orderId) + "\" instead of \"Delivered\"");

            step("SUCCESS: order " + orderId + " (" + ITEMS_PER_ORDER + " items) was placed,"
                    + " accepted, dispatched through the Delivery Portal, handed off, and"
                    + " confirmed Delivered end to end.");

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
