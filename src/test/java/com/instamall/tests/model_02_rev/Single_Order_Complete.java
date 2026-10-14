package com.instamall.tests.model_02_rev;

import com.instamall.components.consent.ConsentPrompts;
import com.instamall.components.navigation.HeaderLocators;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.config.ConfigReader;
import com.instamall.driver.DriverManager;
import com.instamall.driver.PortalDriverManager;
import com.instamall.factory.DriverFactory;
import com.instamall.factory.PortalDriverFactory;
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
import com.instamall.support.PortalSession;
import com.instamall.waits.WaitUtils;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
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
 * Same full cross-app order lifecycle as model_02's SingleOrderComplete —
 * place -&gt; accept -&gt; dispatch -&gt; handoff -&gt; verify delivered — with each
 * phase its own TestNG test case that can be run on its own.
 *
 * No phase depends on another: there is no {@code dependsOnMethods}
 * chain, so any one of them can be selected and run by itself — from
 * the IDE, or from Maven as
 * {@code mvn test -Dtest='Single_Order_Complete#phase4*' -DorderId=IM-AE078}.
 * Run as a whole class they still execute in lifecycle order, which the
 * {@code priority} on each test fixes, and the run stops at the first
 * failure: {@link #stopAfterFailure} skips every later phase, naming
 * the one that failed, so nothing runs against app state that was never
 * reached. The class is listed in testng.xml, so a plain
 * {@code mvn test} runs the whole workflow.
 *
 * What a later phase needs from an earlier one is only the id of the
 * order Phase 1 placed, and only the Portal phases (3a, 3b) and the
 * Customer-app check (5) use it — handed over through
 * {@link PlacedOrder}: in memory when the phases run together, otherwise
 * from {@code -DorderId=…} or from the id the most recent Phase 1
 * recorded on disk. Those phases resolve it first thing and go ahead
 * even when none of the three has one, failing at the step that needs
 * it (finding the order on the Portal's History tab, or in Account &gt;
 * Orders) and naming the three sources there.
 *
 * The Shopper app phases (2 and 4) do not touch the order id at all:
 * they act on the first order in the queue, and nothing in the Shopper
 * page objects reads, compares or carries the id.
 *
 * Each sign-in happens once. The Customer app signs in in Phase 1, the
 * Shopper app in Phase 2, the Delivery Portal in Phase 3a — and nothing
 * is ever uninstalled or signed out, so Phase 4 and Phase 5 open their
 * apps straight on Home, and Phase 3b resumes on the Portal browser 3a
 * left open: in one run through the driver still in memory, across runs
 * by re-attaching to the session 3a recorded on disk
 * ({@link PortalSession}). 3b alone closes that browser, once the order
 * is marked Delivered; {@link #quitAnyRemainingDrivers()} only closes a
 * leftover simulator session.
 *
 * Phase 0 (simulator preflight — WebDriverAgent, granting location before
 * the Customer app is ever installed) is not a scenario being verified,
 * just setup every phase needs, so it runs as {@code @BeforeClass} — and
 * TestNG runs a {@code @BeforeClass} for a single selected method too,
 * so a standalone phase still gets it.
 *
 * See each PHASE 0 helper method's own doc comment for how each step was
 * confirmed live (booted-device discovery, the WebDriverAgent bundle id to
 * look for, and the Xcode 27.0 build overrides it took to get Appium's
 * bundled WebDriverAgent project building at all).
 *
 * Every phase's own body is otherwise unchanged from
 * model_02.SingleOrderComplete — only how the phases are wired together
 * and how the order id reaches them is different here.
 */
public class Single_Order_Complete {

    // The first phase of this run to fail, if any — set by
    // noteOutcome() after each phase, checked by stopAfterFailure()
    // before each. Null while everything is passing.
    private String failedPhase;

    // The order this run is working on. Set by Phase 1 when it places
    // one; every later phase resolves it through PlacedOrder.resolve()
    // first thing, so a phase run on its own picks it up from -DorderId
    // or from the last recorded Phase 1 instead.
    private String orderId;

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(90);

    // Per instruction: grantNotificationAndLocationPermissions() gives
    // itself this many trials to get both permissions granted before
    // moving on regardless — not a time budget. A trial that grants
    // neither (the common case on a Simulator install where both were
    // already granted on an earlier run, so neither alert ever shows at
    // all) still counts against this the same as a trial that grants one.
    private static final int PERMISSION_TRIALS = 3;

    // Paused between trials so a still-settling sign-in round trip gets a
    // moment to actually raise the next alert, rather than every trial
    // firing back to back faster than the app can render anything.
    private static final long PERMISSION_TRIAL_PAUSE_MILLIS = 1000;

    // grantActivityTrackingIfShown()'s own, tighter budget — deliberately
    // smaller than PERMISSION_TRIALS/PERMISSION_TRIAL_PAUSE_MILLIS above.
    // Unlike Notification/Location, nothing upstream of Notification/
    // Location ever checks for them early — but Activity Tracking is
    // different: the first-screen wait each app's launch already sits in
    // (the landing-screen wait in Phase 1, awaitShopperFirstScreen()
    // in Phase 2) polls with SystemAlerts.grantAnyShowingNow() the whole
    // time it is waiting, and this alert shows at launch, within a second
    // or two of it or not at all — so by the time this method is even
    // reached, that polling has very likely already caught and cleared
    // it. A long, dedicated wait here would almost always just be
    // confirming something already gone; this budget only has to cover
    // the narrow race where the first-screen wait exited (its own
    // condition became true) a beat before this alert rendered.
    private static final int ACTIVITY_TRACKING_TRIALS = 3;
    private static final long ACTIVITY_TRACKING_TRIAL_PAUSE_MILLIS = 150;

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

        // No "xcrun simctl install" here for either app — removed.
        // Confirmed live (appium*.log): every phase's own
        // DriverFactory.initializeDriver() call installs its app as part
        // of starting that session — the "appium:app" capability makes
        // the XCUITest driver do its own install-then-launch regardless
        // of what, if anything, was already on the Simulator — the exact
        // same "thrown away a few seconds later" shape the Customer-app
        // launch used to have here (see this method's own history above
        // in version control). An install here was therefore never a
        // second, independent install the app actually needed; it was
        // the SAME install the owning phase's own session was about to
        // do anyway, repeated — which is what was showing up live as
        // "the app installs twice" and, worse, left a window where this
        // method's own just-launched-nothing Customer app install could
        // race a Phase 1 session already mid-install/launch against the
        // very same bundle id on the very same (useNewWDA=false, shared)
        // WebDriverAgent — each side's own launch terminating the
        // other's, which is what an app "closing itself and not
        // relaunching" right after an install actually was. Per
        // instruction that no process repeats anywhere in this test
        // case: each app now installs exactly once, by whichever phase
        // first actually needs it, and never before.
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
        // — left to Step 11's own try/catch in Phase 1, which already
        // tolerates that permission card whether or not it shows.
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

    @Test(priority = 1, description = "Phase 1: Customer app — place an order")
    public void phase1_placeOrderFromCustomerApp() {

        step("PHASE 1: Customer app — place an order");

        // An earlier appium:autoLaunch=false attempt here was reverted
        // when it appeared to crash WDA's HTTP server (port 8100) right
        // as terminateApp() ran a few lines below. Not the real cause —
        // see this try block's own opening comment for what actually was.
        // Back to the plain three-argument call regardless.
        IOSDriver driver = DriverFactory.initializeDriver(false, ConfigReader.get("appPath"));
        DriverManager.setDriver(driver);

        try {

            // Confirmed root cause of every "WDA crashed"/"app closed"
            // failure chased across today's session, terminateApp() and
            // the WDA-side symptoms included: "xcrun simctl privacy
            // --help" documents it outright — "Some permission changes
            // will terminate the application if running." This try block
            // used to open with AppSession.grantLocationAndNotifications(),
            // a WDA-proxied "mobile: setPermission" call made after the
            // session above had already launched the app — so it was
            // granting location to a currently-running app, which iOS
            // terminates by design the moment that grant lands. That
            // termination is also what was taking WDA's own connection to
            // the app down, which is why the NEXT command to run — whatever
            // it happened to be, terminateApp() some days, setSimulatedLocation
            // another — was the one that looked like "the crash". Moved to
            // prepareSimulator() instead, granted before the app is ever
            // launched at all (see that method's own comment) — nothing
            // left here needs to touch it mid-session. Notifications has
            // no simctl privacy equivalent; left to Step 11's own
            // try/catch below, which already tolerates that permission
            // card whether or not it shows.
            //
            // No AppSession.clearAppData() here either — per instruction, a
            // force-install of a new build, closing/switching apps, or
            // restarting the Simulator must never sign the account out.
            // Real iOS preserves an app's data container (keychain-backed
            // auth tokens included) across a reinstall-over-existing the
            // way "xcrun simctl install" does it, same as a real device —
            // clearAppData() was the one thing actively fighting that by
            // wiping it on every single run. Removing it means the
            // first-screen branch below (sign-in landing vs. Home) now
            // does real work: a genuinely fresh account signs in once,
            // and every run after that lands straight on Home instead.
            // Only a real sign-out inside the app, or uninstalling it,
            // should ever show the sign-in screen again.
            //
            // No driver.terminateApp()/activateApp() here either — the
            // session this driver already belongs to launched the app once
            // as part of starting up, which is the one launch this phase
            // needs.
            //
            // setSimulatedLocation() is likewise a plain OS-level "xcrun
            // simctl location ... set" rather than the WDA-proxied
            // AppSession.setSimulatedLocation() — confirmed live to work
            // (exit 0) without touching WDA at all, same reasoning as the
            // location-permission grant above: anything that can be done
            // without asking WDA to proxy it to the app under test is one
            // less thing that can disturb that app's running process.
            runCommand(List.of("xcrun", "simctl", "location", "booted", "set",
                    MIAMI_LATITUDE + "," + MIAMI_LONGITUDE), null);

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
            SelectLocationSheet locationSheet = new SelectLocationSheet();

            String email = ConfigReader.get("testEmail");
            String password = ConfigReader.get("testPassword");
            String addressName = ConfigReader.get("deliveryAddressName");
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

            // The "Make it personal" onboarding screen is gone from this
            // build. The app now launches straight onto the sign-in
            // landing screen: a country chip, a "Mobile number" field and
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
                grantNotificationAndLocationPermissions();

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
                step("A session is already active — skipping onboarding/sign-in,"
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

            step("Explore Malls -> Miami Design District -> Burberry");
            homePage.openMallCard("Miami Design District");

            mallPage.waitForScreen().openStore("Burberry");

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
            // it — read afterwards, as this used to be, there is nothing
            // left to read.
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

            this.orderId = normalizeOrderId(placedOrderId);

            // For any later phase run on its own — see PlacedOrder.
            PlacedOrder.record(this.orderId);

            // Per instruction: a flat 2s on the order confirmation, then
            // minimise the app — the Shopper app is next, and nothing
            // until Phase 5 needs the Customer app in the foreground.
            // Same ending Phase 2 and Phase 4 have; the app keeps its
            // session in the background.
            try {

                Thread.sleep(HOME_SETTLE_BEFORE_MINIMISE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
            }

            step("Minimize the Customer app");
            driver.executeScript("mobile: pressButton", Map.of("name", "home"));

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 2: Shopper app — view and accept the order ====================

    @Test(priority = 2, description = "Phase 2: Shopper app — view and accept the order")
    public void phase2_viewAndAcceptOrderFromShopperApp() {

        // No order id here on purpose: the Shopper app phases act on the
        // first order in the queue and never read or compare the id.
        step("PHASE 2: Shopper app — view and accept the first order in the queue");

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

                // Confirmed live, step by step: right after the Log in tap
                // come the Notifications alert ("Don't Allow" / "Allow" —
                // "Allow" is tapped) and then the Location alert ("Allow
                // Once" / "Allow While Using App" / "Don't Allow" — "Allow
                // While Using App" is tapped). Their order is not guaranteed,
                // so both are checked one by one on each pass of a 3-trial
                // loop rather than waited for in a fixed sequence.
                step("Allow the Notifications and Location permission alerts (either order)");
                grantShopperSignInPermissions();

                // Confirmed live: the third alert after Log in, once the two
                // above are answered — "Allow 'IM Shopper Stage' to track your
                // activity across other companies' apps and websites?", with
                // "Ask App Not to Track" and "Allow"; "Allow" is tapped. In
                // its own try/catch, apart from the Notifications/Location
                // loop: it is an independent permission, and whether it shows
                // at all (iOS only asks once per install) must not disturb
                // the other two or fail the phase.
                step("Allow the Activity Tracking permission alert");
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

            if (orderValidator.isGotItButtonDisplayedNow()) {

                step("GOT IT");
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

                Assert.assertTrue(orderValidator.isItemApproved(),
                        "Item does not show \"Approved\" after accepting it");

            } else {

                step("Item already approved from a previous pass — skipping Got it / Accept this item?");
            }

            Assert.assertTrue(orderValidator.isValidateAndProceedDisplayed(),
                    "\"Validate and proceed\" is not displayed once the item is approved");

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
            // DRIVER — which is the state Phase 3a picks up on the Portal.
            step("Step 43: Click on \"USE PHOTO\"");
            orderPage.usePhoto();

            step("Step 44: \"Submit receipt?\" -> Proceed");
            orderPage.submitReceipt();

            step("Step 45: \"Order processed\" -> OK");
            orderPage.dismissReceiptProcessed();

            homePage.waitForScreen();
            Assert.assertTrue(WaitUtils.isVisible(ShopperHomePageLocators.WAITING_FOR_DRIVER_CARD),
                    "After the receipt was submitted, Home does not show the order as WAITING FOR DRIVER");

            step("Receipt submitted — the order is back on Home as WAITING FOR DRIVER,"
                    + " ready for Phase 3a to assign a driver on the Delivery Portal");

            // Per instruction: a flat 2s on Home, then minimise the app
            // — nothing after this phase needs the Shopper app in the
            // foreground (Phase 3a is the Portal in a browser), same as
            // Phase 4's own ending. Confirmed live: the app goes to
            // background (state 3) and stays signed in.
            try {

                Thread.sleep(HOME_SETTLE_BEFORE_MINIMISE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
            }

            step("Minimize the Shopper app");
            driver.executeScript("mobile: pressButton", Map.of("name", "home"));

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 3a: Delivery Portal — assign a driver, mark Driver At PickUp ====================

    /**
     * Logs into the Portal and drives it up to "Driver At PickUp", then
     * deliberately leaves this browser open — in the same run and across
     * runs. {@link #phase3b_dispatchToDeliveredOnPortal()} resumes on
     * this exact same order detail screen once Phase 4 (the Shopper app
     * handoff) returns, rather than logging back in through the
     * reCAPTCHA a second time: in one run through the driver still held
     * on PortalDriverManager, in a later run by re-attaching to the
     * session {@link #openPortalOrder()} recorded on disk (see
     * {@link PortalSession}). Nothing in this class quits the browser
     * before 3b has marked the order Delivered.
     */
    @Test(priority = 3, description = "Phase 3a: Delivery Portal — assign a driver, mark Driver At PickUp")
    public void phase3a_assignDriverAndMarkAtPickupOnPortal() {

        orderId = PlacedOrder.resolve(orderId);
        step("PHASE 3a: Delivery Portal — assign a driver, then mark Driver At PickUp for order "
                + PlacedOrder.describe(orderId));

        PortalOrderDetailsPage orderDetailsPage = openPortalOrder();

        step("Assign a driver");
        orderDetailsPage.overrideStatusTo("Assigned", "Done");

        step("Driver At PickUp");
        orderDetailsPage.overrideStatusTo("Driver At PickUp", "Done");

        // A raw "xcrun simctl launch" of the Shopper app used to sit
        // here, meant to bring it to the foreground right as the
        // Portal marks the driver at pickup — removed: Phase 4's own
        // DriverFactory.initializeDriver() creates a new Appium session
        // for this same app a moment later, and that session creation
        // does its own install-then-launch regardless of whatever was
        // already running — the same thrown-away-launch shape
        // prepareSimulator() used to have (see its own comment).
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

            step("\"Looks good?\" -> USE PHOTO");
            WaitUtils.waitForClickable(OrderDetailPageLocators.USE_PHOTO_BUTTON).click();

            step("\"Complete handoff?\" -> Proceed");
            WaitUtils.waitForClickable(OrderDetailPageLocators.COMPLETE_HANDOFF_PROCEED).click();

            step("\"Handoff complete\" -> OK");
            WaitUtils.waitForClickable(OrderDetailPageLocators.HANDOFF_COMPLETE_OK).click();

            // Confirmed live: the order drops off Active and reappears on
            // History as "ORDER DELIVERED" the moment handoff completes —
            // this is the real signal the handoff actually went through,
            // not just that the sheets closed without error.
            step("Verify the order moved to the History tab");
            homePage.waitForScreen().openHistoryTab();

            WebElement deliveredCard = WaitUtils.waitForVisibility(
                    ShopperHomePageLocators.orderCardContaining("ORDER DELIVERED"),
                    Duration.ofSeconds(10));
            Assert.assertNotNull(deliveredCard,
                    "No \"ORDER DELIVERED\" card appeared on the History tab after completing handoff");

            // The card itself is the confirmation. Its "VIEW DETAILS" used
            // to be tapped only to read the order number off the detail
            // view and compare it — the Shopper app flows no longer read
            // the order id at all, so there is nothing left to open it for.
            step("Handoff complete — order delivered, confirmed on the History tab");

            // Minimize the Shopper app once this phase's own work is
            // done — Phase 3 resumes on the Portal next, and Phase 5
            // launches the Customer app fresh afterward, so nothing here
            // needs the Shopper app staying in the foreground.
            step("Minimize the Shopper app");
            driver.executeScript("mobile: pressButton", Map.of("name", "home"));

        } finally {

            DriverManager.quitDriver();
        }
    }

    // ==================== Phase 3b: Delivery Portal — dispatch the order through to Delivered ====================

    /**
     * Drives the remaining status overrides through to Delivered.
     *
     * Resumes on the Portal order detail screen 3a left open — in the
     * same run through the driver still in memory, in a later run by
     * re-attaching to the browser 3a recorded ({@link PortalSession}).
     * There is no second login: if there is no browser to resume on,
     * this fails and says so. This is the one phase that closes the
     * Portal browser, and only once the order is marked Delivered; if
     * it fails before that, the browser stays open for the next attempt.
     */
    @Test(priority = 5, description = "Phase 3b: Delivery Portal — dispatch through to Delivered")
    public void phase3b_dispatchToDeliveredOnPortal() {

        orderId = PlacedOrder.resolve(orderId);
        step("PHASE 3b: Delivery Portal — order " + PlacedOrder.describe(orderId)
                + ", dispatch through to Delivered");

        if (PortalDriverManager.getDriver() == null) {

            step("Re-attaching to the Portal browser Phase 3a left open");
            PortalDriverManager.setDriver(PortalSession.reattach().orElseThrow(() -> new AssertionError(
                    "No Portal browser to resume on — run Phase 3a first; it leaves the browser open on the order")));

        } else {

            step("Resuming on the Portal order detail screen Phase 3a left open");
        }

        PortalOrderDetailsPage orderDetailsPage = new PortalOrderDetailsPage();
        orderDetailsPage.waitForScreen();

        step("Pickup Completed");
        orderDetailsPage.overrideStatusTo("Pickup Completed", "Done");

        step("Out For Delivery");
        orderDetailsPage.overrideStatusTo("Out For Delivery", "Done");

        step("Driver At Drop-off");
        orderDetailsPage.overrideStatusTo("Driver At Drop-off", "Done");

        step("Delivered");
        orderDetailsPage.overrideStatusTo("Delivered", "Done");

        // Delivered is confirmed: this, and nowhere else, is where the
        // Portal browser gets closed. Not in a finally block — a failure
        // above leaves it open for the next attempt to re-attach to.
        step("Order marked Delivered — closing the Portal browser");
        PortalSession.close(PortalDriverManager.getDriver());
        PortalDriverManager.setDriver(null);
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

            step("SUCCESS: order " + orderId + " was placed, accepted, dispatched through the"
                    + " Delivery Portal, handed off, and confirmed Delivered end to end.");

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
     *
     * The Portal browser is deliberately NOT quit here. Phase 3a leaves
     * it open for 3b, in this run or a later one (see
     * {@link PortalSession}), and only 3b closes it, once the order is
     * marked Delivered.
     */
    @AfterClass(alwaysRun = true)
    public void quitAnyRemainingDrivers() {

        DriverManager.quitDriver();
    }

    // ==================== Shared helpers ====================

    /**
     * Phase 2's permission step: the Notifications and Location alerts
     * the Shopper app raises right after Log in, answered in whichever
     * order they arrive.
     *
     * Up to {@link #PERMISSION_TRIALS} passes. Each pass sits in its own
     * try/catch and checks the two alerts one by one — Notifications
     * ("Allow"), then Location ("Allow While Using App") — tapping
     * whichever is actually up at that instant, so an alert the pass
     * finds absent is simply looked for again on the next. A pass that
     * fails on a transient driver error (an alert dismissing itself
     * mid-tap, say) is logged and the loop goes on to the next pass
     * rather than failing the phase; the loop ends early once both have
     * been granted, and otherwise after the third pass, whatever was or
     * was not answered — exactly the tolerance every other permission
     * card in this class is handled with.
     */
    private void grantShopperSignInPermissions() {

        boolean notificationGranted = false;
        boolean locationGranted = false;

        for (int trial = 1; trial <= PERMISSION_TRIALS; trial++) {

            try {

                if (!notificationGranted && SystemAlerts.grantNotificationIfShowingNow()) {

                    notificationGranted = true;
                    step("Trial " + trial + ": Notifications alert — tapped \"Allow\"");
                }

                if (!locationGranted && SystemAlerts.grantLocationIfShowingNow()) {

                    locationGranted = true;
                    step("Trial " + trial + ": Location alert — tapped \"Allow While Using App\"");
                }

                if (notificationGranted && locationGranted) {

                    return;
                }

                Thread.sleep(PERMISSION_TRIAL_PAUSE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
                return;

            } catch (Exception trialFailed) {

                System.out.println(">>> Permission trial " + trial + " hit an error ("
                        + trialFailed.getMessage() + ") — moving on to the next trial.");
            }
        }

        step("Permission trials over — Notifications "
                + (notificationGranted ? "granted" : "not seen") + ", Location "
                + (locationGranted ? "granted" : "not seen"));
    }

    /**
     * Logs into the Delivery Portal (once, in Phase 3a) and opens this
     * run's order, leaving the browser on PortalDriverManager and its
     * session recorded for Phase 3b to resume on.
     */
    private PortalOrderDetailsPage openPortalOrder() {

        var driver = PortalDriverFactory.initializeDriver();
        PortalDriverManager.setDriver(driver);
        driver.get(ConfigReader.get("deliveryPortalUrl"));

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

        ordersPage.openOrdersFromSidebar().openHistoryTab();

        // This is the point a Portal phase cannot get past without the
        // id: there is nothing else to pick the order out by.
        PlacedOrder.require(orderId, "Finding the order on the Portal's History tab");

        // Confirming this is genuinely the order Phase 1/2 placed,
        // not just assuming the first one on the list — the Portal's
        // own internal order id embeds this app's own order id as a
        // trailing substring (confirmed live, e.g. a Portal id ending
        // "...JAE078" for Customer-app order "IM-AE078"), so the last
        // 5 characters of the dynamically-fetched orderId (never a
        // hardcoded suffix) are what every fetched Portal id is
        // checked against.
        List<String> portalOrderIds = ordersPage.getAllOrderIds();
        step("Portal order ids on the History tab (" + portalOrderIds.size() + "): " + portalOrderIds);

        String orderIdSuffix = orderId.substring(Math.max(0, orderId.length() - 5));
        step("Looking for a Portal order ending with \"" + orderIdSuffix + "\" (from order " + orderId + ")");

        ordersPage.openOrderEndingWith(orderIdSuffix);

        orderDetailsPage.waitForScreen();

        String portalOrderId = orderDetailsPage.getOrderIdValue().trim();
        step("Portal order open: " + portalOrderId
                + " (status: " + orderDetailsPage.getStatusBadge() + ")");

        Assert.assertTrue(portalOrderId.endsWith(orderIdSuffix),
                "The Portal opened order " + portalOrderId + ", which does not end with \""
                        + orderIdSuffix + "\" (from order " + orderId + ")");

        // Remembered on disk so that a Phase 3b run later, on its own,
        // can re-attach to this very browser instead of logging in again.
        PortalSession.record(driver);

        return orderDetailsPage;
    }

    /**
     * Notification and Location each raise their own native permission
     * alert sometime after a fresh sign-in — confirmed live not in any
     * fixed order relative to each other. Every trial checks both
     * independently, via SystemAlerts' own no-wait methods, and taps
     * whichever is actually up right now — so whichever one was actually
     * asked first gets "Allow" clicked first, regardless of which that
     * is. Repeats for up to PERMISSION_TRIALS trials, or until both have
     * been granted, whichever comes first — on an account where both were
     * already granted on an earlier run and neither alert shows at all,
     * every trial grants nothing and this still stops after
     * PERMISSION_TRIALS, same tolerance as every other permission card in
     * this test.
     */
    private void grantNotificationAndLocationPermissions() {

        boolean notificationGranted = false;
        boolean locationGranted = false;

        for (int trial = 1;
             trial <= PERMISSION_TRIALS && !(notificationGranted && locationGranted);
             trial++) {

            if (!notificationGranted && SystemAlerts.grantNotificationIfShowingNow()) {

                notificationGranted = true;
            }

            if (!locationGranted && SystemAlerts.grantLocationIfShowingNow()) {

                locationGranted = true;
            }

            if (notificationGranted && locationGranted) {

                return;
            }

            try {

                Thread.sleep(PERMISSION_TRIAL_PAUSE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Activity Tracking's own, lighter-weight counterpart to {@link
     * #grantNotificationAndLocationPermissions()} — see
     * ACTIVITY_TRACKING_TRIALS' own doc comment for why this one does
     * not need anywhere near as long a budget: by the time either caller
     * (Phase 1's onboarding wait, Phase 2's awaitShopperFirstScreen())
     * reaches this, its own first-screen poll has very likely already
     * caught and cleared this exact alert via SystemAlerts.
     * grantAnyShowingNow() — this is a short, cheap check for the narrow
     * race where it had not rendered yet at that point, not a real wait
     * for something expected to still be coming.
     */
    private void grantActivityTrackingIfShown() {

        for (int trial = 1; trial <= ACTIVITY_TRACKING_TRIALS; trial++) {

            if (SystemAlerts.grantTrackingIfShowingNow()) {

                return;
            }

            try {

                Thread.sleep(ACTIVITY_TRACKING_TRIAL_PAUSE_MILLIS);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    // How long a phase stays on its last screen (Phase 1 the order
    // confirmation, Phase 2 Home) before minimising the app.
    private static final long HOME_SETTLE_BEFORE_MINIMISE_MILLIS = 2000;


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
    // was. This is how the dead time between "VIEW BAG" and "Review
    // delivery & checkout", and between that and "Get it Today", was
    // pinned to the bag's and checkout's per-element reads rather than
    // to any wait.
    private long lastStepAtMillis = System.currentTimeMillis();

    private void step(String description) {

        long now = System.currentTimeMillis();
        long sincePrevious = now - lastStepAtMillis;
        lastStepAtMillis = now;

        System.out.println(String.format(">>> STEP (+%d.%03ds): %s",
                sincePrevious / 1000, sincePrevious % 1000, description));
    }

}
