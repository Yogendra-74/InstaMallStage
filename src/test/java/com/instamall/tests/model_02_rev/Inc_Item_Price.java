package com.instamall.tests.model_02_rev;

import com.instamall.api.DeliveryWebhooks;
import com.instamall.api.OrderApi;
import com.instamall.components.consent.ConsentPrompts;
import com.instamall.components.navigation.HeaderLocators;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.config.ConfigReader;
import com.instamall.driver.DriverManager;
import com.instamall.factory.DriverFactory;
import com.instamall.flows.CheckoutFlow;
import com.instamall.pages.bag.ShoppingBagPage;
import com.instamall.pages.bag.ShoppingBagPageValidator;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.home.HomePageLocators;
import com.instamall.pages.home.HomePageValidator;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.location.SelectLocationSheet;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.login.LoginPageLocators;
import com.instamall.pages.malls.MallDetailPage;
import com.instamall.pages.orders.OrderConfirmationPage;
import com.instamall.pages.orders.OrdersListPage;
import com.instamall.pages.product.ProductDetailPage;
import com.instamall.pages.product.ProductDetailPageValidator;
import com.instamall.pages.profile.ProfilePage;
import com.instamall.pages.shopper.home.ShopperHomePage;
import com.instamall.pages.shopper.home.ShopperHomePageLocators;
import com.instamall.pages.shopper.home.ShopperShift;
import com.instamall.pages.shopper.home.ShopperShift.ShiftStatus;
import com.instamall.pages.shopper.login.ShopperLoginPage;
import com.instamall.pages.shopper.login.ShopperLoginPageLocators;
import com.instamall.pages.shopper.orders.OrderDetailPage;
import com.instamall.pages.shopper.orders.OrderDetailPageLocators;
import com.instamall.pages.shopper.orders.OrderDetailPageValidator;
import com.instamall.pages.store.StorePage;
import com.instamall.support.PlacedOrder;
import com.instamall.waits.WaitUtils;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Increased-price variant of the full cross-app order lifecycle (see
 * {@link Single_Item_Order}) — place -&gt; accept -&gt; dispatch -&gt;
 * handoff -&gt; verify delivered — the one difference being that in Phase
 * 2 the shopper increases the item's unit price on the "Got it" sheet
 * before accepting it.
 *
 * The price edit: the shopper taps the pencil beside "Per Unit Price",
 * reads the written price, enters a 20% increase on it as the new price,
 * then taps GOT IT. Everything else is the same as
 * {@link Single_Item_Order}, down to how the phases are wired and how
 * the order id reaches the Delivery Solutions and Customer-app phases.
 *
 * Each phase is its own TestNG test case and depends on no other — the
 * {@code priority} only fixes their order when the class is run whole,
 * and {@link #stopAfterFailure} skips every later phase once one has
 * failed. What a later phase needs from Phase 1 (the order id, and the
 * full {@code orderExternalId} Delivery Solutions knows the order by) is
 * handed over through {@link PlacedOrder}: in memory when run together,
 * otherwise from {@code -DorderId=…}/{@code -DorderExternalId=…} or the
 * last recorded Phase 1. The Shopper app phases act on the first order in
 * the queue and never read the id.
 */
public class Inc_Item_Price {

    // The first phase of this run to fail, if any — set by noteOutcome()
    // after each phase, checked by stopAfterFailure() before each.
    private String failedPhase;

    // The order this run is working on. Set by Phase 1; every later phase
    // resolves it through PlacedOrder.resolve() first thing.
    private String orderId;

    // The same order as Delivery Solutions knows it — the full
    // orderExternalId its webhooks carry, which Phases 3a and 3b address
    // their calls to. Not the short id the Customer app shows.
    private String orderExternalId;

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(90);

    /**
     * How long the Notifications and Location alerts get to turn up after
     * a sign-in, rather than a fixed number of passes.
     */
    private static final long PERMISSION_WAIT_MILLIS = 30_000;

    // Paused between trials so a still-settling sign-in round trip gets a
    // moment to actually raise the next alert.
    private static final long PERMISSION_TRIAL_PAUSE_MILLIS = 1000;

    // ---------- PHASE 0 constants ----------

    private static final String WDA_BUNDLE_ID = "com.facebook.WebDriverAgentRunner.xctrunner";

    // Where Appium's own XCUITest driver keeps its bundled copy of
    // WebDriverAgent — confirmed live on this machine.
    private static final String WDA_PROJECT_DIR = System.getProperty("user.home")
            + "/node_modules/appium-xcuitest-driver/node_modules/appium-webdriveragent";

    // The line WebDriverAgentRunner prints once its HTTP server is ready.
    private static final String WDA_READY_MARKER = "ServerURLHere->";

    // Same headroom DriverFactory gives a fresh WebDriverAgent build.
    private static final Duration WDA_BUILD_TIMEOUT = Duration.ofMinutes(4);

    // ==================== Stop after the first failure ====================

    @BeforeMethod(alwaysRun = true)
    public void stopAfterFailure(java.lang.reflect.Method phase) {

        if (failedPhase != null) {

            throw new SkipException("Skipped " + phase.getName()
                    + " because an earlier phase of this run failed: " + failedPhase);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void noteOutcome(ITestResult result) {

        if (result.getStatus() == ITestResult.FAILURE && failedPhase == null) {

            failedPhase = result.getMethod().getMethodName();
        }
    }

    // ==================== Phase 0: Simulator preflight ====================

    @BeforeClass
    public void prepareSimulator() {

        step("PHASE 0: Simulator preflight — WebDriverAgent, grant location before first install");

        String udid = resolveBootedSimulatorUdid();

        ensureWebDriverAgentInstalled(udid);

        // Location is granted here, before the Customer app is ever
        // installed — TCC's grant is keyed by bundle identifier alone, so
        // it does not need the app on disk first.
        step("Granting location access to the Customer app before it is ever installed");
        runCommand(List.of("xcrun", "simctl", "privacy", udid, "grant", "location",
                ConfigReader.get("bundleId")), null);
    }

    private String resolveBootedSimulatorUdid() {

        String output = runCommand(List.of("xcrun", "simctl", "list", "devices"), null);

        Matcher matcher = Pattern.compile("\\(([0-9A-Fa-f-]{36})\\)\\s*\\(Booted\\)").matcher(output);

        Assert.assertTrue(matcher.find(), "No booted iOS Simulator found — boot one before running this test");

        return matcher.group(1);
    }

    private boolean isWebDriverAgentInstalled(String udid) {

        return runCommand(List.of("xcrun", "simctl", "listapps", udid), null).contains(WDA_BUNDLE_ID);
    }

    private void ensureWebDriverAgentInstalled(String udid) {

        if (isWebDriverAgentInstalled(udid)) {

            step("WebDriverAgent already installed on " + udid + " — skipping install");
            return;
        }

        step("WebDriverAgent not installed on " + udid + " — building and installing"
                + " (runs in the background, can take a few minutes on a first build)");

        try {

            File wdaProjectDir = new File(WDA_PROJECT_DIR);

            Assert.assertTrue(wdaProjectDir.isDirectory(),
                    "WebDriverAgent project not found at " + WDA_PROJECT_DIR);

            File buildLog = File.createTempFile("wda-build-", ".log");

            ProcessBuilder builder = new ProcessBuilder(
                    "xcodebuild",
                    "-project", "WebDriverAgent.xcodeproj",
                    "-scheme", "WebDriverAgentRunner",
                    "-destination", "id=" + udid,
                    "IPHONEOS_DEPLOYMENT_TARGET=15.0",
                    "GCC_TREAT_WARNINGS_AS_ERRORS=NO",
                    "CLANG_WARN_RESERVED_IDENTIFIER=NO",
                    "test");

            builder.directory(wdaProjectDir);
            builder.redirectErrorStream(true);
            builder.redirectOutput(buildLog);

            builder.start();

            boolean ready = awaitUntil(() -> logContains(buildLog, WDA_READY_MARKER),
                    WDA_BUILD_TIMEOUT, "WebDriverAgent to report its server URL");

            Assert.assertTrue(ready, "WebDriverAgent did not finish building/starting within "
                    + WDA_BUILD_TIMEOUT.toSeconds() + "s — see " + buildLog.getAbsolutePath());

            step("WebDriverAgent is up (build log: " + buildLog.getAbsolutePath() + ")");

        } catch (IOException e) {

            throw new RuntimeException("Unable to build/install WebDriverAgent", e);
        }
    }

    private boolean logContains(File file, String marker) {

        try {

            return Files.readString(file.toPath()).contains(marker);

        } catch (IOException notWrittenYet) {

            return false;
        }
    }

    private String runCommand(List<String> command, File workingDir) {

        try {

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            if (workingDir != null) {

                builder.directory(workingDir);
            }

            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {

                System.out.println(">>> " + String.join(" ", command) + " exited " + exitCode + ": " + output);
            }

            return output;

        } catch (IOException | InterruptedException e) {

            if (e instanceof InterruptedException) {

                Thread.currentThread().interrupt();
            }

            throw new RuntimeException("Failed to run: " + String.join(" ", command), e);
        }
    }

    // ==================== Phase 1: Customer app — place an order ====================

    @Test(priority = 1, description = "Phase 1: Customer app — place an order")
    public void phase1_placeOrderFromCustomerApp() {

        step("PHASE 1: Customer app — place an order");

        freshInstall(ConfigReader.get("bundleId"), "Customer");

        IOSDriver driver = DriverFactory.initializeDriver(false, ConfigReader.get("appPath"));
        DriverManager.setDriver(driver);

        try {

            runCommand(List.of("xcrun", "simctl", "location", "booted", "set",
                    MIAMI_LATITUDE + "," + MIAMI_LONGITUDE), null);

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
            SelectLocationSheet locationSheet = new SelectLocationSheet();

            String email = ConfigReader.get("testEmail");
            String password = ConfigReader.get("testPassword");
            String addressName = ConfigReader.get("deliveryAddressName");
            String mallName = ConfigReader.get("mallName");
            String storeName = ConfigReader.get("storeName");
            String cardLast4 = ConfigReader.get("savedCardLast4");

            // ---------- Step 1: Activity Tracking ----------

            step("Step 1: Allow the Activity Tracking permission alert");
            try {

                if (SystemAlerts.allowTracking()) {

                    step("Activity Tracking alert — tapped \"Allow\"");

                } else {

                    step("Activity Tracking alert did not show — nothing to allow");
                }

            } catch (Exception trackingAlertFailed) {

                System.out.println(">>> Activity Tracking alert could not be answered ("
                        + trackingAlertFailed.getMessage() + ") — carrying on.");
            }

            // ---------- Step 2: on to the email sign-in screen ----------

            boolean firstScreenShown = awaitUntil(() -> {

                        SystemAlerts.grantAnyShowingNow();
                        backOutTowardsHome();

                        return WaitUtils.isVisibleNow(LoginPageLocators.CONTINUE_WITH_EMAIL)
                                || homeValidator.isHomeTabDisplayedNow();
                    },
                    APP_FIRST_SCREEN_WITHIN, "the sign-in landing screen or Home");

            Assert.assertTrue(firstScreenShown,
                    "Neither the sign-in landing screen nor Home showed after launch");

            if (WaitUtils.isVisibleNow(LoginPageLocators.CONTINUE_WITH_EMAIL)) {

                step("Step 2: Click on \"Continue with Email\"");
                loginPage.clickContinueWithEmail();

                emailLoginPage.waitForScreen();
                step("Sign in as " + email);

                emailLoginPage.enterEmail(email).enterPassword(password).submitCredentials();

                step("Step 3: Grant Notification and Location permissions"
                        + " (order not guaranteed)");
                grantPermissions(CUSTOMER_SIGN_IN_PERMISSIONS);

                ConsentPrompts.acceptAfterSignIn(HomePageLocators.TAB_HOME);

                homePage.waitForScreen();
                step("Home screen");

            } else {

                step("A session is already active — skipping sign-in,"
                        + " going straight to the Home tab");
                homePage.openHomeTab().waitForScreen();
                step("Home screen");
            }

            // ---------- Delivery location ----------

            step("Step 1: Tap the \"Deliver to\" location on the top left");
            homePage.openLocationSelector();
            locationSheet.waitForScreen();

            step("Step 2: Tap \"" + addressName + "\" under Saved addresses");
            locationSheet.chooseSavedAddress(addressName);
            WaitUtils.waitForVisibility(HomePageLocators.selectedLocation(addressName));
            Assert.assertTrue(homeValidator.isLocationSelected(addressName),
                    "The header does not read \"Deliver to " + addressName + "\" after choosing it");

            // ---------- Store and product ----------

            step("Explore Malls -> " + mallName + " -> " + storeName);
            homePage.openMallCard(mallName);

            mallPage.waitForScreen().openStore(storeName);

            String brand = storePage.waitForScreen().getBrandName();
            step("Store: " + brand);

            List<String> tiles = storePage.listVisibleProductTiles();
            Assert.assertFalse(tiles.isEmpty(), "No product tiles on the " + brand + " grid");

            String tile = tiles.get(0);
            String productName = tileNameWithoutBrandAndPrice(tile, brand);

            storePage.openProductTile(tile);
            productPage.waitForScreen();

            Assert.assertTrue(productValidator.isSizeCardDisplayed(),
                    "\"" + productName + "\" has no size card — nothing to buy on this product");

            String color = colorFrom(productPage.getColorCardText());
            String chosenSize;

            if (!productValidator.isSizeCardEnabled()) {

                chosenSize = productPage.getSelectedSize();

                Assert.assertFalse(chosenSize.isEmpty(),
                        "\"" + productName + "\" offers no size at all");

                step("Step 23: Click on \"Add to bag\"");
                productPage.addToBagIfNeeded();

            } else {

                List<String> sizes = productPage.openSizeSheet().getAvailableSizes();

                Assert.assertFalse(sizes.isEmpty(),
                        "\"" + productName + "\" is out of stock in every size");

                int middleIndex = sizes.size() % 2 == 1
                        ? sizes.size() / 2
                        : sizes.size() / 2 - 1;

                chosenSize = sizes.get(middleIndex);

                step("Step 22: Select the mid size");
                productPage.chooseSize(chosenSize);

                step("Step 23: Click on \"Add to bag\" again");
                productPage.addToBagIfNeeded();
            }

            Assert.assertTrue(productValidator.isViewBagDisplayed(),
                    "No \"View bag\" pill after adding to bag — the item was not added");

            step("Step 24: Click on \"View bag\"");
            productPage.clickViewBag();

            bagPage.waitForScreen();
            bagPage.waitForItem(productName, chosenSize);

            Assert.assertTrue(bagValidator.isStoreGroupDisplayed(brand), "Bag is missing the " + brand + " group");
            Assert.assertTrue(bagPage.hasItem(productName, chosenSize),
                    "Bag has no row for \"" + productName + "\" in " + chosenSize);

            // ---------- Checkout, payment, confirmation ----------

            CheckoutFlow checkout = new CheckoutFlow(this::step);
            checkout.openCheckoutFromBag(productName, chosenSize);

            CheckoutFlow.Placed placed = checkout.placeOrder(addressName);
            String placedOrderId = checkout.payAndVerifyOrder(
                    placed, cardLast4, brand, productName, chosenSize, color);

            step("Order placed: " + placedOrderId);

            step("Tap the copy button beside the order number");
            new OrderConfirmationPage().copyOrderId();

            String clipboard = driver.getClipboardText();
            step("Clipboard after copy: " + (clipboard == null || clipboard.isEmpty()
                    ? "(empty — nothing was copied)"
                    : "\"" + clipboard + "\""));

            this.orderId = normalizeOrderId(placedOrderId);

            String copiedId = clipboard != null ? clipboard.trim() : null;

            if (copiedId != null && !copiedId.isEmpty()) {

                this.orderExternalId = copiedId;
                step("Delivery Solutions order id from clipboard: " + copiedId);

            } else {

                step("Clipboard was empty — falling back to the API lookup");
                this.orderExternalId = fetchExternalOrderId(this.orderId);
            }

            PlacedOrder.record(this.orderId, this.orderExternalId);

            try {

                Thread.sleep(HOME_SETTLE_BEFORE_CLOSE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
            }

            closeApp(driver, ConfigReader.get("bundleId"), "Customer");

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 2: Shopper app — edit the price and accept the order ====================

    @Test(priority = 2, description = "Phase 2: Shopper app — edit the price and accept the first order")
    public void phase2_editPriceAndAcceptOrderFromShopperApp() {

        // No order id here on purpose: the Shopper app acts on the first
        // order in the queue and never reads or compares the id.
        step("PHASE 2: Shopper app — edit the price and accept the first order in the queue");

        freshInstall(ConfigReader.get("shopperBundleId"), "Shopper");

        IOSDriver driver = DriverFactory.initializeDriver(
                false, ConfigReader.get("shopperAppPath"), List.of("-mockDocumentScan"));
        DriverManager.setDriver(driver);

        try {

            awaitShopperFirstScreen();

            ShopperLoginPage loginPage = new ShopperLoginPage();
            ShopperHomePage homePage = new ShopperHomePage();
            ShopperShift shift = new ShopperShift();
            OrderDetailPage orderPage = new OrderDetailPage();
            OrderDetailPageValidator orderValidator = new OrderDetailPageValidator();

            if (WaitUtils.isVisibleNow(ShopperLoginPageLocators.LOG_IN_BUTTON)) {

                step("Log in as " + ConfigReader.get("shopperEmail"));
                loginPage.waitForScreen()
                         .enterEmail(ConfigReader.get("shopperEmail"))
                         .enterPassword(ConfigReader.get("shopperPassword"))
                         .tapLogIn();

                step("Allow the permission cards that follow the Log in tap");
                grantPermissions(SHOPPER_SIGN_IN_PERMISSIONS);

            } else {

                step("Already signed in from an earlier run — skipping sign in and permissions");
            }

            step("Steps 35-36: Go on shift, however the app launched");
            ShiftStatus shiftStatus = shift.ensureOnShift();

            homePage.waitForScreen();
            step("On Home");

            if (shiftStatus == ShiftStatus.IN_ORDER) {

                step("Step 37: Status is IN ORDER — click on \"IN PROGRESS\" on the first order listing");
                homePage.tapInProgressOnFirstCard();

            } else if (shiftStatus == ShiftStatus.ON_SHIFT) {

                step("Step 37: Status is ON SHIFT — click on \"VIEW ORDER\" on the first order listing");
                homePage.tapViewOrderOnFirstCard();
            }

            orderPage.waitForScreen();

            step("Order detail open");

            Assert.assertTrue(orderValidator.isDisplayed(),
                    "Order detail screen did not open from the Home card");

            // Steps 38-43: every item in the run gets its unit price bumped
            // 20% and accepted. A run can hold more than one item, and
            // "Validate and proceed" only comes ready once ALL of them have
            // been accepted — so the price edit and acceptance are looped
            // over the whole run (read off the RUN PROGRESS count) before
            // the run is validated.
            int itemCount = parseRunItemCount(orderPage.getRunProgressCount());
            Assert.assertTrue(itemCount > 0, "The run reports no items to accept");
            step("The run holds " + itemCount + " item(s) — applying a 20%"
                    + " price increase to each before validating");

            for (int itemNumber = 1; itemNumber <= itemCount; itemNumber++) {

                step("---- Item " + itemNumber + " of " + itemCount + " ----");

                // Later items sit below the fold once the ones above them
                // are approved — bring this one's GOT IT into view first.
                orderPage.scrollToGotItButton();

                // Step 38: tap GOT IT on the item, which opens the "Got it"
                // item-details sheet.
                Assert.assertTrue(orderValidator.isGotItButtonDisplayedNow(),
                        "GOT IT is not on the order detail for item " + itemNumber
                                + " of " + itemCount + " — it may already be accepted");

                step("Step 38: Click on \"GOT IT\"");
                orderPage.clickGotIt();

                orderPage.waitForGotItSheet();
                Assert.assertTrue(orderValidator.isGotItSheetDisplayed(),
                        "The \"Got it\" item-details sheet did not open after tapping GOT IT"
                                + " for item " + itemNumber + " of " + itemCount);

                // ---------- The increased-price edit ----------

                // Step 39: on the "Got it" sheet, tap the pencil beside the
                // "Per Unit Price" to open the price field (the keyboard
                // comes up over the sheet).
                step("Step 39: Click on the \"Pencil\" icon to edit the Per Unit Price");
                orderPage.clickEditPricePencil();

                // Step 40: fetch the price currently written in the field.
                double writtenPrice = orderPage.getEditablePerUnitPrice();
                step("Step 40: Fetched the written Per Unit Price: " + formatPrice(writtenPrice));

                // Step 41: enter a 20%-increased price as the new price.
                double increasedPrice = writtenPrice * 1.20;
                String newPrice = formatPrice(increasedPrice);
                step("Step 41: Enter the new price " + newPrice
                        + " (20% increase on " + formatPrice(writtenPrice) + ")");
                orderPage.setPerUnitPrice(newPrice);

                // Step 42: tap GOT IT on the sheet to accept the edited item
                // — this opens the "Accept this item?" sheet.
                step("Step 42: Click on \"GOT IT\"");
                orderPage.confirmGotItSheet();

                orderPage.waitForAcceptItemSheet();
                Assert.assertTrue(orderValidator.isAcceptItemSheetDisplayed(),
                        "The \"Accept this item?\" sheet did not open after tapping GOT IT"
                                + " for item " + itemNumber + " of " + itemCount);

                // Step 43: Proceed on "Accept this item?". A loading overlay
                // drops while the backend records the acceptance; once it
                // clears the item shows "Approved" and the RUN PROGRESS
                // count advances — carrying the increased price through.
                step("Step 43: Click on \"Proceed\" on \"Accept this item?\"");
                orderPage.proceedAcceptingItem();

                step("Wait out the loading until the run shows " + itemNumber
                        + " of " + itemCount + " done");
                orderPage.waitForRunProgress(itemNumber, itemCount);
            }

            // Every item is accepted — only now is "Validate and proceed"
            // ready. This was the failure point with more than one item in
            // the bag: it was clicked after the first item instead of all.
            step("All " + itemCount + " item(s) accepted at a 20% increase"
                    + " — waiting for \"Validate and proceed\" to be ready");
            orderPage.waitForValidateAndProceedReady();

            // ---------- Back to the usual accept-through-receipt flow ----------

            step("Step 44: Click on \"Validate and proceed\"");
            orderPage.validateAndProceed();

            orderPage.waitForConfirmPaymentSheet();
            Assert.assertTrue(orderValidator.isConfirmPaymentSheetDisplayed(),
                    "The \"Confirm payment\" sheet did not open");

            step("Step 45: Confirm payment");
            orderPage.confirmPayment();

            // Step 46: the camera permission alert comes up the moment
            // Confirm payment is tapped; iOS asks only once per install, so
            // on a re-run there is nothing to answer.
            step("Step 46: Click on \"Allow\" for the camera permission");
            try {

                if (SystemAlerts.allowCamera()) {

                    step("Camera permission alert — tapped \"Allow\"");

                } else {

                    step("Camera permission alert did not show — nothing to allow");
                }

            } catch (Exception cameraAlertFailed) {

                System.out.println(">>> Camera permission alert could not be answered ("
                        + cameraAlertFailed.getMessage() + ") — carrying on.");
            }

            orderPage.waitForReceiptCaptureScreen();
            Assert.assertTrue(orderValidator.isReceiptCaptureScreenDisplayed(),
                    "\"Capture the receipt\" did not open after confirming payment");

            // Step 47: the Simulator has no camera, so the "DEV · Sample
            // receipt" bypass is used instead.
            step("Step 47: Simulator has no camera — click on \"DEV · Sample receipt\"");
            orderPage.useSampleReceipt();
            step("\"Looks good?\" review screen is up");

            step("Step 48: Click on \"USE PHOTO\"");
            orderPage.usePhoto();

            step("Step 49: \"Submit receipt?\" -> Proceed");
            orderPage.submitReceipt();

            step("Step 50: \"Order processed\" -> OK");
            orderPage.dismissReceiptProcessed();

            homePage.waitForScreen();
            Assert.assertTrue(WaitUtils.isVisible(ShopperHomePageLocators.WAITING_FOR_DRIVER_CARD),
                    "After the receipt was submitted, Home does not show the order as WAITING FOR DRIVER");

            step("Receipt submitted after increasing the price 20% on all "
                    + itemCount + " item(s) — the order is back on Home as"
                    + " WAITING FOR DRIVER, ready for Phase 3a to assign a driver"
                    + " through Delivery Solutions");

            // A flat 2s on Home, then leave the Shopper app on screen — it
            // is NOT closed here. Phase 3a is HTTP-only, so the app stays
            // foreground on Home, and Phase 4 relaunches it from there.
            try {

                Thread.sleep(HOME_SETTLE_BEFORE_CLOSE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
            }

            step("Wait 5s after Phase 2 before Phase 3a");
            try {

                Thread.sleep(POST_PHASE2_SETTLE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
            }

        } finally {

            DriverManager.quitDriver();
        }
    }

    // How long to wait after Phase 2 completes before Phase 3a runs.
    private static final long POST_PHASE2_SETTLE_MILLIS = 5000;

    // ==================== Phase 3a: Delivery Solutions — driver assigned through at-pickup ====================

    @Test(priority = 3, description = "Phase 3a: Delivery Solutions — Driver Assigned, Pickup Started, Driver At Pickup")
    public void phase3a_driverAssignedThroughAtPickup() {

        orderId = PlacedOrder.resolve(orderId);
        orderExternalId = PlacedOrder.resolveExternalId(orderExternalId);

        step("PHASE 3a: Delivery Solutions — Driver Assigned through Driver At Pickup for order "
                + PlacedOrder.describe(orderId));

        PlacedOrder.requireExternalId(orderExternalId, "Phase 3a");

        step("Driver Assigned");
        DeliveryWebhooks.driverAssigned(orderExternalId);

        step("Pickup Started");
        DeliveryWebhooks.pickupStarted(orderExternalId);

        step("Driver At Pickup");
        DeliveryWebhooks.driverAtPickup(orderExternalId);

        step("Phase 3a complete — the courier is at the store; Phase 4 hands off in the Shopper app");
    }

    // ==================== Phase 4: Shopper app again — start handoff ====================

    @Test(priority = 4, description = "Phase 4: Shopper app — start handoff")
    public void phase4_completeHandoffFromShopperApp() {

        step("PHASE 4: Shopper app — start handoff");

        IOSDriver driver = DriverFactory.initializeDriver(
                false, ConfigReader.get("shopperAppPath"), List.of("-mockDocumentScan"));
        DriverManager.setDriver(driver);

        try {

            ShopperHomePage homePage = new ShopperHomePage();
            OrderDetailPage orderPage = new OrderDetailPage();
            OrderDetailPageValidator orderValidator = new OrderDetailPageValidator();

            step("Shopper Home (signed in since Phase 2)");
            Assert.assertTrue(awaitUntil(ShopperHomePage::isDisplayedNow, APP_FIRST_SCREEN_WITHIN,
                            "the Shopper Home screen"),
                    "The Shopper app did not open on Home — was Phase 2 run on this simulator?");
            homePage.waitForScreen();

            step("Start handoff");
            homePage.tapStartHandoff();

            orderPage.waitForStartHandoffSheet();
            Assert.assertTrue(orderValidator.isStartHandoffSheetDisplayed(),
                    "The \"Start handoff?\" sheet did not open");

            orderPage.proceedStartHandoff();

            orderPage.waitForHandoffPhotoScreen();
            Assert.assertTrue(orderValidator.isHandoffPhotoScreenDisplayed(),
                    "\"Capture handoff photo\" did not open after proceeding on \"Start handoff?\"");

            step("Tap \"DEV · Sample handoff photo\"");
            WaitUtils.waitForClickable(OrderDetailPageLocators.DEV_SAMPLE_HANDOFF_PHOTO).click();

            step("\"Looks good?\" -> USE PHOTO (flaky tap — retried up to twice)");
            orderPage.useHandoffPhoto();

            step("\"Complete handoff?\" -> Proceed");
            WaitUtils.waitForClickable(OrderDetailPageLocators.COMPLETE_HANDOFF_PROCEED).click();

            step("\"Handoff complete\" -> OK");
            WaitUtils.waitForClickable(OrderDetailPageLocators.HANDOFF_COMPLETE_OK).click();

            // Per instruction: the moment the handoff's OK is tapped, the
            // Shopper app is minimised — sent to the background, not
            // terminated — and the phases that follow (Phase 3b's Delivery
            // Solutions webhooks, then Phase 5's Customer app) run with it
            // left minimised rather than closed.
            minimizeApp(driver, "Shopper");

            step("Wait 5s after Phase 4 before Phase 3b");
            try {

                Thread.sleep(POST_PHASE4_SETTLE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
            }

        } finally {

            DriverManager.quitDriver();
        }
    }

    // How long to wait after Phase 4 completes before Phase 3b runs.
    private static final long POST_PHASE4_SETTLE_MILLIS = 5000;

    // ==================== Phase 3b: Delivery Solutions — pickup completed through delivered ====================

    @Test(priority = 5, description = "Phase 3b: Delivery Solutions — Pickup Completed, Out For Delivery, Delivered")
    public void phase3b_pickupCompletedThroughDelivered() {

        orderId = PlacedOrder.resolve(orderId);
        orderExternalId = PlacedOrder.resolveExternalId(orderExternalId);

        step("PHASE 3b: Delivery Solutions — Pickup Completed through Delivered for order "
                + PlacedOrder.describe(orderId));

        PlacedOrder.requireExternalId(orderExternalId, "Phase 3b");

        step("Pickup Completed");
        DeliveryWebhooks.pickupCompleted(orderExternalId);

        step("Out For Delivery");
        DeliveryWebhooks.outForDelivery(orderExternalId);

        step("Delivered");
        DeliveryWebhooks.delivered(orderExternalId);

        step("Phase 3b complete — the order is Delivered; Phase 5 confirms it in the Customer app");
    }

    // ==================== Phase 5: Customer app — verify the order shows in Account > Orders ====================

    @Test(priority = 6, description = "Phase 5: Customer app — verify the order shows in Account > Orders")
    public void phase5_verifyOrderInAccountHistory() {

        orderId = PlacedOrder.resolve(orderId);
        step("PHASE 5: Customer app — verify order " + PlacedOrder.describe(orderId)
                + " is listed in Account > Orders");

        IOSDriver driver = DriverFactory.initializeDriver(false, ConfigReader.get("appPath"));
        DriverManager.setDriver(driver);

        try {

            HomePage homePage = new HomePage();
            HomePageValidator homeValidator = new HomePageValidator();
            ProfilePage profilePage = new ProfilePage();
            OrdersListPage ordersListPage = new OrdersListPage();

            step("Customer Home (signed in since Phase 1)");
            Assert.assertTrue(awaitUntil(() -> {

                        backOutTowardsHome();

                        return homeValidator.isHomeTabDisplayedNow();
                    },
                    APP_FIRST_SCREEN_WITHIN, "the Customer Home screen"),
                    "The Customer app did not open on Home — was Phase 1 run on this simulator?");

            homePage.openHomeTab().waitForScreen().openProfile();
            profilePage.waitForScreen();

            step("Account > Orders");
            profilePage.openOrders();

            ordersListPage.waitForScreen();

            PlacedOrder.require(orderId, "Finding the order in Account > Orders");

            boolean listed = ordersListPage.isOrderListed(orderId);

            step(listed
                    ? "Found: " + ordersListPage.getOrderRowText(orderId)
                    : "Order " + orderId + " is not listed");

            Assert.assertTrue(listed, "Order " + orderId + " was not found in Account > Orders");

            step("Final assertion: the status pill reads \"Delivered\"");
            Assert.assertTrue(ordersListPage.hasStatus(orderId, "Delivered"),
                    "Order " + orderId + " was not successfully delivered — Account > Orders"
                            + " shows \"" + ordersListPage.getOrderRowText(orderId) + "\" instead of \"Delivered\"");

            step("SUCCESS: order " + orderId + " was placed, had its price increased 20% by the"
                    + " shopper, accepted, dispatched through the Delivery Solutions webhooks,"
                    + " handed off, and confirmed Delivered end to end.");

            closeApp(driver, ConfigReader.get("bundleId"), "Customer");

        } finally {

            DriverManager.quitDriver();
        }
    }

    @AfterClass(alwaysRun = true)
    public void quitAnyRemainingDrivers() {

        DriverManager.quitDriver();
    }

    // ==================== Shared helpers ====================

    private record Permission(String name, java.util.function.BooleanSupplier grant) {

    }

    private static final List<Permission> SHOPPER_SIGN_IN_PERMISSIONS = List.of(
            new Permission("Notifications", SystemAlerts::grantNotificationIfShowingNow),
            new Permission("Location", SystemAlerts::grantLocationIfShowingNow),
            new Permission("Activity Tracking", SystemAlerts::grantTrackingIfShowingNow));

    private static final List<Permission> CUSTOMER_SIGN_IN_PERMISSIONS = List.of(
            new Permission("Notifications", SystemAlerts::grantNotificationIfShowingNow),
            new Permission("Location", SystemAlerts::grantLocationIfShowingNow));

    private void grantPermissions(List<Permission> permissions) {

        boolean[] granted = new boolean[permissions.size()];
        int outstanding = permissions.size();

        long deadline = System.currentTimeMillis() + PERMISSION_WAIT_MILLIS;

        while (outstanding > 0 && System.currentTimeMillis() < deadline) {

            for (int index = 0; index < permissions.size(); index++) {

                if (granted[index]) {

                    continue;
                }

                Permission permission = permissions.get(index);

                try {

                    if (permission.grant().getAsBoolean()) {

                        granted[index] = true;
                        outstanding--;
                        step(permission.name() + " card — allowed");
                    }

                } catch (Exception cardFailed) {

                    System.out.println(">>> " + permission.name() + " card could not be answered ("
                            + cardFailed.getMessage() + ") — trying again on the next pass.");
                }
            }

            if (outstanding == 0) {

                return;
            }

            try {

                Thread.sleep(PERMISSION_TRIAL_PAUSE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
                return;
            }
        }

        for (int index = 0; index < permissions.size(); index++) {

            if (!granted[index]) {

                step(permissions.get(index).name() + " card did not show — nothing to allow");
            }
        }
    }

    private String fetchExternalOrderId(String appOrderId) {

        String pinned = PlacedOrder.resolveExternalId(null);

        if (pinned != null) {

            step("Delivery Solutions order id pinned for " + appOrderId + ": " + pinned);

            return pinned;
        }

        String looked = OrderApi.findExternalOrderId(appOrderId).orElse(null);

        if (looked != null) {

            step("Delivery Solutions order id for " + appOrderId + ": " + looked);

        } else {

            step("No Delivery Solutions order id for " + appOrderId
                    + " — Phases 3a and 3b will stop and say so. The lookup's own output above says why.");
        }

        return looked;
    }

    // How long a phase stays on its last screen before closing/leaving the app.
    private static final long HOME_SETTLE_BEFORE_CLOSE_MILLIS = 2000;

    private void awaitShopperFirstScreen() {

        awaitUntil(() -> {

                    SystemAlerts.grantAnyShowingNow();

                    return WaitUtils.isVisibleNow(ShopperLoginPageLocators.LOG_IN_BUTTON)
                            || WaitUtils.isVisibleNow(ShopperHomePageLocators.GO_ON_SHIFT_BUTTON)
                            || ShopperHomePage.isDisplayedNow();

                }, APP_FIRST_SCREEN_WITHIN,
                "the Shopper app's first screen (login, dashboard or \"Go on shift?\")");
    }

    private void closeApp(IOSDriver driver, String bundleId, String appName) {

        step("Close the " + appName + " app");

        try {

            driver.terminateApp(bundleId);

        } catch (Exception closeFailed) {

            System.out.println(">>> The " + appName + " app could not be closed ("
                    + closeFailed.getMessage() + ") — carrying on.");
        }
    }

    /**
     * Minimises the app — the hardware Home press — so it is sent to the
     * background and left running rather than terminated. Used at the end
     * of Phase 4: the handoff is done, nothing after it drives the Shopper
     * app, and the app is left minimised instead of closed.
     */
    private void minimizeApp(IOSDriver driver, String appName) {

        step("Minimise the " + appName + " app");

        try {

            driver.executeScript("mobile: pressButton", Map.of("name", "home"));

        } catch (Exception minimizeFailed) {

            System.out.println(">>> The " + appName + " app could not be minimised ("
                    + minimizeFailed.getMessage() + ") — carrying on.");
        }
    }

    private void freshInstall(String bundleId, String appName) {

        String udid = resolveBootedSimulatorUdid();

        step("Fresh install: removing the " + appName + " app (" + bundleId
                + ") so this phase installs and signs in clean");

        runCommand(List.of("xcrun", "simctl", "uninstall", udid, bundleId), null);
        runCommand(List.of("xcrun", "simctl", "privacy", udid, "grant", "location", bundleId), null);
    }

    private void backOutTowardsHome() {

        if (!WaitUtils.isVisibleNow(HomePageLocators.TAB_HOME)
                && WaitUtils.isVisibleNow(HeaderLocators.BACK_CHEVRON)) {

            WaitUtils.waitForClickable(HeaderLocators.BACK_CHEVRON).click();
        }
    }

    private boolean awaitUntil(java.util.function.BooleanSupplier condition, Duration timeout, String description) {

        try {

            return WaitUtils.waitUntil(driver -> condition.getAsBoolean() ? Boolean.TRUE : null, timeout);

        } catch (TimeoutException neverHappened) {

            System.out.println(">>> Timed out waiting for " + description);
            return false;
        }
    }

    private String normalizeOrderId(String orderId) {

        return orderId.replaceFirst("^#", "").trim();
    }

    /**
     * Reads the number of items in the run off the RUN PROGRESS count,
     * e.g. "0 of 3 done" -&gt; 3. This is how the Phase 2 loop knows how
     * many items to apply the 20% increase to before validating.
     */
    private int parseRunItemCount(String progressText) {

        Matcher matcher = Pattern.compile("of\\s+(\\d+)\\s+done").matcher(progressText);

        Assert.assertTrue(matcher.find(),
                "Could not read the item count from the run progress \"" + progressText + "\"");

        return Integer.parseInt(matcher.group(1));
    }

    private String tileNameWithoutBrandAndPrice(String tileName, String brand) {

        int brandAt = tileName.lastIndexOf(" " + brand + " $");

        return brandAt < 0 ? tileName : tileName.substring(0, brandAt);
    }

    private String colorFrom(String colorCardText) {

        return colorCardText.replaceFirst("^Color\\s*·?\\s*", "").trim();
    }

    /**
     * Formats a price for entry into the Per Unit Price field: a whole
     * amount with no decimals (the field itself carries none, e.g.
     * "6100."), otherwise two decimal places. No currency symbol or
     * thousands separators, which the field does not accept.
     */
    private String formatPrice(double amount) {

        return amount == Math.floor(amount)
                ? String.valueOf((long) amount)
                : String.format("%.2f", amount);
    }

    // Stamped on every step so a run's log says where its time went.
    private long lastStepAtMillis = System.currentTimeMillis();

    private void step(String description) {

        long now = System.currentTimeMillis();
        long sincePrevious = now - lastStepAtMillis;
        lastStepAtMillis = now;

        System.out.println(String.format(">>> STEP (+%d.%03ds): %s",
                sincePrevious / 1000, sincePrevious % 1000, description));
    }

}
