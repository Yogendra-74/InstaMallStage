package com.instamall.tests.explore;

import com.instamall.base.ShopperBaseTest;
import com.instamall.config.ConfigReader;
import com.instamall.driver.DriverManager;
import com.instamall.pages.shopper.home.ShopperHomePage;
import com.instamall.pages.shopper.login.ShopperLoginPage;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * TEMPORARY — not part of the suite. Testing a hypothesis from the last
 * run: the "IN PROGRESS" card that errored "You are not authorized to
 * access this order" is an XCUIElementTypeOther (no traits at all) — not
 * a Button, unlike the "NEW ORDER" cards under "UP NEXT", which are
 * XCUIElementTypeButton. If tapping an actual Button card opens cleanly,
 * that confirms the earlier error came from tapping a non-interactive
 * status display rather than from an account/assignment problem.
 */
public class ShopperAppDiscoveryTest extends ShopperBaseTest {

    private final ShopperLoginPage loginPage = new ShopperLoginPage();
    private final ShopperHomePage homePage = new ShopperHomePage();

    @Test
    public void tapAnUpNextButtonCard() throws IOException, InterruptedException {

        Path outDir = Paths.get("test-output/exploration");
        Files.createDirectories(outDir);

        loginPage.waitForScreen()
                 .enterEmail(ConfigReader.get("shopperEmail"))
                 .enterPassword(ConfigReader.get("shopperPassword"))
                 .clickLogIn();

        homePage.waitForScreen();
        dump(outDir, "shopper-app-dashboard-before-tap");

        By newOrderButtonCard = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS 'NEW ORDER' AND accessible == 1");

        try {

            DriverManager.getDriver().findElement(newOrderButtonCard).click();

            Thread.sleep(1500);
            dump(outDir, "shopper-app-up-next-detail");

        } catch (Exception tappingCardFailed) {

            System.out.println(">>> Could not tap the button card: " + tappingCardFailed);
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
