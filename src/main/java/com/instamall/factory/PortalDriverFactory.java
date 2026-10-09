package com.instamall.factory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Collections;
import java.util.Map;

/**
 * Starts a browser session for the Delivery Portal.
 *
 * Requires the machine's regular Chrome to be fully quit before a run:
 * macOS enforces Chrome as a single-instance app, so a second, separately-
 * flagged Chrome (this automated one) gets killed shortly after launch
 * once macOS resolves it against an already-running instance — confirmed
 * live. Firefox was tried as a same-machine workaround but hit the same
 * conflict (a long-running Firefox was also already open), so this stays
 * on Chrome rather than adding a second driver path for no real gain.
 *
 * excludeSwitches/useAutomationExtension/disable-blink-features strip the
 * obvious WebDriver fingerprints (the "Chrome is being controlled by
 * automated test software" banner, navigator.webdriver) — the portal's
 * login independently score-checks the session via reCAPTCHA and
 * rejected an otherwise-correctly-solved challenge as "suspicious"
 * without this, confirmed live. This still never attempts to solve or
 * otherwise defeat the CAPTCHA challenge itself — that stays a manual,
 * human step every time.
 *
 * No driver-binary setup needed: Selenium Manager (built into
 * selenium-java since 4.6) resolves and downloads a matching chromedriver
 * on its own the first time this runs.
 */
public final class PortalDriverFactory {

    private PortalDriverFactory() {

    }

    public static WebDriver initializeDriver() {

        ChromeOptions options = new ChromeOptions();

        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        options.setExperimentalOption("prefs", Map.of(
                "credentials_enable_service", false,
                "profile.password_manager_enabled", false,
                // 1 = allow, 2 = block
                "profile.default_content_setting_values.notifications", 1
        ));
        options.addArguments(
                "--start-maximized",
                "--disable-save-password-bubble",
                "--disable-infobars",
                "--disable-blink-features=AutomationControlled"
        );

        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();

        return driver;
    }

}
