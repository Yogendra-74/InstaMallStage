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
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TEMPORARY — not part of the suite. Signs in with the existing testEmail
 * account (which already holds real staging orders from earlier runs) and
 * dumps page source for the Orders list and, if reachable, an order's
 * detail/tracking screen, so locators for Model 2 can be mapped without
 * placing a new order.
 */
public class OrdersScreenExplorationTest extends BaseTest {

    private final OnboardingPage onboardingPage = new OnboardingPage();
    private final OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();
    private final LoginPage loginPage = new LoginPage();
    private final EmailLoginPage emailLoginPage = new EmailLoginPage();
    private final HomePage homePage = new HomePage();
    private final ProfilePage profilePage = new ProfilePage();

    @Test
    public void dumpOrdersScreens() throws IOException, InterruptedException {

        Path outDir = Paths.get(
                "/private/tmp/claude-502/-Users-yogendra-Desktop-InstaMallAutomationSuite/"
                        + "579fde19-c72a-452f-9246-0b9f9400cb33/scratchpad");
        Files.createDirectories(outDir);

        if (onboardingValidator.isLoginButtonDisplayed()) {

            onboardingPage.clickLogin();
            loginPage.waitForScreen().clickContinueWithEmail();
            emailLoginPage.waitForScreen()
                          .enterEmail(ConfigReader.get("testEmail"))
                          .enterPassword(ConfigReader.get("testPassword"))
                          .clickNext();
        }

        homePage.waitForScreen().openProfile();
        profilePage.waitForScreen();

        dump(outDir, "src_profile.xml");

        WebElement ordersStat = DriverManager.getDriver()
                .findElement(ProfilePageLocators.STAT_ORDERS);
        ordersStat.click();

        Thread.sleep(2000);
        dump(outDir, "src_orders_list.xml");

        By fx0phRow = AppiumBy.iOSNsPredicateString(
                "name BEGINSWITH 'IM-FX0PH' AND accessible == 1");

        try {

            DriverManager.getDriver().findElement(fx0phRow).click();
            Thread.sleep(1500);
            dump(outDir, "src_order_detail.xml");

        } catch (Exception tappingOrderFailed) {

            System.out.println(">>> Could not tap the IM-FX0PH row: " + tappingOrderFailed);
        }

        // Relaunch to get back to a clean Home, and check whether it shows
        // a live order-in-progress card (a real order, as opposed to the
        // example history above). XCUITest has no back-button concept, so
        // relaunching is the reliable way back rather than navigate().back().
        String bundleId = ConfigReader.get("bundleId");
        DriverManager.getDriver().terminateApp(bundleId);
        DriverManager.getDriver().activateApp(bundleId);
        Thread.sleep(3000);

        homePage.waitForScreen();
        dump(outDir, "src_home_after_orders.xml");

        By returnCard = AppiumBy.iOSNsPredicateString(
                "name BEGINSWITH 'Return' AND accessible == 1");

        try {

            DriverManager.getDriver().findElement(returnCard).click();
            Thread.sleep(1500);
            dump(outDir, "src_return_detail.xml");

        } catch (Exception tappingReturnFailed) {

            System.out.println(">>> Could not tap the return card: " + tappingReturnFailed);
        }
    }

    private void dump(Path dir, String name) throws IOException {

        String source = DriverManager.getDriver().getPageSource();
        Files.writeString(dir.resolve(name), source);
        System.out.println(">>> Dumped " + name + " (" + source.length() + " chars)");
    }

}
