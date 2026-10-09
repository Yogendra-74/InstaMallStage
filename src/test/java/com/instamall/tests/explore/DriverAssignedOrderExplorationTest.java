package com.instamall.tests.explore;

import com.instamall.base.BaseTest;
import com.instamall.config.ConfigReader;
import com.instamall.driver.DriverManager;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.pages.profile.ProfilePage;
import com.instamall.pages.profile.ProfilePageLocators;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * TEMPORARY — not part of the suite. Order IM-Z8FBB was advanced past
 * "Order confirmed" from the Shopper app, into "Waiting for Driver". This
 * dumps the home screen and that order's tracking screen so the new stage's
 * elements can be mapped into OrderConfirmationPageLocators, which so far
 * only covers "Order received" and "Order confirmed".
 */
public class DriverAssignedOrderExplorationTest extends BaseTest {

    private static final String ORDER_ID = "IM-Z8FBB";

    private final OnboardingPage onboardingPage = new OnboardingPage();
    private final OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();
    private final LoginPage loginPage = new LoginPage();
    private final EmailLoginPage emailLoginPage = new EmailLoginPage();
    private final HomePage homePage = new HomePage();
    private final ProfilePage profilePage = new ProfilePage();

    @Test
    public void dumpWaitingForDriverScreens() throws IOException, InterruptedException {

        Path outDir = Paths.get("test-output/exploration");
        Files.createDirectories(outDir);

        if (onboardingValidator.isLoginButtonDisplayed()) {

            onboardingPage.clickLogin();
            loginPage.waitForScreen().clickContinueWithEmail();
            emailLoginPage.waitForScreen()
                          .enterEmail(ConfigReader.get("testEmail"))
                          .enterPassword(ConfigReader.get("testPassword"))
                          .clickNext();
        }

        homePage.waitForScreen();
        Thread.sleep(2000);
        dump(outDir, "home-with-order-waiting-for-driver");

        homePage.openProfile();
        profilePage.waitForScreen();

        WebElement ordersStat = DriverManager.getDriver()
                .findElement(ProfilePageLocators.STAT_ORDERS);
        ordersStat.click();

        Thread.sleep(2000);
        dump(outDir, "orders-list-with-waiting-for-driver");

        By orderRow = AppiumBy.iOSNsPredicateString(
                "name BEGINSWITH '" + ORDER_ID + "' AND accessible == 1");

        try {

            DriverManager.getDriver().findElement(orderRow).click();
            Thread.sleep(1500);
            dump(outDir, "order-detail-waiting-for-driver");

        } catch (Exception tappingOrderFailed) {

            System.out.println(">>> Could not tap the " + ORDER_ID + " row: " + tappingOrderFailed);
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
