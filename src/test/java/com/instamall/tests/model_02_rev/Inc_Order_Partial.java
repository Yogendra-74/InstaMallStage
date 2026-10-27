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
 * The "increase the price on half a multi-item order" variant of the full
 * cross-app lifecycle. The Customer app places one order holding three
 * products from a single store (exactly as the Multiple-order Phase 1 builds
 * the bag), and in Phase 2 the Shopper accepts EVERY item — none is rejected
 * or refunded — the only difference between the items being the price:
 *
 * <ol>
 *   <li><b>The first (ceiling) half of the run</b> have their unit price
 *       raised 20% on the "Got it" sheet before being accepted — the same
 *       pencil edit the Increased-price single-item scenario runs (tap the
 *       pencil beside "Per Unit Price", read the written price, enter a
 *       20%-increased price, then tap GOT IT). The ceiling-half split is the
 *       Partial-order one: for an even count, exactly half (N/2); for an odd
 *       count, (N+1)/2 — the larger half.</li>
 *   <li><b>The remaining items</b> are accepted as-is, at their original
 *       price, with a plain GOT IT.</li>
 * </ol>
 *
 * <p>With every item accepted, "Validate and proceed" leads on through Confirm
 * payment and receipt capture, leaving the order WAITING FOR DRIVER — so Phases
 * 3a, 4, 3b and 5 dispatch, hand off and confirm it Delivered end to end,
 * unchanged from the Multiple-order flow.
 *
 * <p>Phases 0, 1, 3a, 4, 3b and 5 are the Multiple-order flow's verbatim, and
 * the price-edit steps are each confirmed live in the Increased-price
 * single-item scenario. What is newly assembled here — and NOT yet confirmed
 * live — is raising the price on only the first half of a multi-item run while
 * accepting the whole run.
 */
public class Inc_Order_Partial {

    // The first phase of this run to fail, if any — set by
    // noteOutcome() after each phase, checked by stopAfterFailure()
    // before each. Null while everything is passing.
    private String failedPhase;

    // The order this run is working on. Set by Phase 1 when it places
    // one; every later phase resolves it through PlacedOrder.resolve()
    // first thing, so a phase run on its own picks it up from -DorderId
    // or from the last recorded Phase 1 instead.
    private String orderId;

    // The same order as Delivery Solutions knows it — the full
    // orderExternalId its webhooks carry, which is what Phases 3a
    // and 3b address their calls to. Not the same string as
    // orderId above, which is the short id the Customer app shows.
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
    // moment to actually raise the next alert, rather than every trial
    // firing back to back faster than the app can render anything.
    private static final long PERMISSION_TRIAL_PAUSE_MILLIS = 1000;

    // ---------- PHASE 0 constants ----------

    private static final String WDA_BUNDLE_ID = "com.facebook.WebDriverAgentRunner.xctrunner";

    // Where Appium's own XCUITest driver keeps its bundled copy of
    // WebDriverAgent — confirmed live on this machine. Not config-driven:
    // this mirrors a local npm install layout, same as config.properties'
    // own hardcoded /Users/yogendra/Downloads app paths.
    private static final String WDA_PROJECT_DIR = System.getProperty("user.home")
            + "/node_modules/appium-xcuitest-driver/node_modules/appium-webdriveragent";

    // A successful "xcodebuild ... test" run against WebDriverAgentRunner
    // never exits on its own — confirmed live — it stays up hosting the
    // runner's own HTTP server, and this is the line it prints once that
    // server is ready.
    private static final String WDA_READY_MARKER = "ServerURLHere->";

    // Same headroom DriverFactory already gives a fresh WebDriverAgent
    // build via wdaLaunchTimeout, for the same reason: the first build for
    // a given simulator runtime runs well past a one-minute default.
    private static final Duration WDA_BUILD_TIMEOUT = Duration.ofMinutes(4);

    // ==================== Stop after the first failure ====================

    /**
     * Runs before every phase: once a phase of this run has failed, every
     * later phase is skipped with a message naming it, rather than run
     * against app state the failed phase never produced. A phase run on
     * its own is untouched — the flag is only ever set inside one run.
     */
    @BeforeMethod(alwaysRun = true)
    public void stopAfterFailure(java.lang.reflect.Method phase) {

        if (failedPhase != null) {

            throw new SkipException("Skipped " + phase.getName()
                    + " because an earlier phase of this run failed: " + failedPhase);
        }
    }

    /**
     * Runs after every phase and remembers the first one to fail.
     */
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

