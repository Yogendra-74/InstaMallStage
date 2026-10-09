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
import java.util.ArrayList;
import java.util.List;

/**
 * A two-item order where only one item's price has risen in-store: the
 * shopper accepts the first item as ordered, but raises the second's
 * per-unit price before accepting it — and the customer is not the one
 * who absorbs that increase, the same rule {@link ItemPriceIncreaseComplete}
 * already establishes for a single-item order.
 *
 *   Customer app (place one order with 2 items from one store, note the
 *   Wallet balance) -&gt; Shopper app (Got it on item 1 as ordered -&gt; Got it
 *   on item 2, but edit its per-unit price up first -&gt; Validate and
 *   proceed once both are approved -&gt; Confirm payment) -&gt; Customer app
 *   again (Wallet balance is unchanged — no debit for the increase)
 *
 * The point of this test, distinct from both
 * {@link ItemPriceIncreaseComplete} (single-item order, whole order priced
 * up) and {@link MultipleOrderPartialFulfillment} (multi-item order, one
 * item rejected rather than price-edited): a price increase on only one
 * item of several does not cancel anything and does not touch the other
 * item's own price — both are still approved and the run still proceeds
 * to Confirm payment, exactly as if neither price had changed.
 *
 * UNCONFIRMED LIVE for the same reason as ItemPriceIncreaseComplete: this
 * session never exercised a price increase live (only a decrease, on
 * ItemPriceDecreaseComplete) — per instruction, this assumes the same
 * symmetric behaviour (the platform/store absorbs an increase) and
 * encodes it the same way. See that class's own doc comment for the full
 * reasoning.
 *
 * Stops at Confirm payment, same as ItemPriceDecreaseComplete/
 * ItemPriceIncreaseComplete/MultipleOrderPartialFulfillment — this
 * build's "Capture the receipt" step was found live to never complete
 * (see those classes' own doc comments for the full account).
 *
 * Does not extend BaseTest/ShopperBaseTest: needs the Customer app for
 * Phases 1 and 3 and the Shopper app for Phase 2, same reasoning as
 * SingleOrderComplete's own doc comment.
 *
 * NOTE: this pays with a staging virtual card (Customer app) and runs a
 * real staging transaction, same as SingleOrderComplete/MultipleOrderComplete.
 */
public class MultipleOrderSingleItemPriceIncreaseComplete {

    /** Two items bought; only the second one's price rises in-store. */
    private static final int ITEMS_PER_ORDER = 2;

    /** How much higher the shopper finds the second item for, as a fraction of its ordered price. */
    private static final BigDecimal PRICE_INCREASE_FACTOR = new BigDecimal("1.20");

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(45);
    private static final Duration LOCATION_ENTRY_WITHIN = Duration.ofSeconds(15);

    /** What Phase 1 places, and what Phase 3 needs to judge the outcome by. */
    private static final class Placement {

        final String orderId;
        final BigDecimal walletBefore;

        Placement(String orderId, BigDecimal walletBefore) {

            this.orderId = orderId;
            this.walletBefore = walletBefore;
        }
    }

    @Test(description = "Place one order with 2 items from the same store, then as the Shopper"
            + " accept the 1st item as ordered and raise the 2nd item's price before accepting"
            + " it — confirm both are approved, the run proceeds to Confirm payment, and the"
            + " customer's Wallet is unchanged (not charged for the increase)")
    public void singleItemPriceIncreaseDoesNotChargeTheCustomer() {

        Placement placement = phase1_placeMultiItemOrderAndNoteWallet();

        phase2_raiseOneItemsPriceFromShopperApp(placement.orderId);

        phase3_verifyWalletUnchanged(placement);
    }

    // ==================== Phase 1: Customer app — place a 2-item order, note the Wallet ====================

