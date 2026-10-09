package com.instamall.tests;

import com.instamall.base.BaseTest;
import com.instamall.pages.home.HomePageLocators;
import com.instamall.pages.profile.ProfilePage;
import com.instamall.components.location.LocationServicesPrompt;
import com.instamall.config.ConfigReader;
import com.instamall.driver.AppSession;
import com.instamall.driver.DriverManager;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.home.HomePageValidator;
import com.instamall.pages.location.SelectLocationSheet;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.waits.WaitUtils;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * DISCOVERY for the Profile module. Writes nothing and taps nothing beyond
 * the Profile tab itself.
 *
 * Deliberately read-only: a profile screen is where the destructive things
 * live — sign out, change password, change email, delete account — and this
 * account is shared by every other test in the suite. Nothing here is
 * touched until the landing screen has been read and the scenarios agreed.
 */
public class ProfileDiscoveryTest extends BaseTest {

    private static final Path EXPLORATION = Paths.get("test-output/exploration");

    private final OnboardingPage onboardingPage = new OnboardingPage();
    private final OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();
    private final LoginPage loginPage = new LoginPage();
    private final EmailLoginPage emailLoginPage = new EmailLoginPage();
    private final HomePage homePage = new HomePage();
    private final HomePageValidator homeValidator = new HomePageValidator();
    private final SelectLocationSheet locationSheet = new SelectLocationSheet();

    private static final Duration LOCATION_ENTRY_WITHIN = Duration.ofSeconds(15);

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    @BeforeClass(alwaysRun = true)
    public void prepareApp() {

        // Wiped so the app comes up signed out and this signs in as the
        // profile account. Without it the app is still signed in as the
        // shared account from whatever ran last, sign in is skipped, and
        // this walks — and later changes — the wrong account entirely.
        AppSession.clearAppData();
        AppSession.grantLocationAndNotifications();
        AppSession.setSimulatedLocation(MIAMI_LATITUDE, MIAMI_LONGITUDE);
    }

    @Test(description = "DISCOVERY: capture the Profile landing screen")
    public void captureProfileScreen() {

        signIn();
        settleLocation(ConfigReader.get("deliveryAddressName"));

        homePage.waitForScreen().openProfile();

        // Give it a moment to paint before reading it.
        WaitUtils.waitUntil(driver ->
                DriverManager.getDriver().getPageSource().contains("My profile") ? Boolean.TRUE : null,
                Duration.ofSeconds(20));

        dump("profile-landing");

        // Each menu row leads to a screen this framework has never seen, and
        // the fields on them are where most negative cases will live. LOGOUT
        // is left alone deliberately: it would end the session and the rest
        // of the walk with it.
        for (String row : new String[] {
                "My profile", "Manage addresses", "Account settings",
                "Help & support", "About & legal"}) {

            openRowAndCapture(row);
        }
    }

    /**
     * Opens one Profile menu row, captures it, and comes back.
     *
     * Returns via the Profile tab rather than a back control: the tab bar is
     * on every one of these screens, and guessing at each screen's own back
     * affordance is what breaks a walk like this halfway through.
     */
    private void openRowAndCapture(String row) {

        By menuRow = AppiumBy.iOSNsPredicateString(
                "name == '" + row + "' AND accessible == 1");

        try {

            // Everything from "Manage addresses" down starts under the
            // tab bar on the redesigned Account screen.
            new ProfilePage().scrollToRow(row);

            WaitUtils.waitForClickable(menuRow, Duration.ofSeconds(15)).click();

        } catch (RuntimeException notThere) {

            System.out.println(">>> Could not open " + row + ": " + notThere.getMessage());

            return;
        }

        // No idea yet what any of these screens looks like, so settle for a
        // moment of quiet rather than waiting on an element by name.
        WaitUtils.isVisible(menuRow, Duration.ofSeconds(2));

        dump("profile-" + row.toLowerCase().replaceAll("[^a-z]+", "-"));

        returnToProfile();
        WaitUtils.isVisible(menuRow, Duration.ofSeconds(10));
    }

    /**
     * Gets back to the Profile screen from whichever sub-screen is open.
     *
     * The tab bar is not on all of them — MANAGE ADDRESSES covers it — which
     * is what stopped an earlier walk halfway through. Those screens carry a
     * back control at the top left instead, unnamed here where other screens
     * call it "BACK", so it is found by where it sits.
     */
    private void returnToProfile() {

        By profileTab = HomePageLocators.TAB_PROFILE;

        if (WaitUtils.isVisible(profileTab, Duration.ofSeconds(2))) {

            homePage.openProfile();

            return;
        }

        DriverManager.getDriver()
                .findElements(AppiumBy.iOSNsPredicateString(
                        "accessible == 1 AND visible == 1"))
                .stream()
                .filter(element -> element.getLocation().getX() < 60
                        && element.getLocation().getY() < 120
                        && element.getSize().getWidth() < 140)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No tab bar and no back control — stuck on this screen"))
                .click();
    }

    private void dump(String label) {

        try {

            Files.createDirectories(EXPLORATION);

            Path file = EXPLORATION.resolve(label + ".xml");

            Files.writeString(file, DriverManager.getDriver().getPageSource());

            System.out.println(">>> Captured " + label + " -> " + file);

        } catch (IOException exception) {

            throw new IllegalStateException("Could not write " + label, exception);
        }
    }

    // ---------- prelude, same shape as the other journeys ----------

    private void signIn() {

        if (onboardingValidator.isLoginButtonDisplayed()) {

            onboardingPage.clickLogin();

            loginPage.waitForScreen().clickContinueWithEmail();

            emailLoginPage.waitForScreen()
                          .enterEmail(ConfigReader.get("profileEmail"))
                          .enterPassword(ConfigReader.get("profilePassword"))
                          .clickNext();
        }
    }

    private void settleLocation(String addressName) {

        if (awaitLocationEntry() == LocationEntry.PROMPT) {

            LocationServicesPrompt.selectLocationNow();

            locationSheet.waitUntilVisible();
            locationSheet.waitForScreen();
            locationSheet.chooseSavedAddress(addressName);

            homePage.waitForScreen();

            return;
        }

        homePage.waitForScreen();

        if (homeValidator.isLocationSelected(addressName)
                && !homePage.listVisibleStoreCards().isEmpty()) {

            System.out.println(">>> Already at \"" + addressName + "\" — nothing to select");

            return;
        }

        homePage.openLocationSelector();

        locationSheet.waitUntilVisible();
        locationSheet.waitForScreen();
        locationSheet.chooseSavedAddress(addressName);

        homePage.waitForScreen();
    }

    private enum LocationEntry { PROMPT, HOME }

    private LocationEntry awaitLocationEntry() {

        try {

            return WaitUtils.waitUntil(driver ->
                            LocationServicesPrompt.isShowingNow()
                                    ? LocationEntry.PROMPT
                                    : homeValidator.isHomeTabDisplayedNow()
                                            ? LocationEntry.HOME
                                            : null,
                    LOCATION_ENTRY_WITHIN);

        } catch (TimeoutException neitherAppeared) {

            return LocationEntry.HOME;
        }
    }
}