        // Neither app is installed here: each phase's own
        // DriverFactory.initializeDriver() installs its app as part of
        // starting that session (the "appium:app" capability), so each
        // app installs exactly once, by the phase that first needs it.
        //
        // Location still has to be granted here, before the Customer app
        // is ever installed by anyone — not after Phase 1's session has
        // installed/launched it. "xcrun simctl privacy --help" documents
        // why: "Some permission changes will terminate the application if
        // running." Confirmed live too (exit 0) that the grant itself
        // does not need the app installed first — TCC's grant is keyed by
        // bundle identifier alone, not tied to a binary being on disk,
        // which is exactly why "simctl privacy" is the standard way to
        // pre-authorize a permission ahead of an app's very first
        // install/launch. Notifications has no simctl privacy equivalent
        // — left to grantPermissions() in Phase 1, which tolerates that
        // permission card whether or not it shows.
        step("Granting location access to the Customer app before it is ever installed");
        runCommand(List.of("xcrun", "simctl", "privacy", udid, "grant", "location",
                ConfigReader.get("bundleId")), null);
    }

    /**
     * There is no booted-simulator UDID in config.properties — only
     * deviceName/platformVersion, which XCUITestOptions resolves on its
     * own. Discovering the booted device's own UDID live, rather than
     * hardcoding one, is what keeps this step valid across simulator
     * resets/rebuilds.
     */
    private String resolveBootedSimulatorUdid() {

        String output = runCommand(List.of("xcrun", "simctl", "list", "devices"), null);

        Matcher matcher = Pattern.compile("\\(([0-9A-Fa-f-]{36})\\)\\s*\\(Booted\\)").matcher(output);

        Assert.assertTrue(matcher.find(), "No booted iOS Simulator found — boot one before running this test");

        return matcher.group(1);
    }

    /**
     * "xcrun simctl listapps" lists every app on the device by bundle id,
     * WebDriverAgentRunner's own included once it has ever been installed
     * — confirmed live against a Simulator that already had a session run
     * on it.
     */
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
                    // Appium's bundled WebDriverAgent project predates
                    // Xcode 27's minimum supported deployment target
                    // (15.0) and trips its now-fatal
                    // -Wreserved-identifier warnings in XCTest's private
                    // headers — both confirmed live (Xcode 27.0, build
                    // 27A266a) to fail the build without these overrides.
                    "IPHONEOS_DEPLOYMENT_TARGET=15.0",
                    "GCC_TREAT_WARNINGS_AS_ERRORS=NO",
                    "CLANG_WARN_RESERVED_IDENTIFIER=NO",
                    "test");

            builder.directory(wdaProjectDir);
            builder.redirectErrorStream(true);
            builder.redirectOutput(buildLog);

            // Not waited on: see WDA_READY_MARKER's own doc comment —
            // this process outlives this method on purpose, left running
            // for the Appium session that follows to talk to over HTTP.
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

    // ==================== Phase 1: Customer app — place a three-item single-store order ====================

    @Test(priority = 1, description = "Phase 1: Customer app — place a three-item single-store order")
    public void phase1_placeOrderFromCustomerApp() {

        step("PHASE 1: Customer app — place a three-item single-store order");

        freshInstall(ConfigReader.get("bundleId"), "Customer");

        IOSDriver driver = DriverFactory.initializeDriver(false, ConfigReader.get("appPath"));
        DriverManager.setDriver(driver);

        try {

            // Location is never granted mid-session: "xcrun simctl
            // privacy --help" documents that "Some permission changes
            // will terminate the application if running", which is why
            // prepareSimulator() and freshInstall() grant it before the
            // app is launched.
            //
            // The simulated location is a plain OS-level "xcrun simctl
            // location ... set" — confirmed live to work (exit 0) without
            // touching WDA at all: anything that can be done without
            // asking WDA to proxy it to the app under test is one less
            // thing that can disturb that app's running process.
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
            SelectLocationSheet locationSheet = new SelectLocationSheet();
            ShoppingBagPage bagPage = new ShoppingBagPage();

            String email = ConfigReader.get("testEmail");
            String password = ConfigReader.get("testPassword");
            String addressName = ConfigReader.get("deliveryAddressName");
            String mallName = ConfigReader.get("mallName");
            String storeName = ConfigReader.get("storeName");
            String cardLast4 = ConfigReader.get("savedCardLast4");

            // ---------- Step 1: Activity Tracking ----------

            // Confirmed live on the new build: the App Tracking
            // Transparency alert ("Allow 'InstaMall Stage' to track your
            // activity across other companies' apps and websites?", with
            // "Ask App Not to Track" and "Allow") comes up over the login
            // screen the moment the app launches. iOS only asks once per
            // install, so it is absent on most runs — hence the same
            // try/catch the Shopper app's own tracking step uses: absent,
            // or failing for any reason, it is logged and the phase goes
            // on.
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

            // The app launches straight onto the sign-in landing screen:
            // a country chip, a "Mobile number" field and
            // a Continue, then "Continue with Apple", "Continue with
            // Google", "Continue with Email" and "Show more options".
            //
            // That screen only shows while signed out. The app is never
            // uninstalled, so from the second run on it resumes signed
            // in — and then there is nothing to sign in to, which is why
            // the sign-in steps sit behind this check rather than running
            // every time.
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

                // Confirmed live: this opens the "Welcome back" screen —
                // "Your personal edit awaits.", an email field, a
                // password field, "Forgot password?", Continue, and
                // "Continue with phone instead" below it.
                step("Step 2: Click on \"Continue with Email\"");
                loginPage.clickContinueWithEmail();

                emailLoginPage.waitForScreen();
                step("Sign in as " + email);

                // Activity Tracking (Step 1) is the one permission that
                // shows at app launch, which is why it has its own step up
                // there. Notification and Location are different: both
                // only show after sign-in, when the account first logs in,
                // and confirmed live in no fixed order relative to each
                // other. So submitCredentials() (not clickNext() — see its
                // own doc comment) only submits the form here, and the
                // loop below grants both permissions explicitly.
                emailLoginPage.enterEmail(email).enterPassword(password).submitCredentials();

                step("Step 3: Grant Notification and Location permissions"
                        + " (order not guaranteed)");
                grantPermissions(CUSTOMER_SIGN_IN_PERMISSIONS);

                // Whatever is left after permissions — the Save Password
                // alert and the consent sheets (Terms and Conditions, AI &
                // Biometric Consent, AI Stylist Consent) — is exactly what
                // ConsentPrompts.acceptAfterSignIn() already handles for
                // every other test in this suite that signs in through
                // this same page object; it returns once Home is on
                // screen with nothing on top of it.
                ConsentPrompts.acceptAfterSignIn(HomePageLocators.TAB_HOME);

                // ---------- Home ----------

                homePage.waitForScreen();
                step("Home screen");

            } else {

                // Already signed in — none of the sign-in, permission or
                // consent steps above applies. Force back to the Home tab
                // rather than assuming the app happened to resume there,
                // then carry on exactly as the fresh-sign-in branch does.
                step("A session is already active — skipping sign-in,"
                        + " going straight to the Home tab");
                homePage.openHomeTab().waitForScreen();
                step("Home screen");
            }

            // ---------- Delivery location ----------

            // Confirmed live: the header chip ("Deliver to 250 NW 23rd
            // St") opens the "Deliver to" sheet — "Where are we
            // delivering?", an address search box, Home / Work / Other
            // slots, a Nearby address and the saved addresses.
            step("Step 1: Tap the \"Deliver to\" location on the top left");
            homePage.openLocationSelector();
            locationSheet.waitForScreen();

            // Confirmed live: the saved row reads "Automation Address 38
            // NW 24th St, Miami, FL 33127, USA"; tapping it closes the
            // sheet and the header reads "Deliver to AUTOMATION ADDRESS".
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
            Assert.assertTrue(tiles.size() >= 3,
                    "Need at least three product tiles on the " + brand + " grid to add three products,"
                            + " but found " + tiles.size());

            // ---------- First product ----------
            // The first tile on the grid, and only that one. No looking
            // for another if it cannot be bought: per instruction, this
            // run buys the product the store puts first, and fails
            // outright when that product is unavailable rather than
            // quietly ordering something else. Adding it drops back to the
            // store grid, where the "View bag" pill now reads one item.
            step("First product: " + tileNameWithoutBrandAndPrice(tiles.get(0), brand));
            AddedProduct first = addTileToBag(tiles.get(0), brand, storePage, productPage, productValidator);
            step("Added to bag (1 of 3): " + first.name() + " (" + first.size() + ", " + first.color() + ")");

            // ---------- Second product ----------
            // Back on the store grid, add the second tile the same way.
            // Confirmed live the same as the first: adding it closes the
            // product page and drops back to the store grid, now with the
            // "View bag" pill reading two items.
            step("Second product: " + tileNameWithoutBrandAndPrice(tiles.get(1), brand));
            AddedProduct second = addTileToBag(tiles.get(1), brand, storePage, productPage, productValidator);
            step("Added to bag (2 of 3): " + second.name() + " (" + second.size() + ", " + second.color() + ")");

            // ---------- Third product ----------
            // Back on the store grid again, add the third tile the same
            // way. Adding it drops back to the store grid once more, now
            // with the "View bag" pill reading three items.
            step("Third product: " + tileNameWithoutBrandAndPrice(tiles.get(2), brand));
            AddedProduct third = addTileToBag(tiles.get(2), brand, storePage, productPage, productValidator);
            step("Added to bag (3 of 3): " + third.name() + " (" + third.size() + ", " + third.color() + ")");

            // ---------- Back to the stores list ----------
            // Tap the back button in the top-left corner of the store
            // screen to return to the mall's stores list.
            step("Tap the back button (top left) to return to the stores list");
            storePage.goBack();
            mallPage.waitForScreen();

            // ---------- Open the bag from the stores list ----------
            // On the stores list, tap the bag icon in the top-right corner.
            step("Tap the bag icon in the top-right corner of the stores list");
            mallPage.openBag();
            bagPage.waitForOpen();

            // ---------- The single Prada store group ----------
            // There is only one store group in the bag, so there is nothing
            // to choose between — this run checks out the one group it built.
            // Per framework rule the store checked out is always Prada.
            //
            // The three items are NOT echo-verified on the order
            // confirmation (an empty expectation list skips that check):
            // this flow only needs the order placed so the Shopper phase can
            // pick it off the queue and accept it.
            List<CheckoutFlow.ItemExpectation> items = List.of();

            // ---------- Expand the store group, reach its checkout button ----------
            // This build shows a store group's "Review delivery & checkout"
            // button only once the group is expanded, so expand it first,
            // then scroll down until the button is on screen (stopping at the
            // bottom if it is not).
            step("Expand the \"" + brand + "\" store group");
            bagPage.expandStoreGroup(brand);

            step("Scroll down to the \"Review delivery & checkout\" button");
            bagPage.scrollToCheckoutButton();

            // ---------- Checkout, payment, confirmation — shared with every
            // other journey, from "Review delivery & checkout" onward ----------
            CheckoutFlow checkout = new CheckoutFlow(this::step);
            checkout.reviewDeliveryAndCheckout();

            CheckoutFlow.Placed placed = checkout.placeOrder(addressName);
            String placedOrderId = checkout.payAndVerifyOrder(placed, cardLast4, brand, items);

            step("Order placed for " + brand + " with 3 item(s): " + placedOrderId);

            // Tap the copy button beside the order number to get the
            // complete order ID on the clipboard.
            step("Tap the copy button beside the order number");
            new OrderConfirmationPage().copyOrderId();

            String clipboard = driver.getClipboardText();
            step("Clipboard after copy: " + (clipboard == null || clipboard.isEmpty()
                    ? "(empty — nothing was copied)"
                    : "\"" + clipboard + "\""));

            this.orderId = normalizeOrderId(placedOrderId);

            // The copy button gives the complete order ID — the full
            // ULID that Delivery Solutions knows the order by. No API
            // lookup needed.
            String copiedId = clipboard != null ? clipboard.trim() : null;

            if (copiedId != null && !copiedId.isEmpty()) {

                this.orderExternalId = copiedId;
                step("Delivery Solutions order id from clipboard: " + copiedId);

            } else {

                step("Clipboard was empty — falling back to the API lookup");
                this.orderExternalId = fetchExternalOrderId(this.orderId);
            }

            // For any later phase run on its own — see PlacedOrder.
            PlacedOrder.record(this.orderId, this.orderExternalId);

            // Per instruction: a flat 2s on the order confirmation, so
            // it is readable, then close the app. Same ending Phase 2,
            // Phase 4 and Phase 5 have.
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

    // ==================== Phase 2: Shopper app — view and accept the order ====================

    @Test(priority = 2, description = "Phase 2: Shopper app — accept every item, raising the price 20% on the first half")
    public void phase2_increasePriceOnPartialOrderFromShopperApp() {

        // No order id here on purpose: the Shopper app phases act on the
        // first order in the queue and never read or compare the id.
        step("PHASE 2: Shopper app — accept every item, raising the price on the first half");

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

            // The one place the Shopper app signs in. The app is never
            // uninstalled and every session runs with noReset, so from
            // the second run on it is already signed in and the login
            // screen never comes up — signing in again is neither
            // possible nor wanted. The permission alerts below belong
            // inside this branch for the same reason: iOS only ever asks
            // once per install, at that first sign-in.
            if (WaitUtils.isVisibleNow(ShopperLoginPageLocators.LOG_IN_BUTTON)) {

                step("Log in as " + ConfigReader.get("shopperEmail"));
                loginPage.waitForScreen()
                         .enterEmail(ConfigReader.get("shopperEmail"))
                         .enterPassword(ConfigReader.get("shopperPassword"))
                         .tapLogIn();

                // Confirmed live, step by step: the Log in tap is followed
                // by the Notifications card ("Don't Allow" / "Allow"), the
                // Location card ("Allow Once" / "Allow While Using App" /
                // "Don't Allow") and the Activity Tracking card ("Ask App
                // Not to Track" / "Allow"). Their order is not guaranteed
                // and any of them can be absent, so they are traversed one
                // by one rather than waited for in sequence.
                step("Allow the permission cards that follow the Log in tap");
                grantPermissions(SHOPPER_SIGN_IN_PERMISSIONS);

            } else {

                step("Already signed in from an earlier run — skipping sign in and permissions");
            }

            // Steps 35-36: leave the shopper on shift, whatever the app
            // launched into. One nested if/else, per instruction: if the
            // "Go on shift?" sheet is up, tap "Go on shift" and give the
            // badge two seconds; otherwise read the badge and turn the
            // shift on from whatever state it is in. Shared with every
            // other shopper journey — see ShopperShift.
            step("Steps 35-36: Go on shift, however the app launched");
            ShiftStatus shiftStatus = shift.ensureOnShift();

            homePage.waitForScreen();
            step("On Home");

            // Step 37: open the first order listing, by the status just
            // stored above (OFFLINE and ON BREAK were turned into ON
            // SHIFT by their branches, so only two cases remain):
            //   IN ORDER  — the shopper is already inside a run; its
            //               card carries "IN PROGRESS", tap that
            //   ON SHIFT  — a new order is waiting; its card carries
            //               "VIEW ORDER", tap that
            // Confirmed live for ON SHIFT: the card is one leaf element
            // with the button fused into its name, and the bottom-biased
            // tap in ShopperHomePage.tapCardCta() lands on it; the order
            // detail (order number, IN PROGRESS, RUN PROGRESS, the item,
            // GOT IT, Validate and proceed) opened straight after.
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

            // ================================================================
            // WHERE THIS SCENARIO COMBINES Partial_Order_Found'S SPLIT WITH
            // Increased_Price_Single'S PRICE EDIT.
            //
            // EVERY item is accepted — NONE is rejected or refunded. The only
            // difference between the items is the price:
            //
            //   * the first (ceiling) half of the run have their unit price
            //     raised 20% on the "Got it" sheet before being accepted
            //     (Increased_Price_Single's pencil edit),
            //   * the remaining items are accepted as-is, at their original
            //     price, with a plain GOT IT.
            //
            // The split is Partial_Order_Found's: half when the count is even
            // (N/2), or (N+1)/2 — the larger half — when it is odd. Integer
            // (N + 1) / 2 is the ceiling of N/2 and gives both: N=4 -> 2,
            // N=3 -> 2, N=2 -> 1.
            //
            // Each item lists its own GOT IT down a scroll view; accepting one
            // leaves it with none, so each loop's scroll-to-button lands on the
            // next still-pending item. "Validate and proceed" only comes ready
            // once all of them are accepted, so the RUN PROGRESS count
            // ("{done} of {total} done") is what is waited on between items.
            // ================================================================

            int totalItems = parseRunTotal(orderPage.getRunProgressCount());
            int priceEditCount = (totalItems + 1) / 2;

            step("Run has " + totalItems + " item(s) (" + orderPage.getRunProgressCount()
                    + ") — accepting every item, raising the price 20% on the first "
                    + priceEditCount + " and leaving the remaining "
                    + (totalItems - priceEditCount) + " at the original price");

            for (int item = 1; item <= totalItems; item++) {

                boolean raisePrice = item <= priceEditCount;

                // Bring the next pending item's GOT IT on screen — iOS
                // leaves a below-the-fold button out of the tree until it
                // is scrolled to.
                orderPage.scrollToGotItButton();

                if (!orderValidator.isGotItButtonDisplayedNow()) {

                    // Already accepted on a previous pass — its count is
                    // already in, so there is nothing to do for this one.
                    step("Item " + item + " of " + totalItems
                            + " already accepted — skipping Got it / Accept this item?");
                    continue;
                }

                step("Item " + item + " of " + totalItems + ": GOT IT"
                        + (raisePrice ? " (price to be raised 20%)" : " (price unchanged)"));
                orderPage.clickGotIt();

                orderPage.waitForGotItSheet();
                Assert.assertTrue(orderValidator.isGotItSheetDisplayed(),
                        "The \"Got it\" item-details sheet did not open for item " + item);

                // ---------- The increased-price edit (first half only) ----------
                // On the "Got it" sheet, tap the pencil beside the "Per Unit
                // Price" to open the price field (the keyboard comes up over
                // the sheet), read the written price, then enter a
                // 20%-increased price as the new one. The remaining items skip
                // this entirely and are accepted at their original price.
                if (raisePrice) {

                    step("Click on the \"Pencil\" icon to edit the Per Unit Price");
                    orderPage.clickEditPricePencil();

                    double writtenPrice = orderPage.getEditablePerUnitPrice();
                    step("Fetched the written Per Unit Price: " + formatPrice(writtenPrice));

                    double increasedPrice = writtenPrice * 1.20;
                    String newPrice = formatPrice(increasedPrice);
                    step("Enter the new price " + newPrice
                            + " (20% increase on " + formatPrice(writtenPrice) + ")");
                    orderPage.setPerUnitPrice(newPrice);
                }

                // Tap GOT IT on the sheet to accept the item — this opens the
                // "Accept this item?" sheet.
                step("Click on \"GOT IT\" to confirm the item");
                orderPage.confirmGotItSheet();

                orderPage.waitForAcceptItemSheet();
                Assert.assertTrue(orderValidator.isAcceptItemSheetDisplayed(),
                        "The \"Accept this item?\" sheet did not open for item " + item);

                step("Proceed on \"Accept this item?\" (item " + item + " of " + totalItems + ")");
                orderPage.proceedAcceptingItem();

                // Proceeding drops a loading overlay while the backend
                // records the acceptance; once it clears, the RUN PROGRESS
                // count advances by one. "Validate and proceed" only comes
                // ready after the last item, so this per-item count is the
                // right thing to wait on in between — same long-running-backend
                // headroom the validate button itself is given.
                step("Wait out the loading until RUN PROGRESS reads "
                        + item + " of " + totalItems + " done");
                orderPage.waitForRunProgress(item, totalItems);
            }

            step("All " + totalItems + " item(s) accepted (" + priceEditCount
                    + " at a 20% increase, " + (totalItems - priceEditCount)
                    + " at the original price)");
            orderPage.waitForValidateAndProceedReady();

            step("Validate and proceed");
            orderPage.validateAndProceed();

            orderPage.waitForConfirmPaymentSheet();
            Assert.assertTrue(orderValidator.isConfirmPaymentSheetDisplayed(),
                    "The \"Confirm payment\" sheet did not open");

            step("Confirm payment");
            orderPage.confirmPayment();

            // Step 41: confirmed live, step by step — the camera permission
            // alert ("'IM Shopper Stage' would like to access the Camera",
            // "Don't Allow" / "Allow") comes up the moment Confirm payment
            // is tapped, before the receipt screen behind it is used.
            // "Allow" is tapped, inside its own try/catch: iOS asks only
            // once per install, so on a re-run there is nothing to answer,
            // and nothing about this alert may fail the phase.
            step("Step 41: Click on \"Allow\" for the camera permission");
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

            // Confirmed live right after the alert: "RECEIPT · BURBERRY /
            // Capture the receipt", with "Camera is not available on this
            // device." in place of a viewfinder on the Simulator, a "DEV ·
            // Sample receipt" bypass, and Take photo / Toggle flash / Flip
            // camera controls.
            orderPage.waitForReceiptCaptureScreen();
            Assert.assertTrue(orderValidator.isReceiptCaptureScreenDisplayed(),
                    "\"Capture the receipt\" did not open after confirming payment");

            // Step 42: this is a Simulator, so the capture button is not
            // usable — the "DEV · Sample receipt" bypass is tapped instead.
            // Confirmed live: it moves straight to "Looks good?", with
            // RETAKE / USE PHOTO and "DEV · Upload receipt (skip confirm)".
            step("Step 42: Simulator has no camera — click on \"DEV · Sample receipt\"");
            orderPage.useSampleReceipt();
            step("\"Looks good?\" review screen is up");

            // Steps 43-45, confirmed live in one pass: USE PHOTO opens
            // "Submit receipt?" ("Upload this receipt to complete and
            // process the order."), Proceed there ends on "Order
            // processed / Receipt uploaded and order submitted.", and OK
            // returns to Home with the order's card reading WAITING FOR
            // DRIVER — which is the state Phase 3a picks up from.
            step("Step 43: Click on \"USE PHOTO\"");
            orderPage.usePhoto();

            step("Step 44: \"Submit receipt?\" -> Proceed");
            orderPage.submitReceipt();

            step("Step 45: \"Order processed\" -> OK");
            orderPage.dismissReceiptProcessed();

            homePage.waitForScreen();
            Assert.assertTrue(WaitUtils.isVisible(ShopperHomePageLocators.WAITING_FOR_DRIVER_CARD),
                    "After the receipt was submitted, Home does not show the order as WAITING FOR DRIVER");

            step("Receipt submitted — the fully-accepted, half-price-increased order is back on"
                    + " Home as WAITING FOR DRIVER, ready for Phase 3a to assign a driver"
                    + " through Delivery Solutions");

            // Per instruction: a flat 2s on Home, then leave the Shopper
            // app on screen — it is NOT closed here. Phase 3a is API
            // automation that runs in the background (HTTP calls only), so
            // the Shopper app stays in the foreground on Home, and Phase 4
            // relaunches it from there. Quitting the Appium session below
            // ends automation only; the XCUITest driver leaves the app
            // running (shouldTerminateApp defaults to false).
            try {

                Thread.sleep(HOME_SETTLE_BEFORE_CLOSE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
            }

            // A flat 5s after Phase 2 finishes, so the just-accepted order
            // has a moment to settle on the backend before Phase 3a posts
            // its delivery webhooks against it.
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

    /**
     * Moves the order from "a shopper has it" to "a courier is at the
     * store", by posting the three Delivery Solutions webhooks that say
     * so: ORDER_ASSIGNED, PICKUP_STARTED, PICKUP_AT_LOCATION.
     *
     * This used to drive the DS portal in a browser to the same end, at
     * the cost of a hand-solved reCAPTCHA, and of keeping that browser
     * alive all the way through Phase 4 so 3b would not have to solve a
     * second one. The portal was never the thing under test — the apps
     * reacting to these states is — so nothing here opens a browser now.
     *
     * Still its own test case, run on its own the same way as before,
     * given an order to work on:
     * {@code mvn test -Dtest='Increased_Price_Partial#phase3a*' -DorderExternalId=…}
     */
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

        // No order id here either — see Phase 2.
        step("PHASE 4: Shopper app — start handoff");

        IOSDriver driver = DriverFactory.initializeDriver(
                false, ConfigReader.get("shopperAppPath"), List.of("-mockDocumentScan"));
        DriverManager.setDriver(driver);

        try {

            ShopperHomePage homePage = new ShopperHomePage();
            OrderDetailPage orderPage = new OrderDetailPage();
            OrderDetailPageValidator orderValidator = new OrderDetailPageValidator();

            // Signed in and every permission granted in Phase 2, and the
            // app is never uninstalled — so this launch lands on Home.
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

            // Confirmed live: the same DEV bypass Phase 2's receipt
            // capture has exists here too — "DEV · Sample handoff photo"
            // swaps in a sample image instead of needing real camera
            // hardware, then "Looks good?" -> USE PHOTO -> "Complete
            // handoff?" -> Proceed -> "Handoff complete" -> OK.
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

            // A flat 5s after Phase 4 finishes, so the completed handoff
            // has a moment to settle on the backend before Phase 3b posts
            // its delivery webhooks against it.
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

    /**
     * Takes the order the rest of the way once Phase 4's handoff has
     * happened: PICKUP_COMPLETED, OUT_FOR_DELIVERY, ORDER_DELIVERED.
     *
     * Nothing is resumed or re-attached here. The browser-era version of
     * this phase could only run after 3a had left a logged-in window
     * open for it, in this run or recorded on disk from an earlier one;
     * three HTTP calls need none of that, so this phase stands alone
     * given an order id like any other.
     *
     * DELIVERY_AT_LOCATION ("Driver At Drop-off") is deliberately not
     * sent — the portal flow used to step through it, and the agreed
     * API sequence does not. The collection still has that payload if
     * it is wanted back.
     */
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

            // Signed in in Phase 1, and the app is never uninstalled — so
            // this launch lands on Home.
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

            // The list is searched by id; without one there is nothing
            // to look for, and this is where that shows.
            PlacedOrder.require(orderId, "Finding the order in Account > Orders");

            boolean listed = ordersListPage.isOrderListed(orderId);

            step(listed
                    ? "Found: " + ordersListPage.getOrderRowText(orderId)
                    : "Order " + orderId + " is not listed");

            Assert.assertTrue(listed, "Order " + orderId + " was not found in Account > Orders");

            // ---------- Final assertion: this is what makes the whole
            // lifecycle test pass or fail — every earlier phase (place,
            // accept, dispatch, handoff) is only a precondition for the
            // order actually reaching "Delivered" here. ----------
            step("Final assertion: the status pill reads \"Delivered\"");
            Assert.assertTrue(ordersListPage.hasStatus(orderId, "Delivered"),
                    "Order " + orderId + " was not successfully delivered — Account > Orders"
                            + " shows \"" + ordersListPage.getOrderRowText(orderId) + "\" instead of \"Delivered\"");

            step("SUCCESS: order " + orderId + " was placed with multiple items, every item accepted"
                    + " (the first half at a 20% price increase, the rest at the original price),"
                    + " dispatched through the Delivery Solutions webhooks, handed off, and confirmed"
                    + " Delivered end to end.");

            closeApp(driver, ConfigReader.get("bundleId"), "Customer");

        } finally {

            DriverManager.quitDriver();
        }
    }

    /**
     * Closes whatever simulator session is still open at the end of the
     * class (a phase that failed before its own quit). Every session runs
     * with noReset, so a leftover simulator session is the only thing that
     * can be left open.
     */
    @AfterClass(alwaysRun = true)
    public void quitAnyRemainingDrivers() {

        DriverManager.quitDriver();
    }

    // ==================== Shared helpers ====================

    /**
     * A product that has been added to the bag — what was chosen, read off
     * its own product page before the "Add to bag" tap closed it.
     */
    private record AddedProduct(String name, String size, String color) {

    }

    /**
     * Opens one store tile, picks a size the same way Phase 1's first
     * product does (the single/only size, or the middle size when the
     * sheet offers a list), adds it to the bag, and returns to the store
     * grid.
     *
     * Confirmed live: the second "Add to bag" tap closes the product page
     * and drops back to the store grid, where the "View bag" pill is both
     * the proof the item landed and the way to the bag — so this asserts
     * that pill, then waits for the store screen to confirm the return.
     */
    // How many times the return-to-grid back tap is retried before giving
    // up — a swallowed tap is the common case, and a couple of retries
    // clears it; the waitForGrid() after the loop then fails loudly if the
    // grid genuinely never came back.
    private static final int BACK_TO_GRID_ATTEMPTS = 4;

    private AddedProduct addTileToBag(String tile, String brand, StorePage storePage,
            ProductDetailPage productPage, ProductDetailPageValidator productValidator) {

        String productName = tileNameWithoutBrandAndPrice(tile, brand);

        storePage.openProductTile(tile);
        productPage.waitForScreen();

        Assert.assertTrue(productValidator.isSizeCardDisplayed(),
                "\"" + productName + "\" has no size card — nothing to buy on this product");

        // Read while the product page is still up. Confirmed live on this
        // build: "Add to bag" closes the page and drops back to the store
        // grid, and the color card goes with it, so read afterwards there
        // is nothing left to read.
        String color = colorFrom(productPage.getColorCardText());
        String chosenSize;

        if (!productValidator.isSizeCardEnabled()) {

            chosenSize = productPage.getSelectedSize();

            Assert.assertFalse(chosenSize.isEmpty(),
                    "\"" + productName + "\" offers no size at all");

            // Nothing to choose on this shape (single/no option), so
            // Step 22 does not apply — but Step 23 still does.
            step("Step 23: Click on \"Add to bag\"");
            productPage.addToBagIfNeeded();

        } else {

            List<String> sizes = productPage.openSizeSheet().getAvailableSizes();

            Assert.assertFalse(sizes.isEmpty(),
                    "\"" + productName + "\" is out of stock in every size");

            // Step 22: the single middle option when there is an odd
            // number of sizes, or the first of the two middle options
            // when there is an even number — confirmed live against a
            // 4-size list (37, 37.5, 38, 40): selected 37.5, the first of
            // the middle pair.
            int middleIndex = sizes.size() % 2 == 1
                    ? sizes.size() / 2
                    : sizes.size() / 2 - 1;

            chosenSize = sizes.get(middleIndex);

            step("Step 22: Select the mid size");
            productPage.chooseSize(chosenSize);

            // Confirmed live: the first "Add to bag" tap is what opened
            // the Select Size sheet, so once a size is picked it has to be
            // tapped again to actually add the item.
            step("Step 23: Click on \"Add to bag\" again");
            productPage.addToBagIfNeeded();
        }

        Assert.assertTrue(productValidator.isViewBagDisplayed(),
                "No \"View bag\" pill after adding to bag — the item was not added");

        // Confirmed live: "Add to bag" leaves the app on the product page
        // with the "View bag" pill (see StorePageLocators.VIEW_BAG's own
        // note — the pill shows here, not on the store grid yet), so it
        // does not drop back to the grid on its own. Tap the header back
        // chevron to return to the store grid for the next product. The tap
        // can be swallowed by the pill or the transition, and on some runs
        // Add to bag drops straight back to the grid on its own — so re-tap
        // back only while the grid is NOT yet showing, which both retries a
        // swallowed tap and skips the tap entirely when already on the grid,
        // and never taps once more from the grid into the mall. The grid is
        // confirmed by its priced tiles (position-independent) rather than
        // the pinned store header.
        for (int attempt = 0; attempt < BACK_TO_GRID_ATTEMPTS && !storePage.isGridShowingNow(); attempt++) {

            productPage.goBack();
            storePage.settleForGrid();
        }
        storePage.waitForGrid();

        return new AddedProduct(productName, chosenSize, color);
    }

    /**
     * One permission card: what to call it in the log, and how to answer
     * it when it is on screen.
     */
    private record Permission(String name, java.util.function.BooleanSupplier grant) {

    }

    /**
     * The cards the Shopper app raises after the Log in tap, confirmed
     * live in this order — though the order is not guaranteed, which is
     * why they are polled rather than awaited one after another.
     */
    private static final List<Permission> SHOPPER_SIGN_IN_PERMISSIONS = List.of(
            new Permission("Notifications", SystemAlerts::grantNotificationIfShowingNow),
            new Permission("Location", SystemAlerts::grantLocationIfShowingNow),
            new Permission("Activity Tracking", SystemAlerts::grantTrackingIfShowingNow));

    /**
     * The cards the Customer app raises once the sign-in form is
     * submitted. Activity Tracking is not among them: it comes at launch
     * instead, which is why Phase 1 answers that one as its own first
     * step.
     */
    private static final List<Permission> CUSTOMER_SIGN_IN_PERMISSIONS = List.of(
            new Permission("Notifications", SystemAlerts::grantNotificationIfShowingNow),
            new Permission("Location", SystemAlerts::grantLocationIfShowingNow));

    /**
     * Answers every permission card in the list, however they arrive.
     *
     * Each card is tried in its own try/catch on every pass, so one that
     * errors or never shows cannot stop the others being answered, and
     * each is only ever answered once. The loop runs until all of them
     * are granted or {@link #PERMISSION_WAIT_MILLIS} is up — a card can
     * arrive seconds after the one before it, and a fixed handful of
     * passes ran out while one was still on screen, blocking the rest of
     * the phase behind it.
     */
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

    /**
     * The full Delivery Solutions id for an order the Customer app has
     * just placed — the {@code orderExternalId} its webhooks carry, and
     * the only thing Phases 3a and 3b can address a call to.
     *
     * The app never displays it. Its confirmation screen renders the
     * short form only ("ORDER IM-0THZ8"), so it is read back from the
     * order service (see {@link OrderApi}), with {@code -DorderExternalId=…}
     * taking precedence for a run that wants to pin it by hand.
     *
     * A null is not quietly tolerated: Phases 3a and 3b stop at
     * {@link PlacedOrder#requireExternalId} rather than post a webhook
     * naming an order the receiver cannot find.
     */
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

            step("No Delivery Solutions order id for " + appOrderId + " — Phases 3a and 3b"
                    + " will stop and say so. The lookup's own output above says why.");
        }

        return looked;
    }

    // How long a phase stays on its last screen (Phase 1 the order
    // confirmation, Phase 2 Home) before closing the app.
    private static final long HOME_SETTLE_BEFORE_CLOSE_MILLIS = 2000;

    private void awaitShopperFirstScreen() {

        awaitUntil(() -> {

                    SystemAlerts.grantAnyShowingNow();

                    // The "Go on shift?" sheet counts as having arrived:
                    // it comes up over Home and, confirmed live, hides
                    // the ACTIVE tab underneath it, so waiting only for
                    // the dashboard times out on exactly the state the
                    // shift logic is there to handle.
                    return WaitUtils.isVisibleNow(ShopperLoginPageLocators.LOG_IN_BUTTON)
                            || WaitUtils.isVisibleNow(ShopperHomePageLocators.GO_ON_SHIFT_BUTTON)
                            || ShopperHomePage.isDisplayedNow();

                }, APP_FIRST_SCREEN_WITHIN,
                "the Shopper app's first screen (login, dashboard or \"Go on shift?\")");
    }

    /**
     * Reads the item total out of the RUN PROGRESS count, e.g. 3 from
     * "0 of 3 done" — how many items the Shopper has to accept in this run.
     */
    private int parseRunTotal(String runProgressCount) {

        Matcher matcher = Pattern.compile("of\\s+(\\d+)\\s+done").matcher(runProgressCount);

        Assert.assertTrue(matcher.find(),
                "RUN PROGRESS did not read \"<done> of <total> done\" — got \""
                        + runProgressCount + "\"");

        return Integer.parseInt(matcher.group(1));
    }

    /**
     * Closes the app at the end of a phase.
     *
     * Terminated, not just sent to the background: the next phase that
     * wants it launches it fresh. Terminating leaves the install and its
     * data container alone, so the account stays signed in — Phase 4
     * still finds the Shopper app signed in from Phase 2, and Phase 5
     * the Customer app from Phase 1.
     *
     * Tolerant on purpose: an app that is already gone is already in the
     * state this is asking for, and a phase that has done its real work
     * should not fail on the way out.
     */
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

    /**
     * Wipes this app off the simulator so the session that follows
     * installs it clean.
     *
     * Per instruction, each app is installed fresh at the start of its
     * own phase, and from then on is never uninstalled or signed out
     * again. Sessions themselves never do this: they run with noReset.
     * The uninstall is here, deliberate and in one place.
     *
     * The location grant is reapplied afterwards because an uninstall
     * takes the app's TCC entry with it, and Phase 0's grant ran before
     * this.
     */
    private void freshInstall(String bundleId, String appName) {

        String udid = resolveBootedSimulatorUdid();

        step("Fresh install: removing the " + appName + " app (" + bundleId
                + ") so this phase installs and signs in clean");

        runCommand(List.of("xcrun", "simctl", "uninstall", udid, bundleId), null);
        runCommand(List.of("xcrun", "simctl", "privacy", udid, "grant", "location", bundleId), null);
    }

    /**
     * The Customer app reopens on whatever screen it was last on — a
     * store page, the bag, checkout — not necessarily one with the tab
     * bar. Confirmed live: relaunched straight onto "Delivery & checkout"
     * after a run that stopped there. One tap of the header's back
     * chevron per poll walks it back to a screen that has the tab bar.
     */
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

    // Stamped on every step so a run's log says where its time went —
    // the seconds since the previous step, right beside what that step
    // was.
    private long lastStepAtMillis = System.currentTimeMillis();

    private void step(String description) {

        long now = System.currentTimeMillis();
        long sincePrevious = now - lastStepAtMillis;
        lastStepAtMillis = now;

        System.out.println(String.format(">>> STEP (+%d.%03ds): %s",
                sincePrevious / 1000, sincePrevious % 1000, description));
    }

}
