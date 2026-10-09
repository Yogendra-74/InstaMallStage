package com.instamall.tests;

import com.instamall.base.BaseTest;
import com.instamall.components.navigation.HeaderLocators;
import com.instamall.pages.home.HomePageLocators;
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
import java.util.Map;

/**
 * DISCOVERY, second pass. The first walk read the five menu rows the
 * landing screen shows above the fold; this one reaches what it missed:
 *
 *  - the landing screen scrolled down (ACCOUNT SETTINGS and LOGOUT rows)
 *  - the ACCOUNT SETTINGS screen itself
 *  - MY PROFILE -> Edit Account, the form most negative cases will live on
 *  - MY PROFILE -> BRAND PREFERENCES, as the representative preference screen
 *  - MANAGE ADDRESSES -> Add New Address, the other form
 *
 * Still read-only where it counts: forms are opened and dumped, never
 * saved, and LOGOUT is only looked at, never tapped.
 */
public class ProfileDiscoveryPhase2Test extends BaseTest {

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

        AppSession.clearAppData();
        AppSession.grantLocationAndNotifications();
        AppSession.setSimulatedLocation(MIAMI_LATITUDE, MIAMI_LONGITUDE);
    }

    @Test(description = "DISCOVERY: capture the profile screens the first walk missed")
    public void captureRemainingProfileScreens() {

        signIn();
        settleLocation(ConfigReader.get("deliveryAddressName"));

        homePage.waitForScreen().openProfile();

        WaitUtils.waitForVisibility(row("My profile"), Duration.ofSeconds(20));

        // ---- landing, below the fold ----

        swipeUp();
        swipeUp();

        dump("profile-landing-scrolled");

        // ---- ACCOUNT SETTINGS ----

        if (openRow("Account settings")) {

            dump("profile-account-settings");

            returnToProfile();
        }

        // ---- My profile (opens the Edit profile form directly now; the
        //      YOUR PROFILE hub and its preference rows are gone) ----

        scrollToTopOfProfile();

        if (openRow("My profile")) {

            WaitUtils.waitForVisibility(row("EDIT PROFILE"), Duration.ofSeconds(15));

            pause();
            dump("profile-edit-account");

            returnToProfile();
        }

        // ---- MANAGE ADDRESSES -> Add New Address ----

        scrollToTopOfProfile();

        if (openRow("Manage addresses")) {

            WaitUtils.waitForVisibility(row("Manage addresses"), Duration.ofSeconds(15));

            if (openRow("Add new address")) {

                pause();
                dump("profile-add-new-address");
            }
        }
    }

    // ---------- helpers ----------

    private By row(String name) {

        return AppiumBy.iOSNsPredicateString(
                "name == '" + name + "' AND accessible == 1 AND visible == 1");
    }

    private boolean openRow(String name) {

        try {

            WaitUtils.waitForClickable(row(name), Duration.ofSeconds(15)).click();

            return true;

        } catch (RuntimeException notThere) {

            System.out.println(">>> Could not open " + name + ": " + notThere.getMessage());

            return false;
        }
    }

    private void swipeUp() {

        DriverManager.getDriver().executeScript(
                "mobile: swipe", Map.of("direction", "up"));

        pause();
    }

    private void pause() {

        try {

            Thread.sleep(1500);

        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();
        }
    }

    /**
     * Back to the top of the Profile landing screen, wherever we are.
     * Re-tapping the Profile tab resets the scroll position on this app.
     */
    private void scrollToTopOfProfile() {

        returnToProfile();

        DriverManager.getDriver().executeScript(
                "mobile: swipe", Map.of("direction", "down"));

        pause();
    }

    private void returnToProfile() {

        By profileTab = HomePageLocators.TAB_PROFILE;

        if (WaitUtils.isVisible(profileTab, Duration.ofSeconds(2))) {
            homePage.openProfile();
            return;
        }

        // Pushed screens carry the shared, unnamed back chevron.
        if (WaitUtils.isVisible(HeaderLocators.BACK_CHEVRON, Duration.ofSeconds(2))) {
            DriverManager.getDriver().findElement(HeaderLocators.BACK_CHEVRON).click();
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
