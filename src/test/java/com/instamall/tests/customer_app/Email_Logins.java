package com.instamall.tests.customer_app;

import com.instamall.components.navigation.HeaderLocators;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.config.ConfigReader;
import com.instamall.driver.DriverManager;
import com.instamall.factory.DriverFactory;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.home.HomePageLocators;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.login.LoginPageLocators;
import com.instamall.pages.profile.ProfilePage;
import com.instamall.waits.WaitUtils;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Customer app's email sign-in screen ("Welcome back"), one TestNG
 * test case per scenario:
 *
 * <ol>
 *   <li>Continue stays disabled until email, password and the consent
 *       checkbox are all set — then signs in with those details and logs
 *       back out (Profile &rarr; Log out &rarr; confirm), landing back on
 *       the sign-in landing screen.</li>
 *   <li>Invalid credentials are rejected: the error text shows and the
 *       user stays on the sign-in screen.</li>
 *   <li>"Forgot password?" opens the password-reset flow.</li>
 *   <li>"Continue with phone instead" returns to the phone sign-in
 *       landing screen.</li>
 * </ol>
 *
 * Unlike the order-lifecycle classes in model_02_rev, these four
 * scenarios are independent: none depends on another, so there is no
 * {@code dependsOnMethods} chain and no stop-after-first-failure gate —
 * a failing scenario does not skip the rest. Each one can be run on its
 * own from the IDE or from Maven, e.g.
 * {@code mvn test -Dtest='Email_Logins#rejectInvalidCredentials'}.
 *
 * Every scenario starts from a clean, signed-out email sign-in screen.
 * {@link #openEmailSignInScreen} runs before each test: it reinstalls the
 * Customer app (so the landing screen always shows — no session is ever
 * carried over), launches it, answers the launch-time Activity Tracking
 * alert, and taps through "Continue with Email" to the sign-in form. The
 * reinstall is what guarantees the signed-out start: scenario 1 is the
 * only one that signs in, and it logs itself back out, but even if it did
 * not the next scenario's reinstall would still open a clean,
 * signed-out screen.
 *
 * Phase 0 (simulator preflight — WebDriverAgent, granting location before
 * the Customer app is ever installed) is the same setup every
 * model_02_rev class needs, so it runs as {@code @BeforeClass} and is
 * copied here unchanged; see its helpers' own comments for how each step
 * was confirmed live.
 */
public class Email_Logins {

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private static final Duration APP_FIRST_SCREEN_WITHIN = Duration.ofSeconds(90);

    // Deliberately wrong credentials for the rejection scenario. Hardcoded
    // rather than taken from config so a real staging account is never
    // put through repeated failed sign-ins (account lockout) — this pair
    // is meant to fail.
    private static final String INVALID_EMAIL = "no.such.user@yopmail.com";
    private static final String INVALID_PASSWORD = "WrongPass!999";

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

    // Shared across each scenario's own setup/teardown.
    private EmailLoginPage emailLoginPage;

    // ==================== Phase 0: Simulator preflight ====================

    @BeforeClass
    public void prepareSimulator() {

        step("PHASE 0: Simulator preflight — WebDriverAgent, grant location before first install");

        String udid = resolveBootedSimulatorUdid();

        ensureWebDriverAgentInstalled(udid);

        // Location is granted here, before the Customer app is ever
        // installed by anyone — "xcrun simctl privacy --help" documents
        // that some permission changes terminate a running app, and the
        // grant is keyed by bundle id alone, so it does not need the app
        // on disk first.
        step("Granting location access to the Customer app before it is ever installed");
        runCommand(List.of("xcrun", "simctl", "privacy", udid, "grant", "location",
                ConfigReader.get("bundleId")), null);
    }

    // ==================== Per-scenario setup / teardown ====================

    /**
     * Opens a clean, signed-out email sign-in screen before each
     * scenario: reinstall the Customer app, launch it, set the simulated
     * location, answer the launch-time Activity Tracking alert, wait for
     * the sign-in landing screen, then tap "Continue with Email".
     */
    @BeforeMethod(alwaysRun = true)
    public void openEmailSignInScreen(Method scenario) {

        step("SETUP for " + scenario.getName() + ": open a clean email sign-in screen");

        freshInstall(ConfigReader.get("bundleId"), "Customer");

        IOSDriver driver = DriverFactory.initializeDriver(false, ConfigReader.get("appPath"));
        DriverManager.setDriver(driver);

        // A plain OS-level location set — confirmed live to work without
        // touching WDA at all, so it cannot disturb the app under test.
        runCommand(List.of("xcrun", "simctl", "location", "booted", "set",
                MIAMI_LATITUDE + "," + MIAMI_LONGITUDE), null);

        LoginPage loginPage = new LoginPage();
        emailLoginPage = new EmailLoginPage();

        // The App Tracking Transparency alert comes up over the landing
        // screen the moment the app launches. iOS asks only once per
        // install, and every scenario reinstalls, so it shows on each run
        // — answered in its own try/catch so nothing about it fails setup.
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

        // The app launches straight onto the sign-in landing screen while
        // signed out. Each scenario reinstalls, so it is always signed out
        // and this screen is always the one that shows.
        boolean landingShown = awaitUntil(() -> {

                    SystemAlerts.grantAnyShowingNow();
                    backOutTowardsLanding();

                    return WaitUtils.isVisibleNow(LoginPageLocators.CONTINUE_WITH_EMAIL);
                },
                APP_FIRST_SCREEN_WITHIN, "the sign-in landing screen");

        Assert.assertTrue(landingShown, "The sign-in landing screen did not show after launch");

        step("Click on \"Continue with Email\"");
        loginPage.clickContinueWithEmail();

        emailLoginPage.waitForScreen();
        step("On the email sign-in screen");
    }

    @AfterMethod(alwaysRun = true)
    public void closeCustomerApp() {

        IOSDriver driver = DriverManager.getDriver();

        if (driver != null) {

            closeApp(driver, ConfigReader.get("bundleId"), "Customer");
        }

        DriverManager.quitDriver();
    }

    // ==================== Scenario 1 ====================

    @Test(priority = 1, description = "Continue gating, then sign in and log out back to the sign-in landing screen")
    public void continueDisabledUntilFieldsAndConsentComplete() {

        step("SCENARIO 1: Continue disabled until fields + consent complete");

        String email = ConfigReader.get("testEmail");
        String password = ConfigReader.get("testPassword");

        // Nothing filled yet.
        Assert.assertFalse(emailLoginPage.isContinueEnabled(),
                "Continue was already enabled with no fields filled");

        step("Step 1: Fill only the email, then observe Continue");
        emailLoginPage.enterEmail(email);
        Assert.assertFalse(emailLoginPage.isContinueEnabled(),
                "Continue became enabled with only the email filled");

        step("Step 2: Fill the password too (consent still unticked), then observe Continue");
        emailLoginPage.enterPassword(password);
        Assert.assertFalse(emailLoginPage.isContinueEnabled(),
                "Continue became enabled before the consent box was ticked");

        step("Step 3: Dismiss the keyboard and tick the consent box");
        emailLoginPage.dismissKeyboard().acceptConsent();

        Assert.assertTrue(emailLoginPage.isContinueEnabled(),
                "Continue stayed disabled after email, password and consent were all set");

        step("Continue is enabled only once email, password and consent are all set");

        // ---------- Sign in, then log out back to the landing screen ----------

        // The gating above left both fields filled with the real staging
        // credentials and the consent box ticked, so Continue is live —
        // tapping it signs in for real. clickNext() submits and then clears
        // whatever follows a fresh sign-in (Save Password alert, the
        // Notification and Location permission alerts in no fixed order,
        // and the consent sheets), returning once Home is clear.
        step("Step 4: Tap Continue to sign in");
        emailLoginPage.clickNext();

        HomePage homePage = new HomePage();
        homePage.waitForScreen();
        step("Signed in — on Home");

        // ---------- Login - Logout: Profile -> Logout -> confirm ----------

        step("Step 5: Open Profile");
        homePage.openHomeTab().waitForScreen().openProfile();

        ProfilePage profilePage = new ProfilePage();
        profilePage.waitForScreen();

        step("Step 6: Log out -> confirm");
        profilePage.tapLogout().confirmLogout();

        step("Assert the user is signed out and back on the sign-in landing screen");
        new LoginPage().waitForScreen();

        Assert.assertTrue(WaitUtils.isVisible(LoginPageLocators.SEND_OTP_BUTTON),
                "Logging out did not return the app to the sign-in landing screen");

        step("Signed out and returned to the sign-in landing screen");
    }

    // ==================== Scenario 2 ====================

    @Test(priority = 2, description = "Invalid credentials are rejected; the user stays on the sign-in screen")
    public void rejectInvalidCredentials() {

        step("SCENARIO 2: Reject invalid credentials");

        step("Step 1: Enter a wrong email/password");
        step("Step 2: Tap Continue");
        emailLoginPage.enterEmail(INVALID_EMAIL)
                      .enterPassword(INVALID_PASSWORD)
                      .submitCredentials();

        step("Assert the invalid-credentials error text is shown");
        Assert.assertTrue(emailLoginPage.isInvalidCredentialsErrorShown(),
                "No invalid-credentials error was shown after submitting wrong details");

        step("Assert the user is still on the email sign-in screen");
        Assert.assertTrue(emailLoginPage.isScreenDisplayed(),
                "The app left the email sign-in screen after a rejected sign-in");

        step("Invalid credentials were rejected and the user stayed on the sign-in screen");
    }

    // ==================== Scenario 3 ====================

    @Test(priority = 3, description = "'Forgot password?' opens the password-reset flow")
    public void forgotPasswordLink() {

        step("SCENARIO 3: Forgot password link");

        step("Step 1: Tap \"Forgot password?\"");
        emailLoginPage.clickForgotPassword();

        step("Assert the password-reset flow opened");
        Assert.assertTrue(emailLoginPage.isPasswordResetOpen(),
                "The email sign-in screen did not give way to the password-reset flow"
                        + " after tapping \"Forgot password?\"");

        step("The password-reset flow opened");
    }

    // ==================== Scenario 4 ====================

    @Test(priority = 4, description = "'Continue with phone instead' returns to the phone sign-in landing screen")
    public void toggleToPhoneSignIn() {

        step("SCENARIO 4: Toggle to phone sign-in");

        step("Step 1: Tap \"Continue with phone instead\"");
        emailLoginPage.clickContinueWithPhone();

        step("Assert the phone sign-in landing screen is back");
        new LoginPage().waitForScreen();

        Assert.assertTrue(WaitUtils.isVisible(LoginPageLocators.SEND_OTP_BUTTON),
                "The phone sign-in landing screen did not return after"
                        + " tapping \"Continue with phone instead\"");

        step("The app returned to the phone sign-in landing screen");
    }

    // ==================== Phase 0 helpers (shared with model_02_rev) ====================

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
     * WebDriverAgentRunner's own included once it has ever been installed.
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

            // Not waited on: this process outlives this method on purpose,
            // left running for the Appium session that follows to talk to
            // over HTTP.
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

    // ==================== Shared helpers ====================

    /**
     * Wipes the Customer app off the simulator so the session that
     * follows installs it clean and signed out, then reapplies the
     * location grant the uninstall took with the app's TCC entry.
     */
    private void freshInstall(String bundleId, String appName) {

        String udid = resolveBootedSimulatorUdid();

        step("Fresh install: removing the " + appName + " app (" + bundleId
                + ") so this scenario opens a clean, signed-out sign-in screen");

        runCommand(List.of("xcrun", "simctl", "uninstall", udid, bundleId), null);
        runCommand(List.of("xcrun", "simctl", "privacy", udid, "grant", "location", bundleId), null);
    }

    /**
     * Closes the app at the end of a scenario. Tolerant on purpose: an app
     * that is already gone is already in the state this is asking for, and
     * the real work of the scenario is already done by the time this runs.
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
     * On a fresh install the app opens on the sign-in landing screen, but
     * one tap of the header's back chevron per poll walks back to it from
     * any inner screen a stray tap might have reached — same shape as the
     * Customer lifecycle's walk back towards Home.
     */
    private void backOutTowardsLanding() {

        if (!WaitUtils.isVisibleNow(LoginPageLocators.CONTINUE_WITH_EMAIL)
                && !WaitUtils.isVisibleNow(HomePageLocators.TAB_HOME)
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

    // Stamped on every step so a run's log says where its time went —
    // the seconds since the previous step, right beside what that step was.
    private long lastStepAtMillis = System.currentTimeMillis();

    private void step(String description) {

        long now = System.currentTimeMillis();
        long sincePrevious = now - lastStepAtMillis;
        lastStepAtMillis = now;

        System.out.println(String.format(">>> STEP (+%d.%03ds): %s",
                sincePrevious / 1000, sincePrevious % 1000, description));
    }

}
