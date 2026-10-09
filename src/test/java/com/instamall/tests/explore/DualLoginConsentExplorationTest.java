package com.instamall.tests.explore;

import com.instamall.base.BaseTest;
import com.instamall.config.ConfigReader;
import com.instamall.driver.AppSession;
import com.instamall.driver.DriverManager;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.EmailLoginPageLocators;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.waits.WaitUtils;
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
 * TEMPORARY — not part of the suite. placeOrderWithSavedCard failed
 * waiting for "Agree & Continue" to become clickable, right after
 * dual.login@yopmail.com's first-ever sign in. This signs in the same way
 * and dumps the screen the moment that prompt is seen, without going
 * through ConsentPrompts at all, so its actual current shape can be
 * mapped.
 */
public class DualLoginConsentExplorationTest extends BaseTest {

    private final OnboardingPage onboardingPage = new OnboardingPage();
    private final LoginPage loginPage = new LoginPage();
    private final EmailLoginPage emailLoginPage = new EmailLoginPage();

    @BeforeClass(alwaysRun = true)
    public void prepareSignedOutApp() {

        AppSession.clearAppData();
        AppSession.grantLocationAndNotifications();
    }

    @Test
    public void dumpConsentPromptScreen() throws IOException, InterruptedException {

        Path outDir = Paths.get("test-output/exploration");
        Files.createDirectories(outDir);

        onboardingPage.clickLogin();
        loginPage.waitForScreen().clickContinueWithEmail();
        emailLoginPage.waitForScreen()
                      .enterEmail(ConfigReader.get("testEmail"))
                      .enterPassword(ConfigReader.get("testPassword"));

        // Dismiss the keyboard and tap Continue directly — bypassing
        // EmailLoginPage.clickNext(), which is what calls the
        // ConsentPrompts logic that got stuck.
        emailLoginPage.dismissKeyboard();
        emailLoginPage.acceptConsent();

        WaitUtils.waitForEnabled(EmailLoginPageLocators.NEXT_BUTTON);
        DriverManager.getDriver()
                .findElement(EmailLoginPageLocators.NEXT_BUTTON)
                .click();

        // Give whatever comes up after sign-in time to appear, then dump
        // repeatedly so the sequence of screens/sheets is visible.
        for (int i = 1; i <= 6; i++) {

            Thread.sleep(5000);
            dump(outDir, "dual-login-post-signin-" + i);
        }
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
