package com.instamall.tests.explore;

import com.instamall.base.BaseTest;
import com.instamall.config.ConfigReader;
import com.instamall.driver.DriverManager;
import com.instamall.pages.home.HomePage;
import com.instamall.pages.login.EmailLoginPage;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * TEMPORARY — not part of the suite. Re-run whenever an order's stage has
 * just been advanced manually, to capture what the customer side shows for
 * Model 2. Dumps Home (where the live order-in-progress card lives) and,
 * if a card mentioning the order is found, its detail/tracking screen.
 */
public class LiveOrderTrackingExplorationTest extends BaseTest {

    private final OnboardingPage onboardingPage = new OnboardingPage();
    private final OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();
    private final LoginPage loginPage = new LoginPage();
    private final EmailLoginPage emailLoginPage = new EmailLoginPage();
    private final HomePage homePage = new HomePage();

    @Test
    public void checkLiveOrder() throws IOException, InterruptedException {

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

        homePage.waitForScreen();
        Thread.sleep(2000);

        dump(outDir, "src_home_live_check.xml");

        By anyOrderCard = AppiumBy.iOSNsPredicateString(
                "(name CONTAINS 'IM-R7CE0' OR name CONTAINS 'IM-FX0PH'"
                        + " OR name BEGINSWITH 'Confirming your order') AND accessible == 1");

        List<WebElement> allCards = DriverManager.getDriver().findElements(anyOrderCard);

        System.out.println(">>> Order-related elements on Home: " + allCards.size());

        for (WebElement card : allCards) {

            System.out.println(">>> Home card: " + card.getAttribute("name"));
        }

        openAndDump("IM-R7CE0", outDir, "src_r7ce0_detail.xml");
        openAndDump("IM-FX0PH", outDir, "src_fx0ph_detail.xml");
    }

    /**
     * Opens whichever Home card mentions the order id, dumps its detail
     * screen, then relaunches back to a clean Home so the next lookup
     * starts fresh.
     */
    private void openAndDump(String orderId, Path outDir, String fileName)
            throws IOException, InterruptedException {

        By card = AppiumBy.iOSNsPredicateString(
                "name CONTAINS '" + orderId + "' AND accessible == 1");

        List<WebElement> matches = DriverManager.getDriver().findElements(card);

        if (matches.isEmpty()) {

            System.out.println(">>> No Home card found for " + orderId
                    + " — trying the generic \"Confirming your order\" card instead");

            matches = DriverManager.getDriver().findElements(
                    AppiumBy.iOSNsPredicateString(
                            "name BEGINSWITH 'Confirming your order' AND accessible == 1"));
        }

        if (matches.isEmpty()) {

            System.out.println(">>> Still nothing on Home for " + orderId + " — skipping");
            return;
        }

        matches.get(0).click();
        Thread.sleep(1500);
        dump(outDir, fileName);

        String bundleId = ConfigReader.get("bundleId");
        DriverManager.getDriver().terminateApp(bundleId);
        DriverManager.getDriver().activateApp(bundleId);
        Thread.sleep(3000);
        homePage.waitForScreen();
        Thread.sleep(2000);
    }

    private void dump(Path dir, String name) throws IOException {

        String source = DriverManager.getDriver().getPageSource();
        Files.writeString(dir.resolve(name), source);
        System.out.println(">>> Dumped " + name + " (" + source.length() + " chars)");
    }

}
