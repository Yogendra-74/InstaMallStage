package com.instamall.tests;

import com.instamall.base.BaseTest;
import com.instamall.pages.home.HomePageLocators;
import com.instamall.driver.AppSession;
import com.instamall.driver.DriverManager;
import com.instamall.flows.ProfileEntryFlow;
import com.instamall.pages.profile.AboutLegalPage;
import com.instamall.pages.profile.AboutLegalPageLocators;
import com.instamall.pages.profile.HelpSupportPage;
import com.instamall.pages.profile.ProfilePage;
import com.instamall.pages.profile.ProfilePageLocators;
import com.instamall.waits.WaitUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * DISCOVERY, third pass — the three interactions the first ProfileTest
 * run showed behave differently than assumed:
 *
 *  - tapping an FAQ question (no new visible StaticText appeared)
 *  - tapping the Virtual Try-On switch (its value stayed "0")
 *  - tapping LOGOUT (onboarding never appeared — a confirmation?)
 *
 * Each is performed once and the resulting tree dumped. LOGOUT is dumped
 * but not confirmed if a dialog appears.
 */
public class ProfileDiscoveryPhase3Test extends BaseTest {

    private static final Path EXPLORATION = Paths.get("test-output/exploration");

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    private final ProfileEntryFlow entryFlow = new ProfileEntryFlow();
    private final ProfilePage profilePage = new ProfilePage();
    private final HelpSupportPage helpSupportPage = new HelpSupportPage();
    private final AboutLegalPage aboutLegalPage = new AboutLegalPage();

    @BeforeClass(alwaysRun = true)
    public void prepareApp() {

        AppSession.clearAppData();
        AppSession.grantLocationAndNotifications();
        AppSession.setSimulatedLocation(MIAMI_LATITUDE, MIAMI_LONGITUDE);
    }

    @Test(description = "DISCOVERY: FAQ tap, Try-On switch tap, LOGOUT tap")
    public void captureInteractionOutcomes() {

        entryFlow.openProfileTab();

        // ---- FAQ question tap ----

        profilePage.openHelpAndSupport();
        helpSupportPage.waitForScreen().tapQuestion("How do I track my order?");

        pause();
        dump("faq-after-question-tap");

        backToProfileTab();

        // ---- Virtual Try-On switch tap ----

        profilePage.waitForScreen().openAboutAndLegal();
        aboutLegalPage.waitForScreen();

        System.out.println(">>> Try-On before tap: " + aboutLegalPage.getVirtualTryOnState());

        aboutLegalPage.toggleVirtualTryOn();
        pause();

        System.out.println(">>> Try-On after switch tap: " + aboutLegalPage.getVirtualTryOnState());
        dump("about-legal-after-switch-tap");

        // On the redesigned build the tap raises a "Turn off AI Try-On?"
        // confirmation instead of flipping the switch; the dump above has
        // captured it, so keep things as they are and carry on. Then also
        // try tapping the row label.
        if (aboutLegalPage.isConsentConfirmationShowing()) {

            System.out.println(">>> Switch tap raised the consent confirmation — keeping consent as is.");
            aboutLegalPage.keepConsentAsIs();
        }

        aboutLegalPage.waitForScreen();

        DriverManager.getDriver()
                .findElement(AboutLegalPageLocators.VIRTUAL_TRY_ON_LABEL)
                .click();
        pause();

        System.out.println(">>> Try-On after label tap: " + aboutLegalPage.getVirtualTryOnState());
        dump("about-legal-after-label-tap");

        if (aboutLegalPage.isConsentConfirmationShowing()) {

            aboutLegalPage.keepConsentAsIs();
        }

        backToProfileTab();

        // ---- LOGOUT tap (dump only, never confirm) ----

        profilePage.waitForScreen().tapLogout();
        pause();

        dump("profile-after-logout-tap");
    }

    // ---------- helpers ----------

    private void backToProfileTab() {

        WaitUtils.waitForClickable(
                HomePageLocators.TAB_PROFILE, Duration.ofSeconds(10)).click();

        // Presence, not visibility: the Account list keeps its scroll
        // position on the way back, so "My profile" is often off the top.
        WaitUtils.waitForPresence(
                ProfilePageLocators.MENU_MY_PROFILE, Duration.ofSeconds(15));
    }

    private void pause() {

        try {

            Thread.sleep(2000);

        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();
        }
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

}
