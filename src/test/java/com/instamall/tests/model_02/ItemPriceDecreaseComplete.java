package com.instamall.tests.model_02;

import com.instamall.components.consent.ConsentPrompts;
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
import com.instamall.pages.home.HomePageLocators;
import com.instamall.pages.home.HomePageValidator;
import com.instamall.pages.location.SelectLocationSheet;
import com.instamall.pages.location.SelectLocationSheetValidator;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.pages.product.ProductDetailPage;
import com.instamall.pages.product.ProductDetailPageValidator;
import com.instamall.pages.profile.ProfilePage;
import com.instamall.pages.shopper.home.ShopperHomePage;
import com.instamall.pages.shopper.login.ShopperLoginPage;
import com.instamall.pages.shopper.login.ShopperLoginPageLocators;
import com.instamall.pages.shopper.orders.OrderDetailPage;
import com.instamall.pages.shopper.orders.OrderDetailPageValidator;
import com.instamall.pages.store.StorePage;
import com.instamall.utils.CurrencyUtils;
import com.instamall.waits.WaitUtils;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

/**
 * A shopper finds an item's in-store price has dropped below what it was
 * ordered at, lowers it on the "Got it" sheet, and the difference is
 * credited to the customer rather than kept by the store:
 *
 *   Customer app (place a single-item order, note the Wallet balance) -&gt;
 *   Shopper app (Got it -&gt; edit the per-unit price down -&gt; confirm -&gt;
 *   Accept this item -&gt; Validate and proceed -&gt; Confirm payment, where the
 *   credit is issued) -&gt; Customer app again (Wallet balance has grown by
 *   exactly the price difference)
 *
 * Confirmed live (same session that built this): lowering the price here
 * fires an in-app notification — "The price of an item has decreased. The
 * reduced amount will be credited to your InstaMall Wallet." — right when
 * Confirm payment is tapped, and the credit itself is already in the
 * Wallet by the time a fresh Customer-app session checks it; no need to
 * wait for the order to reach Delivered. That is also what lets this test
 * finish cleanly: it stops right after Confirm payment, before "Capture
 * the receipt" — this build's receipt-capture step (RETAKE / USE PHOTO /
 * the DEV skip-confirm shortcut alike) was found live to never leave that
 * screen, the same kind of camera-shaped gap SingleOrderComplete's own doc
 * comment already documents for the no-camera-hardware case, except this
 * one reproduced with a real camera permission granted and a DEV bypass
 * present — worth a closer look separately, but not what this test is
 * about. Phase 2 never needs that screen: the Wallet credit already fired
 * before it.
 *
 * Does not extend BaseTest/ShopperBaseTest: needs the Customer app for
 * Phases 1 and 3 and the Shopper app for Phase 2, same reasoning as
 * SingleOrderComplete's own doc comment.
 *
 * NOTE: this pays with a staging virtual card (Customer app) and runs a
 * real staging transaction, same as SingleOrderComplete.
 */
public class ItemPriceDecreaseComplete {

    /** How much lower the shopper finds the item for, as a fraction of its ordered price. */
    private static final BigDecimal PRICE_DECREASE_FACTOR = new BigDecimal("0.80");

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(45);
    private static final Duration LOCATION_ENTRY_WITHIN = Duration.ofSeconds(15);

    /** What Phase 1 places, and what Phase 3 needs to judge the outcome by. */
    private static final class Placement {

        final String orderId;
        final BigDecimal orderedPrice;
        final BigDecimal walletBefore;

        Placement(String orderId, BigDecimal orderedPrice, BigDecimal walletBefore) {

            this.orderId = orderId;
            this.orderedPrice = orderedPrice;
            this.walletBefore = walletBefore;
        }
    }

    @Test(description = "Shopper lowers an item's price below what it was ordered at — the"
            + " difference is credited to the customer's InstaMall Wallet rather than kept,"
            + " confirmed by the Wallet balance growing by exactly that difference")
    public void priceDecreaseCreditsTheWallet() {

        Placement placement = phase1_placeOrderAndNoteWallet();

        phase2_lowerPriceFromShopperApp(placement.orderId, placement.orderedPrice);

        phase3_verifyWalletCredited(placement);
    }

    // ==================== Phase 1: Customer app — place order, note the Wallet ====================

