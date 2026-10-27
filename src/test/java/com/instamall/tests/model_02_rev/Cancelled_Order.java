package com.instamall.tests.model_02_rev;

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
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The "item not found" variant of the MULTIPLE-order lifecycle: the
 * Customer app places one order holding three items from a single store
 * (exactly as {@link Multiple_Item_Order}'s Phase 1 builds the bag,
 * trimmed to one store), and the Shopper app then reports every item in
 * that run as not found — looping the same "Couldn't get this item" ->
 * reason -> issue refund steps {@link Cancelled_Item} runs once
 * for a single item, now once per item, until the all-rejected run
 * cancels the order.
 *
 * <p>Assembled from two existing journeys:
 *
 * <ul>
 *   <li>Phase 0 — simulator preflight, verbatim from both source classes.</li>
 *   <li>Phase 1 — Customer app places a three-item single-store order.
 *       This is {@link Multiple_Item_Order}'s Phase 1 up to and
 *       including "add three products from the first store", then its own
 *       checkout — but WITHOUT the second (Moncler) store and the random
 *       store-group pick, since there is only one group to check out.</li>
 *   <li>Phase 2 — Shopper app, verbatim from
 *       {@link Cancelled_Item}'s Phase 2 up to the order detail
 *       screen, then the "Couldn't get this item" -> reason -> issue
 *       refund -> Proceed steps run once per item in the run instead of
 *       once, ending the same way: the all-rejected order is cancelled and
 *       lands on History as ORDER REJECTED.</li>
 * </ul>
 *
 * <p>The single-item Phase 2 steps this loop is built from are each
 * confirmed live in {@link Cancelled_Item}; what is newly
 * assembled here — and NOT yet confirmed live — is driving them once per
 * item off the RUN PROGRESS count and relying on each rejected item's
 * "Couldn't get this item" button dropping out of the tree (replaced by
 * its "Rejected" status) so the next tap lands on the next item. If a
 * later item needs scrolling into view before its button is tappable,
 * that is the first thing to add here.
 *
 * <p>Wiring matches both source classes: no {@code dependsOnMethods}, so
 * a phase can be run on its own ({@code mvn test
 * -Dtest='Multiple_Order_Not_Found#phase2*'}); {@code priority} fixes
 * lifecycle order when the class is run whole; and {@link
 * #stopAfterFailure} skips every later phase once one fails. The Shopper
 * phase acts on the first order in the queue and never reads an order id,
 * so none is carried between phases.
 */
public class Cancelled_Order {

    // The first phase of this run to fail, if any — set by
    // noteOutcome() after each phase, checked by stopAfterFailure()
    // before each. Null while everything is passing.
    private String failedPhase;

    // The order Phase 1 places. Phase 3 resolves it through
    // PlacedOrder.resolve() first thing, so a phase run on its own picks
    // it up from -DorderId or the last recorded Phase 1 instead.
    private String orderId;

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(90);

    // How long a single item's refund gets to settle — the run's
    // "x of N done" count has to tick up by one after each Proceed, and
    // that write is on the same backend the single-item flow already gives
    // ample headroom for (see VALIDATE_READY_WITHIN in OrderDetailPage).
    private static final Duration REFUND_SETTLES_WITHIN = Duration.ofSeconds(60);

    // How long the just-rejected order gets to leave the Active tab and
    // turn up on History as "ORDER REJECTED". The server writes that
    // state and the tabs have to refresh for it, so how long it takes is
    // not this run's to control — the same reason Single_Order_Not_Found
    // gives its History card a generous window.
    private static final Duration ORDER_LEAVES_ACTIVE_WITHIN = Duration.ofSeconds(45);

    /**
     * How long the Notifications and Location alerts get to turn up after
     * a sign-in, rather than a fixed number of passes.
     */
    private static final long PERMISSION_WAIT_MILLIS = 30_000;

    // Paused between trials so a still-settling sign-in round trip gets a
    // moment to actually raise the next alert, rather than every trial
    // firing back to back faster than the app can render anything.
    private static final long PERMISSION_TRIAL_PAUSE_MILLIS = 1000;

    // How long a phase stays on its last screen before closing the app.
    private static final long HOME_SETTLE_BEFORE_CLOSE_MILLIS = 2000;

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

    /**
     * {@link Multiple_Item_Order}'s Phase 1, trimmed to one store:
     * adds three products from the first store and checks that single
     * store group out. The second (Moncler) store and the random
     * store-group pick are dropped — there is only one group here, and it
     * is the one the shopper later fails to find every item of.
     */
    @Test(priority = 1, description = "Phase 1: Customer app — place a three-item single-store order")
    public void phase1_placeThreeItemOrderFromCustomerApp() {

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

            // ---------- Store and products ----------

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
            // quietly ordering something else.
            step("First product: " + tileNameWithoutBrandAndPrice(tiles.get(0), brand));
            AddedProduct first = addTileToBag(tiles.get(0), brand, storePage, productPage, productValidator);
            step("Added to bag (1 of 3): " + first.name() + " (" + first.size() + ", " + first.color() + ")");

            // ---------- Second product ----------
            step("Second product: " + tileNameWithoutBrandAndPrice(tiles.get(1), brand));
            AddedProduct second = addTileToBag(tiles.get(1), brand, storePage, productPage, productValidator);
            step("Added to bag (2 of 3): " + second.name() + " (" + second.size() + ", " + second.color() + ")");

            // ---------- Third product ----------
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

            // ---------- The single store group ----------
            // Unlike Multiple_Order_Complete, there is only one store group
            // in the bag, so there is nothing to choose between — this run
            // checks out the one group it built.
            //
            // The three items are NOT echo-verified on the order
            // confirmation: this scenario only needs the order placed so
            // the Shopper phase can pick it off the queue, and it never
            // reads the item names back. (Verifying them there meant
            // expanding the confirmation's "Show N more items" toggle and
            // scrolling each item into view — all of which existed solely
            // for that check, so an empty expectation list skips it.)
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

            // Remembered for Phase 3, which verifies this same order reads
            // "Cancelled" in Account > Orders. Recorded too, so Phase 3 can
            // be run on its own and still resolve it (from -DorderId or the
            // last recorded Phase 1).
            this.orderId = normalizeOrderId(placedOrderId);
            PlacedOrder.record(this.orderId);

            step("SUCCESS: order placed for " + brand + " with 3 item(s): " + placedOrderId);

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 2: Shopper app — report every item not found ====================

    /**
     * {@link Cancelled_Item}'s Phase 2, verbatim up to the order
     * detail screen (fresh install, sign in, grant the permission cards,
     * go on shift, open the first order off the queue), then its "Couldn't
     * get this item" -> reason -> issue refund -> Proceed steps run once
     * per item in the run instead of once — until every item is Rejected,
     * which cancels the order. Ends the same way: the Active tab reads "No
     * active orders." and the order lands on History as ORDER REJECTED.
     */
    @Test(priority = 2, description = "Phase 2: Shopper app — report every item in the run as not found")
    public void phase2_reportEveryItemNotFoundFromShopperApp() {

        // No order id here on purpose: the Shopper app phases act on the
        // first order in the queue and never read or compare the id.
        step("PHASE 2: Shopper app — report every item in the run as not found");

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
            // WHERE THIS SCENARIO PARTS WAYS WITH Multiple_Order_Complete.
            //
            // The complete journey taps GOT IT on each item and goes on
            // through "Accept this item?" -> Validate and proceed ->
            // receipt capture. This one does the opposite for EVERY item:
            // the shopper reports each one as not found. "Couldn't get this
            // item" sits on the order detail screen right beside GOT IT.
            // ================================================================

            // The run holds more than one item now, so how many to reject
            // is read off the RUN PROGRESS count ("0 of 3 done") rather
            // than assumed. Each rejected item's "Couldn't get this item"
            // button drops out of the tree — replaced by its "Rejected"
            // status — so COULD_NOT_GET_ITEM's first-match predicate lands
            // on the next unhandled item on the following pass.
            int total = totalItemsInRun(orderPage);
            Assert.assertTrue(total >= 1,
                    "The run reports no items to reject — RUN PROGRESS read \""
                            + orderPage.getRunProgressCount() + "\"");

            step("Run holds " + total + " item(s) — reporting every one as not found");

            List<String> reasons = OrderDetailPageLocators.ALL_REASONS;

            for (int done = 1; done <= total; done++) {

                // The list scrolls down as items are handled, leaving the
                // next item's button below the fold and out of the tree —
                // bring it on screen before tapping it.
                orderPage.scrollToCouldNotGetItem();

                step("Item " + done + " of " + total + ": Click on \"Couldn't get this item\"");
                orderPage.couldNotGetItem();

                // The "Couldn't get" reason sheet. Rather than pin one
                // reason, a random one of the five the sheet offers is
                // picked each item — they are all valid "item not found"
                // outcomes and the refund path past this is the same
                // whichever is chosen, so rotating them widens what the
                // scenario exercises. The labels live on the locators
                // (ALL_REASONS) so this stays in step with the app.
                orderPage.waitForCouldNotGetSheet();

                String reason = reasons.get(ThreadLocalRandom.current().nextInt(reasons.size()));

                step("Item " + done + " of " + total + ": Select the \"" + reason + "\" reason");
                orderPage.chooseCouldNotGetReason(reason);

                // The refund button carries the live amount and the
                // customer's name ("ISSUE REFUND · $3,350.00 TO SUDHANSHU"),
                // both of which change every run — the locator matches the
                // fixed "ISSUE REFUND" prefix only, never the amount, so
                // the label is read off the button for the log rather than
                // built from a known price.
                step("Item " + done + " of " + total + ": Click on \""
                        + orderPage.getIssueRefundButtonLabel() + "\"");
                orderPage.clickIssueRefund();

                // The "Issue refund?" confirmation -> Proceed.
                orderPage.waitForIssueRefundConfirmation();
                Assert.assertTrue(orderValidator.isIssueRefundConfirmationDisplayed(),
                        "The \"Issue refund?\" confirmation did not open for item " + done);

                step("Item " + done + " of " + total + ": \"Issue refund?\" -> Proceed");
                orderPage.confirmIssueRefund();

                // Proceed closes the "Issue refund?" sheet and issues the
                // refund on the backend. The RUN PROGRESS count is NOT read
                // to confirm this — after the first item the list is
                // scrolled down to the next item and that header sits off
                // screen, so it cannot be read reliably. Instead wait for
                // the confirmation sheet to actually close, then let the
                // refund overlay clear, before the next item is tapped;
                // couldNotGetItem() then scrolls to and waits on the next
                // item itself.
                awaitRefundSettled();
                step("Item " + done + " of " + total + " rejected");
            }

            // Every item rejected: "Validate and proceed" comes back
            // enabled once the last refund has settled. The backend refund
            // runs first, so the button is waited out the same way the
            // single-item path waits for it, then validated to finalise the
            // run.
            orderPage.waitForValidateAndProceedReady();

            // A "Rejected" pill is NOT asserted here. On a multi-item run
            // the list is scrolled to the bottom (by the last item's own
            // couldNotGetItem()), so the item rows and their pills sit above
            // the fold and a plain visibility check finds none even though
            // every refund went through. The authoritative proof the
            // rejection took is the Active/History state checked below ("No
            // active orders." then an "ORDER REJECTED" card), not a pill
            // that happens to be scrolled off here.
            if (!orderValidator.isItemRejected()) {

                System.out.println(">>> No \"Rejected\" pill on screen after the last refund"
                        + " (the list is scrolled past the item rows) — proceeding on the"
                        + " refunds already issued; the Active/History checks below confirm it.");
            }

            step("Validate and proceed");
            orderPage.validateAndProceed();

            // A one-off "Order processed" confirmation can flash between
            // validating and landing on Home. For an all-rejected run it is
            // more likely to show than the single-item case, but it is
            // still dismissed only if it happens to be up — the real proof
            // is the Home/History state checked below.
            if (orderValidator.isOrderProcessedAlertDisplayed()) {

                step("\"Order processed\" -> OK");
                orderPage.dismissOrderProcessedAlert();
            }

            // Validating the all-rejected run cancels the order and returns
            // the shopper to Home.
            homePage.waitForScreen();
            step("On Home after the refunds");

            // Cancelling the order drops a notification banner across the top
            // of Home, over the tab bar where "History" sits, and it swallows
            // the tap until it clears on its own. So "History" is tapped on a
            // wait-until loop rather than once: each pass dismisses any system
            // alert, taps "History", and checks whether it actually opened
            // onto the cancelled order's "ORDER REJECTED" card — the loop ends
            // the moment it does, i.e. once the notification has gone and the
            // tap finally lands.
            step("Click the \"History\" tab to verify the rejected order"
                    + " (retrying until the post-cancel notification clears)");

            boolean rejectedOnHistory = awaitUntil(() -> {

                        SystemAlerts.grantAnyShowingNow();

                        if (WaitUtils.isVisibleNow(ShopperHomePageLocators.HISTORY_TAB)) {

                            try {

                                homePage.openHistoryTab();

                            } catch (Exception coveredByNotification) {

                                // The banner took this tap — try again next pass.
                            }
                        }

                        return WaitUtils.isVisibleNow(
                                ShopperHomePageLocators.orderCardContaining("ORDER REJECTED"));

                    }, ORDER_LEAVES_ACTIVE_WITHIN,
                    "the History tab to open onto the \"ORDER REJECTED\" card once the notification clears");

            Assert.assertTrue(rejectedOnHistory,
                    "No \"ORDER REJECTED\" card appeared on the History tab after retrying past"
                            + " the post-cancel notification");

            step("Refunds complete — the order is confirmed as ORDER REJECTED on the History tab");

            // Per instruction: a flat 2s on the last screen, then close
            // the app, same ending as every other phase. The app goes to
            // background and stays signed in.
            try {

                Thread.sleep(HOME_SETTLE_BEFORE_CLOSE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
            }

            closeApp(driver, ConfigReader.get("shopperBundleId"), "Shopper");

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 3: Customer app — verify the order reads Cancelled ====================

    /**
     * Reopens the Customer app and confirms the order Phase 1 placed now
     * reads "Cancelled" in Account &gt; Orders — the customer-facing proof
     * that rejecting every item in Phase 2 cancelled the order.
     *
     * The Customer app is reused, not reinstalled: Phase 1 installed and
     * signed it in, and every session runs with noReset, so it is still
     * signed in here. The order is identified by the id Phase 1 placed
     * (resolved through {@link PlacedOrder} so this phase can also be run
     * on its own).
     */
    @Test(priority = 3, description = "Phase 3: Customer app — verify the order reads Cancelled in Account > Orders")
    public void phase3_verifyOrderCancelledFromCustomerApp() {

        orderId = PlacedOrder.resolve(orderId);

        step("PHASE 3: Customer app — verify order " + PlacedOrder.describe(orderId) + " is Cancelled");

        PlacedOrder.require(orderId, "Verifying the order is Cancelled in Account > Orders");

        IOSDriver driver = DriverFactory.initializeDriver(false, ConfigReader.get("appPath"));
        DriverManager.setDriver(driver);

        try {

            HomePage homePage = new HomePage();
            HomePageValidator homeValidator = new HomePageValidator();
            ProfilePage profilePage = new ProfilePage();
            OrdersListPage ordersListPage = new OrdersListPage();

            // The app reopens on whatever screen it was last on — walk it
            // back to a screen with the tab bar, dismissing any system
            // alert on the way, then assert Home actually showed.
            boolean onHome = awaitUntil(() -> {

                        SystemAlerts.grantAnyShowingNow();
                        backOutTowardsHome();

                        return homeValidator.isHomeTabDisplayedNow();

                    }, APP_FIRST_SCREEN_WITHIN, "the Customer Home screen");

            Assert.assertTrue(onHome,
                    "The Customer app did not open on Home — was Phase 1 run on this simulator?");

            step("Account > Orders");
            homePage.openHomeTab().waitForScreen().openProfile();
            profilePage.waitForScreen();
            profilePage.openOrders();
            ordersListPage.waitForScreen();

            Assert.assertTrue(ordersListPage.isOrderListed(orderId),
                    "Order " + orderId + " was not found in Account > Orders");

            step("Found: " + ordersListPage.getOrderRowText(orderId));

            // The order-level status for an all-rejected order reads
            // "Cancelled" here — confirmed live against the "Your orders"
            // list, where each row is "IM-XXXXX, Cancelled, $…".
            step("Assert the order status reads \"Cancelled\"");
            Assert.assertTrue(ordersListPage.hasStatus(orderId, "Cancelled"),
                    "Order " + orderId + " is not Cancelled — Account > Orders shows \""
                            + ordersListPage.getOrderRowText(orderId) + "\" instead");

            step("SUCCESS: order " + orderId + " reads Cancelled in the Customer app");

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

    /**
     * Closes whatever simulator session is still open at the end of the
     * class (a phase that failed before its own quit). Closing the app
     * is fine; uninstalling it is not, and never happens — every session
     * runs with noReset, so the Customer app installed in Phase 1 and
     * the Shopper app installed in Phase 2 stay on the simulator.
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

    // How many times the return-to-grid back tap is retried before giving
    // up — a swallowed tap is the common case, and a couple of retries
    // clears it; the waitForGrid() after the loop then fails loudly if the
    // grid genuinely never came back.
    private static final int BACK_TO_GRID_ATTEMPTS = 4;

    /**
     * Opens one store tile, picks a size the same way Phase 1's first
     * product does (the single/only size, or the middle size when the
     * sheet offers a list), adds it to the bag, and returns to the store
     * grid. Verbatim from {@link Multiple_Item_Order}.
     */
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
        // with the "View bag" pill, so it does not drop back to the grid on
        // its own. Tap the header back chevron to return to the store grid
        // for the next product. The tap can be swallowed, and on some runs
        // Add to bag drops straight back to the grid on its own — so re-tap
        // back only while the grid is NOT yet showing, which both retries a
        // swallowed tap and skips the tap entirely when already on the grid,
        // and never taps once more from the grid into the mall.
        for (int attempt = 0; attempt < BACK_TO_GRID_ATTEMPTS && !storePage.isGridShowingNow(); attempt++) {

            productPage.goBack();
            storePage.settleForGrid();
        }
        storePage.waitForGrid();

        return new AddedProduct(productName, chosenSize, color);
    }

    /**
     * Reads how many items the run holds off the RUN PROGRESS count, e.g.
     * "0 of 3 done" -> 3.
     */
    private int totalItemsInRun(OrderDetailPage orderPage) {

        String count = orderPage.getRunProgressCount();

        Matcher matcher = Pattern.compile("of\\s+(\\d+)\\s+done").matcher(count);

        Assert.assertTrue(matcher.find(),
                "Could not read the item count from the RUN PROGRESS line \"" + count + "\"");

        return Integer.parseInt(matcher.group(1));
    }

    // How long the refund overlay gets to clear after the "Issue refund?"
    // sheet closes, before the next item is tapped.
    private static final long REFUND_OVERLAY_SETTLE_MILLIS = 2500;

    /**
     * Holds off the next item until the one just rejected has settled.
     *
     * Rather than read the RUN PROGRESS count — which sits at the top of
     * the screen and scrolls out of view once the list moves down to a
     * later item — this waits for the "Issue refund?" confirmation to
     * actually close (proof Proceed registered), then pauses briefly for
     * the backend refund overlay to clear. couldNotGetItem() then scrolls
     * to and waits on the next item on its own.
     */
    private void awaitRefundSettled() {

        WaitUtils.waitForInvisibility(
                OrderDetailPageLocators.ISSUE_REFUND_CONFIRM_TITLE, REFUND_SETTLES_WITHIN);

        try {

            Thread.sleep(REFUND_OVERLAY_SETTLE_MILLIS);

        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();
        }
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
     * Closes the app at the end of a phase.
     *
     * Terminated, not just sent to the background: the next phase that
     * wants it launches it fresh. Terminating leaves the install and its
     * data container alone, so the account stays signed in.
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
     * Wipes this app off the simulator so the session that follows
     * installs it clean.
     *
     * Per instruction, each app is installed fresh at the start of its
     * own phase — the Customer app in Phase 1, the Shopper app in Phase
     * 2 — and from then on is never uninstalled or signed out again.
     * That is what makes the sign-in and permission steps real work
     * rather than branches that never run.
     *
     * Sessions themselves never do this: they run with noReset, which is
     * what keeps the later phases' state intact. The uninstall is here,
     * deliberate and in one place.
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