    private Placement phase1_placeMultiItemOrderAndNoteWallet() {

        step("PHASE 1: Customer app — place one order with " + ITEMS_PER_ORDER
                + " items from one store, note the Wallet balance");

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

            // ---------- Wallet balance, before anything can be credited or debited ----------

            homePage.openProfile();
            profilePage.waitForScreen();

            BigDecimal walletBefore = CurrencyUtils.parse(profilePage.getWalletTileText());
            step("Wallet before: " + walletBefore);

            profilePage.waitForScreen();
            homePage.openHomeTab();
            homePage.waitForScreen();

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

            return new Placement(normalizeOrderId(placedOrderId), walletBefore);

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 2: Shopper app — accept item 1, raise item 2's price ====================

    private void phase2_raiseOneItemsPriceFromShopperApp(String orderId) {

        step("PHASE 2: Shopper app — accept item 1 as ordered, raise item 2's price before"
                + " accepting it, for order " + orderId);

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

            // Item 1: Got it, exactly as ordered — no price edit.
            for (int itemNumber = 1; itemNumber <= ITEMS_PER_ORDER; itemNumber++) {

                Assert.assertFalse(orderValidator.isValidateAndProceedDisplayed(),
                        "\"Validate and proceed\" showed up after only " + (itemNumber - 1)
                                + " of " + ITEMS_PER_ORDER + " items were accounted for");

                Assert.assertTrue(orderValidator.isGotItButtonDisplayedNow(),
                        "\"GOT IT\" is not displayed for item " + itemNumber);

                step("Item " + itemNumber + "/" + ITEMS_PER_ORDER + " — Got it ("
                        + orderPage.getRunProgressCount() + ")");

                orderPage.clickGotIt();

                orderPage.waitForGotItSheet();
                Assert.assertTrue(orderValidator.isGotItSheetDisplayed(),
                        "The \"Got it\" item-details sheet did not open for item " + itemNumber);

                boolean isLastItem = itemNumber == ITEMS_PER_ORDER;

                if (isLastItem) {

                    Assert.assertTrue(orderValidator.isPriceEditIconDisplayed(),
                            "The Per Unit Price edit icon is not displayed on item " + itemNumber
                                    + "'s \"Got it\" sheet");

                    orderPage.openPriceEditor();

                    BigDecimal shownPrice = CurrencyUtils.parse(orderPage.getPerUnitPrice());
                    step("Item " + itemNumber + " Per Unit Price shown: " + shownPrice);

                    BigDecimal raisedPrice = shownPrice.multiply(PRICE_INCREASE_FACTOR)
                            .setScale(2, RoundingMode.HALF_UP);

                    step("Raising item " + itemNumber + "'s price to: " + raisedPrice);
                    orderPage.setPerUnitPrice(raisedPrice.toPlainString());
                }

                orderPage.confirmGotItSheet();

                orderPage.waitForAcceptItemSheet();
                Assert.assertTrue(orderValidator.isAcceptItemSheetDisplayed(),
                        "The \"Accept this item?\" sheet did not open for item " + itemNumber);

                orderPage.proceedAcceptingItem();

                Assert.assertTrue(orderValidator.isItemApproved(),
                        "Item " + itemNumber + " does not show \"Approved\" after accepting it");
            }

            step("Both items approved — run progress: " + orderPage.getRunProgressCount());

            Assert.assertTrue(orderValidator.isValidateAndProceedDisplayed(),
                    "\"Validate and proceed\" is not displayed once both items are approved");

            step("Validate and proceed");
            orderPage.validateAndProceed();

            orderPage.waitForConfirmPaymentSheet();
            Assert.assertTrue(orderValidator.isConfirmPaymentSheetDisplayed(),
                    "The \"Confirm payment\" sheet did not open — a price increase on one of "
                            + ITEMS_PER_ORDER + " items should still lead here for both items"
                            + " approved, not block the run");

            String amount = orderPage.getConfirmPaymentAmount();
            step("Confirm payment $" + amount);
            orderPage.confirmPayment(amount);

            step("Stopping here — same \"Capture the receipt\" gap as ItemPriceDecreaseComplete/"
                    + "ItemPriceIncreaseComplete/MultipleOrderPartialFulfillment.");

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 3: Customer app — the Wallet is unchanged ====================

    private void phase3_verifyWalletUnchanged(Placement placement) {

        step("PHASE 3: Customer app — confirm the Wallet was neither credited nor debited");

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

            } else {

                step("Already signed in from Phase 1 — skipping sign in");
            }

            homePage.waitForScreen().openProfile();
            profilePage.waitForScreen();

            BigDecimal walletAfter = CurrencyUtils.parse(profilePage.getWalletTileText());
            step("Wallet after: " + walletAfter + " (was " + placement.walletBefore + ")");

            // ---------- Final assertion: this is what makes the whole scenario pass
            // or fail — Phase 1 placing the order and Phase 2 raising one item's price
            // are only preconditions; the scenario only counts as the customer
            // genuinely not paying more once the Wallet is confirmed untouched by it. ----------
            step("Final assertion: Wallet is unchanged at " + placement.walletBefore);
            Assert.assertTrue(CurrencyUtils.equalAmounts(walletAfter, placement.walletBefore),
                    "Wallet reads " + walletAfter + " — expected it unchanged at "
                            + placement.walletBefore + " after order " + placement.orderId
                            + "'s second item was priced up — the customer should not be charged"
                            + " for an in-store price increase on just one item of several");

            step("SUCCESS: order " + placement.orderId + " had one of " + ITEMS_PER_ORDER
                    + " items priced up by the shopper, both items were still approved, and the"
                    + " customer's Wallet was left untouched — the increase was not passed on.");

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
