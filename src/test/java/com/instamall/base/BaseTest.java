package com.instamall.base;

import com.instamall.components.permissions.SystemAlerts;
import com.instamall.config.ConfigReader;
import com.instamall.driver.AppSession;
import com.instamall.driver.DriverManager;
import com.instamall.factory.DriverFactory;
import com.instamall.pages.home.HomePageLocators;
import com.instamall.pages.onboarding.OnboardingPageLocators;
import com.instamall.waits.WaitUtils;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.TimeoutException;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;
import java.util.List;

public class BaseTest {

    /** Cold start: bundle load plus splash before the first screen paints. */
    private static final Duration FIRST_SCREEN_TIMEOUT = Duration.ofSeconds(45);

    protected IOSDriver driver;

    @BeforeClass(alwaysRun = true)
    public void startSession() {

        driver = DriverFactory.initializeDriver(
                autoDismissAlerts(), ConfigReader.get(appPathKey()), processArguments());
        DriverManager.setDriver(driver);

        // Pre-grants every permission named by config.properties'
        // "autoGrantedPermissions" to whichever app this class drives
        // (Customer or Shopper, via bundleIdKey()), before resetApp()'s
        // relaunch below ever shows it a screen — so the permission
        // alerts this would otherwise raise (and which can silently sit
        // on screen blocking every element lookup after them, see
        // SystemAlerts' own doc comment) never appear in the first place.
        AppSession.grantConfiguredPermissions(ConfigReader.get(bundleIdKey()));
    }

    /**
     * Whether iOS should answer system alerts by itself for this class.
     *
     * Screen tests want that — it keeps them free of permission popups.
     * Flows that need a permission actually granted override this and
     * handle the alerts themselves.
     */
    protected boolean autoDismissAlerts() {

        return true;
    }

    /**
     * Extra launch arguments to hand the app for this class — empty for
     * every class that does not ask for one, so this changes nothing for
     * them by default. See DriverFactory's own doc comment.
     */
    protected List<String> processArguments() {

        return List.of();
    }

    /**
     * Which config property names the app to install and launch.
     * Overridden by ShopperBaseTest, so the session lifecycle here does
     * not have to be duplicated per app.
     */
    protected String appPathKey() {

        return "appPath";
    }

    /**
     * Which config property names that app's bundle id, used to relaunch
     * it fresh before every test.
     */
    protected String bundleIdKey() {

        return "bundleId";
    }

    @BeforeMethod(alwaysRun = true)
    public void resetApp() {

        // Session setup already failed — skip instead of throwing an NPE
        // from every test, which buries the real @BeforeClass error.
        if (driver == null) {

            throw new SkipException(
                    "Driver was not initialized — see the startSession failure above");
        }

        String bundleId = ConfigReader.get(bundleIdKey());

        // App ko band karke dobara kholo — fresh state,
        // lekin WDA session wahi purana (fast).
        driver.terminateApp(bundleId);
        driver.activateApp(bundleId);

        awaitRelaunchFirstScreen();
    }

    /**
     * Lets the cold start finish before the test looks at the screen.
     *
     * The relaunch shows a splash for several seconds while the bundle
     * loads, longer than the framework's short glances. A test that opens
     * with "is the Login button showing?" to decide whether to sign in
     * would answer no on the splash, skip signing in, and then wait for a
     * home screen that never comes. So this waits for whichever of the two
     * opening screens arrives — onboarding when signed out, home when a
     * session is active — and gives up quietly if neither does, leaving
     * the test's own waits to say what went wrong.
     *
     * Overridable: this checks Customer-app locators, which mean nothing
     * to another app installed on the same simulator — see
     * ShopperBaseTest.
     *
     * Also grants any permission alert that shows up here — confirmed
     * live that App Tracking Transparency can arrive at this exact point
     * (right after a fresh relaunch, before onboarding even paints) with
     * nothing else in the Customer-app flow watching for it at this
     * stage; left unanswered, it belongs to springboard, not the app, and
     * sits there through the rest of the run and its teardown, silently
     * blocking every element lookup after it. Harmless for the (most)
     * classes that leave autoDismissAlerts at its default true: iOS has
     * already answered any alert before this ever gets a chance to see
     * one, so this finds nothing to tap and no-ops.
     */
    protected void awaitRelaunchFirstScreen() {

        try {

            WaitUtils.waitUntil(webDriver -> {

                        SystemAlerts.grantAnyShowingNow();

                        return WaitUtils.isVisibleNow(OnboardingPageLocators.LOGIN_BUTTON)
                                        || WaitUtils.isVisibleNow(HomePageLocators.TAB_HOME)
                                ? Boolean.TRUE
                                : null;
                    },
                    FIRST_SCREEN_TIMEOUT);

        } catch (TimeoutException neitherAppeared) {

            System.out.println(">>> Neither onboarding nor home showed within "
                    + FIRST_SCREEN_TIMEOUT.toSeconds() + "s of relaunch — carrying on.");
        }
    }

    @AfterClass(alwaysRun = true)
    public void endSession() {

        DriverManager.quitDriver();
    }
}