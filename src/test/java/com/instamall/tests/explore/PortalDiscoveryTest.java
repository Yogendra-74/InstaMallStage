package com.instamall.tests.explore;

import com.instamall.base.PortalBaseTest;
import com.instamall.config.ConfigReader;
import com.instamall.driver.PortalDriverManager;
import com.instamall.pages.portal.PortalOrdersPage;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * TEMPORARY — not part of the suite. Discovery pass on the Delivery
 * Portal: log in, land on Orders, and capture what is there to design the
 * real PortalLoginPage / PortalOrdersPage page objects from.
 */
public class PortalDiscoveryTest extends PortalBaseTest {

    @Test
    public void loginAndDumpOrdersPage() throws IOException, InterruptedException {

        Path outDir = Paths.get("test-output/exploration");
        Files.createDirectories(outDir);

        dump(outDir, "portal-login-screen");

        // Login fields have no known locators yet — try the obvious ones
        // first; if these miss, the dumped source above shows the real
        // markup to target instead.
        try {

            new WebDriverWait(PortalDriverManager.getDriver(), Duration.ofSeconds(15))
                    .until(driver -> driver.findElement(By.cssSelector(
                            "input[type='email'], input[name='email'], input[name='username']")));

            PortalDriverManager.getDriver()
                    .findElement(By.cssSelector("input[type='email'], input[name='email'], input[name='username']"))
                    .sendKeys(ConfigReader.get("deliveryPortalUsername"));

            PortalDriverManager.getDriver()
                    .findElement(By.cssSelector("input[type='password'], input[name='password']"))
                    .sendKeys(ConfigReader.get("deliveryPortalPassword"));

            dump(outDir, "portal-login-filled");

            // reCAPTCHA stays a manual, human step (see PortalDriverFactory's
            // doc comment) — pause here so it can be solved by hand before
            // the login click fires.
            Thread.sleep(20000);

            PortalDriverManager.getDriver()
                    .findElement(By.cssSelector("button[type='submit'], button"))
                    .click();

            Thread.sleep(3000);
            dump(outDir, "portal-after-login");

            new PortalOrdersPage().openOrdersFromSidebar();

            Thread.sleep(2000);
            dump(outDir, "portal-orders-page");

        } catch (Exception loginAttemptFailed) {

            System.out.println(">>> Login attempt via guessed selectors failed: " + loginAttemptFailed);
            dump(outDir, "portal-login-attempt-failed");
        }
    }

    private void dump(Path dir, String baseName) throws IOException {

        String source = PortalDriverManager.getDriver().getPageSource();
        Files.writeString(dir.resolve(baseName + ".html"), source);
        System.out.println(">>> Dumped " + baseName + ".html (" + source.length() + " chars)");

        File screenshot = ((org.openqa.selenium.TakesScreenshot) PortalDriverManager.getDriver())
                .getScreenshotAs(OutputType.FILE);
        Path target = dir.resolve(baseName + ".png");
        Files.copy(screenshot.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        System.out.println(">>> Screenshot saved: " + target.toAbsolutePath());
    }

}