    private Placement phase1_placeOrderAndNoteWallet() {

        step("PHASE 1: Customer app — place a single-item order, note the Wallet balance");

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
            ProfilePage profilePage = new ProfilePage();

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

                // Save Password? / App Tracking Transparency / AI & Biometric
                // Consent / AI Stylist Consent — whichever of these show up.
                ConsentPrompts.acceptAfterSignIn(HomePageLocators.TAB_HOME);

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

            // ---------- Wallet balance, before anything is credited ----------

            homePage.openProfile();
            profilePage.waitForScreen();

            BigDecimal walletBefore = CurrencyUtils.parse(profilePage.getWalletTileText());
            step("Wallet before: " + walletBefore);

            profilePage.waitForScreen();
            homePage.openHomeTab();
            homePage.waitForScreen();

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

            BigDecimal orderedPrice = bagPage.getItemPrice(productName, chosenSize);
            step("Ordered at: " + orderedPrice);

            // ---------- Checkout, payment, confirmation — shared with every other journey ----------

            CheckoutFlow checkout = new CheckoutFlow(this::step);
            checkout.openCheckoutFromBag(productName, chosenSize);

            CheckoutFlow.Placed placed = checkout.placeOrder(addressName);
            String placedOrderId = checkout.payAndVerifyOrder(
                    placed, cardLast4, brand, productName, chosenSize, color);

            step("Order placed: " + placedOrderId);

            return new Placement(normalizeOrderId(placedOrderId), orderedPrice, walletBefore);

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 2: Shopper app — lower the item's price ====================

    private void phase2_lowerPriceFromShopperApp(String orderId, BigDecimal orderedPrice) {

        step("PHASE 2: Shopper app — lower order " + orderId + "'s item price below " + orderedPrice);

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
            step("Order detail open: " + shopperOrderId + " — run progress: " + orderPage.getRunProgressCount());

            Assert.assertEquals(shopperOrderId, orderId,
                    "The order opened on the Shopper app does not match the one Phase 1 placed");

            Assert.assertTrue(orderValidator.isDisplayed(),
                    "Order detail screen did not open from the Home card");

            Assert.assertTrue(orderValidator.isGotItButtonDisplayedNow(),
                    "\"GOT IT\" is not displayed for the only item in this run");

            step("Got it");
            orderPage.clickGotIt();

            orderPage.waitForGotItSheet();
            Assert.assertTrue(orderValidator.isGotItSheetDisplayed(),
                    "The \"Got it\" item-details sheet did not open");

            Assert.assertTrue(orderValidator.isPriceEditIconDisplayed(),
                    "The Per Unit Price edit icon is not displayed on the \"Got it\" sheet");

            orderPage.openPriceEditor();

            BigDecimal shownPrice = CurrencyUtils.parse(orderPage.getPerUnitPrice());
            step("Per Unit Price shown: " + shownPrice);

            Assert.assertTrue(CurrencyUtils.equalAmounts(shownPrice, orderedPrice),
                    "The \"Got it\" sheet shows a different price (" + shownPrice
                            + ") than the order was placed at (" + orderedPrice + ")");

            BigDecimal loweredPrice = orderedPrice.multiply(PRICE_DECREASE_FACTOR)
                    .setScale(2, RoundingMode.HALF_UP);

            step("Lowering price to: " + loweredPrice);
            orderPage.setPerUnitPrice(loweredPrice.toPlainString());

            orderPage.confirmGotItSheet();

            orderPage.waitForAcceptItemSheet();
            Assert.assertTrue(orderValidator.isAcceptItemSheetDisplayed(),
                    "The \"Accept this item?\" sheet did not open");

            step("Proceed on \"Accept this item?\"");
            orderPage.proceedAcceptingItem();

            Assert.assertTrue(orderValidator.isItemApproved(),
                    "Item does not show \"Approved\" after accepting it at the lowered price");

            Assert.assertTrue(orderValidator.isValidateAndProceedDisplayed(),
                    "\"Validate and proceed\" is not displayed once the item is approved");

            step("Validate and proceed");
            orderPage.validateAndProceed();

            orderPage.waitForConfirmPaymentSheet();
            Assert.assertTrue(orderValidator.isConfirmPaymentSheetDisplayed(),
                    "The \"Confirm payment\" sheet did not open");

            String amount = orderPage.getConfirmPaymentAmount();
            step("Confirm payment $" + amount + " — this is also when the Wallet credit notification fires");
            orderPage.confirmPayment(amount);

            step("Stopping here — the Wallet credit already fired at Confirm payment; nothing"
                    + " past this point (receipt capture) is needed for this scenario.");

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 3: Customer app — the Wallet grew by the price difference ====================

    private void phase3_verifyWalletCredited(Placement placement) {

        step("PHASE 3: Customer app — confirm the Wallet grew by the price difference");

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

                ConsentPrompts.acceptAfterSignIn(HomePageLocators.TAB_HOME);

            } else {

                step("Already signed in from Phase 1 — skipping sign in");
            }

            homePage.waitForScreen().openProfile();
            profilePage.waitForScreen();

            BigDecimal walletAfter = CurrencyUtils.parse(profilePage.getWalletTileText());
            step("Wallet after: " + walletAfter + " (was " + placement.walletBefore + ")");

            BigDecimal priceDifference = placement.orderedPrice
                    .multiply(BigDecimal.ONE.subtract(PRICE_DECREASE_FACTOR))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal expectedWallet = placement.walletBefore.add(priceDifference);

            // ---------- Final assertion: this is what makes the whole scenario pass
            // or fail — Phase 1 placing the order and Phase 2 lowering the price are
            // only preconditions; the scenario only counts as the customer
            // genuinely paying less once the Wallet actually holds the difference. ----------
            step("Final assertion: Wallet grew by exactly " + priceDifference
                    + " (expected " + expectedWallet + ")");
            Assert.assertTrue(CurrencyUtils.equalAmounts(walletAfter, expectedWallet),
                    "Wallet reads " + walletAfter + " — expected " + expectedWallet + " (before "
                            + placement.walletBefore + " + price difference " + priceDifference
                            + ") after order " + placement.orderId + "'s item was lowered from "
                            + placement.orderedPrice);

            step("SUCCESS: order " + placement.orderId + "'s item was lowered from "
                    + placement.orderedPrice + ", and the difference was credited to the"
                    + " customer's Wallet — the order was never overcharged for a price drop"
                    + " found in-store.");

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
