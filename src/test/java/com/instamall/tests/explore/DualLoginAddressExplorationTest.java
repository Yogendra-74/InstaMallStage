package com.instamall.tests.explore;

import com.instamall.base.BaseTest;
import com.instamall.config.ConfigReader;
import com.instamall.driver.AppSession;
import com.instamall.driver.DriverManager;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.location.SelectLocationSheet;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import org.openqa.selenium.OutputType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * TEMPORARY — not part of the suite. dual.login@yopmail.com is a fresh
 * Customer account with no saved addresses, unlike the previous shared
 * account — placeOrderWithSavedCard failed on "Z9 VENTURES" not being
 * listed. This dumps the address sheet to see what a brand-new account's
 * sheet actually offers (empty state? quick slots? nothing at all?).
 *
 * Left at autoDismissAlerts's default (true), matching EndToEndOrderTest:
 * permissions are pre-granted via AppSession below (simctl-level, no UI
 * alert involved), so there is nothing for autoDismissAlerts=false to buy
 * here.
 *
 * Also calls AppSession.clearAppData() before anything else, matching
 * EndToEndOrderTest — both because this test wants a genuinely
 * signed-out start (it does its own login below) and because skipping it
 * was confirmed to reproduce a WDA proxy failure on resetApp()'s first
 * terminateApp() call: with useNewWDA=false reusing an already-running
 * WDA, resetApp()'s terminateApp() became the session's first
 * app-management command and consistently hit connect ECONNREFUSED
 * 127.0.0.1:8100. clearAppData() also goes through WDA's app-management
 * channel, and running it first in @BeforeClass absorbs that — every
 * EndToEndOrderTest run on the same simulator has it first and never hit
 * the failure.
 */
public class DualLoginAddressExplorationTest extends BaseTest {

    private final OnboardingPage onboardingPage = new OnboardingPage();
    private final LoginPage loginPage = new LoginPage();
    private final EmailLoginPage emailLoginPage = new EmailLoginPage();
    private final HomePage homePage = new HomePage();
    private final SelectLocationSheet locationSheet = new SelectLocationSheet();

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    @BeforeClass(alwaysRun = true)
    public void prepareLocation() {

        AppSession.clearAppData();
        AppSession.grantLocationAndNotifications();
        AppSession.setSimulatedLocation(MIAMI_LATITUDE, MIAMI_LONGITUDE);
    }

    @Test
    public void dumpAddressSheetForFreshAccount() throws IOException, InterruptedException {

        Path outDir = Paths.get("test-output/exploration");
        Files.createDirectories(outDir);

        onboardingPage.clickLogin();
        loginPage.waitForScreen().clickContinueWithEmail();
        emailLoginPage.waitForScreen()
                      .enterEmail(ConfigReader.get("testEmail"))
                      .enterPassword(ConfigReader.get("testPassword"))
                      .clickNext();

        Thread.sleep(3000);
        dump(outDir, "dual-login-home-or-empty-state");

        try {

            homePage.openLocationSelector();

        } catch (Exception openChipFailed) {

            homePage.openLocationSelectorFromEmptyState();
        }

        locationSheet.waitForScreen();
        Thread.sleep(1000);
        dump(outDir, "dual-login-address-sheet");
    }

    private void dump(Path dir, String baseName) throws IOException {

        String source = DriverManager.getDriver().getPageSource();
        Files.writeString(dir.resolve(baseName + ".xml"), source);
        System.out.println(">>> Dumped " + baseName + ".xml (" + source.length() + " chars)");

        File screenshot = DriverManager.getDriver().getScreenshotAs(OutputType.FILE);
        Path target = dir.resolve(baseName + ".png");
        Files.copy(screenshot.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        System.out.println(">>> Screenshot saved: " + target.toAbsolutePath());
    }

}
