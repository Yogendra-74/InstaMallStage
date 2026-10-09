package com.instamall.flows;

import com.instamall.components.location.LocationServicesPrompt;
import com.instamall.config.ConfigReader;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.home.HomePageValidator;
import com.instamall.pages.location.SelectLocationSheet;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.pages.profile.ProfilePage;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.TimeoutException;

import java.time.Duration;

/**
 * From a cold launch to the Profile landing screen, whatever state the
 * app comes up in: signed out (signs in as the profile account), signed
 * in without a delivery address (settles one), or fully settled (goes
 * straight to the tab).
 *
 * Shared by the profile tests, which relaunch the app before every method
 * — the first pass through here signs in, every later pass detects the
 * live session and costs only the navigation.
 */
public final class ProfileEntryFlow {

    private static final Duration LOCATION_ENTRY_WITHIN = Duration.ofSeconds(15);

    private final OnboardingPage onboardingPage = new OnboardingPage();
    private final OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();
    private final LoginPage loginPage = new LoginPage();
    private final EmailLoginPage emailLoginPage = new EmailLoginPage();
    private final HomePage homePage = new HomePage();
    private final HomePageValidator homeValidator = new HomePageValidator();
    private final SelectLocationSheet locationSheet = new SelectLocationSheet();
    private final ProfilePage profilePage = new ProfilePage();

    /**
     * Signs in (when needed), settles the delivery address (when needed),
     * and lands on the Profile tab.
     */
    public ProfilePage openProfileTab() {

        signInIfSignedOut();
        settleLocation(ConfigReader.get("deliveryAddressName"));

        homePage.waitForScreen().openProfile();

        return profilePage.waitForScreen();
    }

    private void signInIfSignedOut() {

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

            chooseAddress(addressName);

            return;
        }

        homePage.waitForScreen();

        if (homeValidator.isLocationSelected(addressName)) {

            return;
        }

        homePage.openLocationSelector();

        chooseAddress(addressName);
    }

    private void chooseAddress(String addressName) {

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
