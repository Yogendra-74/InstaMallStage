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
import java.util.List;

/**
 * The negative twin of {@link SingleOrderComplete}: a shopper who cannot
 * find an order's (only) item marks it "Couldn't get this item" rather than
 * "Got it", issues the refund the app offers for that, and the order is
 * cancelled outright rather than ever reaching the Delivery Portal.
 *
 *   Customer app (place a single-item order) -&gt; Shopper app (open the
 *   order, mark its one item "Couldn't get this item" with a reason,
 *   issue the refund, confirm) -&gt; Shopper app Home (verify the order is
 *   gone from Active and shows "ORDER REJECTED" in History)
 *
 * Confirmed live (same session that built this): with only one item in
 * the run, rejecting it does not just reject that line — "Validate and
 * proceed" goes on to show an "Order processed" alert ("Order submitted
 * with no items to pick up.") and the whole order leaves Active for
 * History as "ORDER REJECTED". A multi-item order would only lose that
 * one item; this class deliberately places a single-item order so the
 * one rejection is also the order's last word.
 *
 * Does not extend BaseTest/ShopperBaseTest: needs the Customer app for
 * Phase 1 and the Shopper app for Phase 2, same reasoning as
 * SingleOrderComplete's own doc comment.
 *
 * No Delivery Portal or handoff phase here on purpose — a rejected order
 * never reaches a driver, so there is nothing further to dispatch.
 *
 * NOTE: this pays with a staging virtual card (Customer app) and runs a
 * real staging transaction, same as SingleOrderComplete; Phase 2 then
 * refunds it in full through the Shopper app's own "Couldn't get" flow.
 */
public class ItemNotFoundCancellationComplete {

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(45);
    private static final Duration LOCATION_ENTRY_WITHIN = Duration.ofSeconds(15);

    @Test(description = "Place a single-item order, then as the Shopper mark that item"
            + " \"Couldn't get this item\" and issue the refund — confirm the order is"
            + " cancelled outright (gone from Active, \"ORDER REJECTED\" in History)"
            + " rather than ever reaching the Delivery Portal")
    public void itemNotFoundCancelsTheOrder() {

        String orderId = phase1_placeOrderFromCustomerApp();

        phase2_rejectOnlyItemFromShopperApp(orderId);
    }

    // ==================== Phase 1: Customer app — place a single-item order ====================

    private String phase1_placeOrderFromCustomerApp() {

        step("PHASE 1: Customer app — place a single-item order");

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

            // ---------- Store and product ----------

            List<String> storeCards = homePage.listVisibleStoreCards();
            Assert.assertFalse(storeCards.isEmpty(), "No store cards on the home feed");

            String storeCard = storeCards.get(0);
            homePage.openStoreCard(storeCard);

            String brand = storePage.waitForScreen().getBrandName();
            step("Store: " + brand);

            List<String> tiles = storePage.listVisibleProductTiles();
            Assert.assertFalse(tiles.isEmpty(), "No product tiles on the " + brand + " grid");

            String productName = null;
            String chosenSize = null;
            String color = null;

            for (String tile : tiles) {

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

                productName = name;
                chosenSize = candidateSize;
                color = colorFrom(productPage.getColorCardText());
                break;
            }

            Assert.assertNotNull(productName,
                    "Not one product on the " + brand + " grid could be bought"
                            + " — every tile was out of stock or sizeless");

            step("Add to bag: " + productName + " (" + chosenSize + ", " + color + ")");
            productPage.addToBagIfNeeded();

            Assert.assertTrue(productValidator.isViewBagDisplayed(),
                    "No VIEW BAG pill after adding to bag — the item was not added");

            productPage.clickViewBag();

            bagPage.waitForScreen();
            bagPage.waitForItem(productName, chosenSize);

            Assert.assertTrue(bagValidator.isStoreGroupDisplayed(brand), "Bag is missing the " + brand + " group");
            Assert.assertTrue(bagPage.hasItem(productName, chosenSize),
                    "Bag has no row for \"" + productName + "\" in " + chosenSize);

            // ---------- Checkout, payment, confirmation — shared with every other journey ----------

            CheckoutFlow checkout = new CheckoutFlow(this::step);
            checkout.openCheckoutFromBag(productName, chosenSize);

            CheckoutFlow.Placed placed = checkout.placeOrder(addressName);
            String placedOrderId = checkout.payAndVerifyOrder(
                    placed, cardLast4, brand, productName, chosenSize, color);

            step("Order placed: " + placedOrderId);

            return normalizeOrderId(placedOrderId);

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 2: Shopper app — reject the only item, confirm the order cancels ====================

    private void phase2_rejectOnlyItemFromShopperApp(String orderId) {

        step("PHASE 2: Shopper app — mark the only item of order " + orderId
                + " \"Couldn't get this item\" and issue the refund");

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

            Assert.assertTrue(orderValidator.isGotItButtonDisplayedNow(),
                    "\"GOT IT\" / \"Couldn't get this item\" is not displayed for the only item in this run");

            step("Couldn't get this item");
            orderPage.couldNotGetItem();

            orderPage.waitForCouldNotGetSheet();
            Assert.assertTrue(orderValidator.isCouldNotGetSheetDisplayed(),
                    "The \"Couldn't get\" reason sheet did not open");

            step("Reason: " + OrderDetailPageLocators.REASON_OUT_OF_STOCK_ENTIRELY);
            orderPage.chooseCouldNotGetReason(OrderDetailPageLocators.REASON_OUT_OF_STOCK_ENTIRELY);

            String refundLabel = orderPage.getIssueRefundButtonLabel();
            step("Issue refund: " + refundLabel);
            orderPage.clickIssueRefund();

            orderPage.waitForIssueRefundConfirmation();
            Assert.assertTrue(orderValidator.isIssueRefundConfirmationDisplayed(),
                    "The \"Issue refund?\" confirmation did not open");

            step("Proceed");
            orderPage.confirmIssueRefund();

            Assert.assertTrue(orderValidator.isItemRejected(),
                    "Item does not show \"Rejected\" after the refund was issued");

            step("Validate and proceed");
            orderPage.validateAndProceed();

            orderPage.waitForOrderProcessedAlert();
            Assert.assertTrue(orderValidator.isOrderProcessedAlertDisplayed(),
                    "The \"Order processed\" alert did not open — with every item in the run rejected,"
                            + " this is what confirms the order itself was cancelled rather than just the item");

            step("OK");
            orderPage.dismissOrderProcessedAlert();

            homePage.waitForScreen();

            // ---------- Final assertion: this is what makes the whole scenario pass
            // or fail — Phase 1 placing the order and the item-level "Rejected"
            // status above are only preconditions; the scenario only counts as a
            // genuinely cancelled order once Active shows none left. ----------
            step("Final assertion: Active shows \"No active orders.\"");
            Assert.assertTrue(homeValidator.isNoActiveOrdersDisplayed(),
                    "Order " + orderId + " still shows under Active — rejecting its only item"
                            + " did not cancel the order");

            homePage.openHistoryTab();
            Assert.assertTrue(homeValidator.isOrderCardDisplayed("ORDER REJECTED"),
                    "No \"ORDER REJECTED\" card found in History after the order was cancelled");

            step("SUCCESS: order " + orderId + " was placed, its only item was marked \"Couldn't get"
                    + " this item\", the refund was issued, and the order itself was cancelled end to end.");

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
