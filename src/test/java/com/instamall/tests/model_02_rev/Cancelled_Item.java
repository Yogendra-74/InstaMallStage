package com.instamall.tests.model_02_rev;

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
import com.instamall.pages.product.ProductDetailPage;
import com.instamall.pages.product.ProductDetailPageValidator;
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
 * The "item not found" variant of the order lifecycle: the Customer app
 * places an order exactly as in {@link Single_Item_Order}, the
 * Shopper app picks it up off the queue exactly as in that class — and
 * from the order detail screen on, the shopper reports the item as not
 * found instead of approving it.
 *
 * <p>WORK IN PROGRESS — only the two phases that are shared, verbatim,
 * with {@link Single_Item_Order} are in place so far:
 *
 * <ul>
 *   <li>Phase 0 — simulator preflight, unchanged.</li>
 *   <li>Phase 1 — Customer app places an order, unchanged.</li>
 *   <li>Phase 2 — Shopper app, unchanged up to and including the
 *       "VIEW ORDER" tap that opens the order detail screen. Everything
 *       the complete journey does from there (GOT IT, "Accept this
 *       item?", Validate and proceed, receipt capture) is where the two
 *       scenarios diverge and is deliberately NOT here yet.</li>
 * </ul>
 *
 * <p>The wiring is the same as {@link Single_Item_Order}'s, and for
 * the same reasons — see that class's own doc comment for the full
 * account. In short: no {@code dependsOnMethods}, so any one phase can
 * be run on its own ({@code mvn test -Dtest='Single_Order_Not_Found#phase2*'});
 * {@code priority} fixes lifecycle order when the class is run whole;
 * {@link #stopAfterFailure} skips every later phase once one fails; and
 * the order id Phase 1 places is handed to later phases through
 * {@link PlacedOrder} (in memory in one run, from {@code -DorderId=…}
 * or the last recorded Phase 1 otherwise).
 *
 * <p>The Shopper app phases do not touch the order id at all: they act
 * on the first order in the queue.
 */
public class Cancelled_Item {

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
    // orderExternalId its webhooks carry. Not the same string as
    // orderId above, which is the short id the Customer app shows.
    // Captured in Phase 1 exactly as Single_Order_Complete does, so the
    // not-found phases still to be written have it if they need it.
    private String orderExternalId;

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(90);

    // How long the just-rejected order gets to leave the Active tab and
    // turn up on History as "ORDER REJECTED". The server writes that
    // state and the tabs have to refresh for it, so how long it takes is
    // not this run's to control — the same reason Single_Order_Complete
    // gives its History card a generous window.
    private static final Duration ORDER_LEAVES_ACTIVE_WITHIN = Duration.ofSeconds(45);

    /**
     * How long the Notifications and Location alerts get to turn up after
     * a sign-in, rather than a fixed number of passes.
     *
     * Confirmed live on the Shopper app: on a fresh install the
     * Notifications card can arrive well after the Log in tap, and three
     * passes a second apart gave it about three seconds — the loop then
     * ended with the card still sitting on screen, blocking everything
     * after it. The alert API dismisses it correctly whenever it is
     * actually up, so what was missing was time, not handling.
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
                    + " because the earlier phase of this run failed: " + failedPhase);
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

    // ==================== Phase 1: Customer app — place an order ====================

    /**
     * Identical to {@link Single_Item_Order#phase1_placeOrderFromCustomerApp()}:
     * what the shopper later fails to find has to be ordered first, and
     * nothing about this scenario changes how it is ordered.
     */
    @Test(priority = 1, description = "Phase 1: Customer app — place an order")
    public void phase1_placeOrderFromCustomerApp() {

        step("PHASE 1: Customer app — place an order");

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
            Assert.assertFalse(tiles.isEmpty(), "No product tiles on the " + brand + " grid");

            // The first tile on the grid, and only that one. No looking
            // for another if it cannot be bought: per instruction, this
            // run buys the product the store puts first, and fails
            // outright when that product is unavailable rather than
            // quietly ordering something else.
            String tile = tiles.get(0);
            String productName = tileNameWithoutBrandAndPrice(tile, brand);

            storePage.openProductTile(tile);
            productPage.waitForScreen();

            Assert.assertTrue(productValidator.isSizeCardDisplayed(),
                    "\"" + productName + "\" has no size card — nothing to buy on this product");

            // Read while the product page is still up. Confirmed live on
            // this build: "Add to bag" closes the page and drops back to
            // the store grid, and the "Color Chino Grey" card goes with
            // it, so read afterwards there is nothing left to read.
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
                // 4-size list (37, 37.5, 38, 40): selected 37.5, the
                // first of the middle pair.
                int middleIndex = sizes.size() % 2 == 1
                        ? sizes.size() / 2
                        : sizes.size() / 2 - 1;

                chosenSize = sizes.get(middleIndex);

                step("Step 22: Select the mid size");
                productPage.chooseSize(chosenSize);

                // Confirmed live: the first "Add to bag" tap is what
                // opened the Select Size sheet, so once a size is picked
                // it has to be tapped again to actually add the item.
                step("Step 23: Click on \"Add to bag\" again");
                productPage.addToBagIfNeeded();
            }

            // Confirmed live: the second "Add to bag" tap closes the
            // product page and drops back to the store grid, where the
            // pill reads "View bag · 2". That pill is both the proof the
            // item landed and the way to the bag.
            Assert.assertTrue(productValidator.isViewBagDisplayed(),
                    "No \"View bag\" pill after adding to bag — the item was not added");

            step("Step 24: Click on \"View bag\"");
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
            // it is readable, then close the app.
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

    // ==================== Phase 2: Shopper app — open the order off the queue ====================

    /**
     * Identical to {@link Single_Item_Order#phase2_viewAndAcceptOrderFromShopperApp()}
     * up to the "VIEW ORDER" tap: fresh install, sign in, grant the
     * permission cards, go on shift, and open the first order in the
     * queue.
     *
     * <p>INCOMPLETE ON PURPOSE — the order detail screen is where the
     * "not found" journey parts ways with the complete one, and those
     * steps are not written yet. The phase currently ends with the
     * order detail open and nothing done on it, then closes the app.
     */
    @Test(priority = 2, description = "Phase 2: Shopper app — open the order off the queue")
    public void phase2_viewOrderFromShopperApp() {

        // No order id here on purpose: the Shopper app phases act on the
        // first order in the queue and never read or compare the id.
        step("PHASE 2: Shopper app — open the first order in the queue");

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
            // WHERE THIS SCENARIO PARTS WAYS WITH Single_Order_Complete.
            //
            // The complete journey taps GOT IT here and goes on through
            // "Accept this item?" -> Validate and proceed -> receipt
            // capture. This one does the opposite: the shopper reports
            // the item as not found. "Couldn't get this item" sits on the
            // order detail screen right beside GOT IT (same place the
            // complete journey reads for GOT IT).
            // ================================================================

            step("Step 1: Click on \"Couldn't get this item\"");
            orderPage.couldNotGetItem();

            // Step 2: the "Couldn't get" reason sheet. Rather than pin one
            // reason, a random one of the five the sheet offers is picked
            // each run — they are all valid "item not found" outcomes and
            // the refund path past this is the same whichever is chosen,
            // so rotating them widens what the scenario exercises over
            // repeated runs. The labels live on the locators (ALL_REASONS)
            // so this stays in step with whatever the app offers.
            orderPage.waitForCouldNotGetSheet();

            List<String> reasons = OrderDetailPageLocators.ALL_REASONS;
            String reason = reasons.get(ThreadLocalRandom.current().nextInt(reasons.size()));

            step("Step 2: Select the \"" + reason + "\" reason");
            orderPage.chooseCouldNotGetReason(reason);

            // Step 3: the refund button carries the live amount and the
            // customer's name ("ISSUE REFUND · $3,350.00 TO SUDHANSHU"),
            // both of which change every run — the locator matches the
            // fixed "ISSUE REFUND" prefix only, never the amount, so the
            // label is read off the button for the log rather than built
            // from a known price.
            step("Step 3: Click on \"" + orderPage.getIssueRefundButtonLabel() + "\"");
            orderPage.clickIssueRefund();

            // Step 4: the "Issue refund?" confirmation -> Proceed.
            orderPage.waitForIssueRefundConfirmation();
            Assert.assertTrue(orderValidator.isIssueRefundConfirmationDisplayed(),
                    "The \"Issue refund?\" confirmation did not open");

            step("Step 4: \"Issue refund?\" -> Proceed");
            orderPage.confirmIssueRefund();

            // Step 5: Proceed issues the refund and drops back on the order
            // detail — confirmed live: the item now shows "Rejected" under
            // STATUS and "Validate and proceed" comes back enabled (the run
            // reads "1 of 1 done"). The refund runs on the backend first,
            // so the button is waited out the same way the accept path
            // waits for it, then validated to finalise the run.
            orderPage.waitForValidateAndProceedReady();
            Assert.assertTrue(orderValidator.isItemRejected(),
                    "The item does not show \"Rejected\" after the refund was issued");

            step("Step 5: Validate and proceed");
            orderPage.validateAndProceed();

            // A one-off "Order processed" confirmation can flash between
            // validating and landing on Home — confirmed live it often does
            // not show at all for a single-item reject, which goes straight
            // to Home. So it is dismissed if it happens to be up and not
            // required when it is not; the real proof is the Home/History
            // state checked below.
            if (orderValidator.isOrderProcessedAlertDisplayed()) {

                step("\"Order processed\" -> OK");
                orderPage.dismissOrderProcessedAlert();
            }

            // Step 6: validating the all-rejected run cancels the order, so
            // the Active tab no longer carries it — confirmed live: it reads
            // "No active orders." This is the real signal the rejection went
            // through, not just that the sheets closed without error.
            homePage.waitForScreen();
            step("On Home after the refund");

            Assert.assertTrue(WaitUtils.isVisible(
                            ShopperHomePageLocators.NO_ACTIVE_ORDERS_MESSAGE, ORDER_LEAVES_ACTIVE_WITHIN),
                    "After the refund, the Active tab does not read \"No active orders.\""
                            + " — the order was not cancelled");

            // The cancelled order moves to History as "ORDER REJECTED".
            step("Step 6: Verify the order moved to the History tab as ORDER REJECTED");
            homePage.openHistoryTab();

            Assert.assertTrue(WaitUtils.isVisible(
                            ShopperHomePageLocators.orderCardContaining("ORDER REJECTED"),
                            ORDER_LEAVES_ACTIVE_WITHIN),
                    "No \"ORDER REJECTED\" card appeared on the History tab after issuing the refund");

            step("Refund complete — the order is cancelled, confirmed as ORDER REJECTED on the History tab");

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
     * just placed — the {@code orderExternalId} its webhooks carry. The
     * app never displays it, so it is read back from the order service
     * (see {@link OrderApi}), with {@code -DorderExternalId=…} taking
     * precedence for a run that wants to pin it by hand. Only used as the
     * fallback when the clipboard copy in Phase 1 came back empty.
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

            step("No Delivery Solutions order id for " + appOrderId
                    + " — the lookup's own output above says why.");
        }

        return looked;
    }

    // How long a phase stays on its last screen (Phase 1 the order
    // confirmation, Phase 2 the order detail) before closing the app.
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
