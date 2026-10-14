package com.instamall.pages.shopper.login;

import com.instamall.base.BasePage;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.driver.DriverManager;
import com.instamall.pages.shopper.home.ShopperHomePage;
import com.instamall.pages.shopper.home.ShopperHomePageLocators;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public class ShopperLoginPage extends BasePage {

    /**
     * Cold sign-in against the backend, seen to take several seconds — and
     * with every Shopper app test now granting rather than auto-declining
     * permission prompts, a genuinely fresh login (no password saved yet)
     * can queue up Save Password, then a location prompt, then a
     * notifications prompt, each needing to render and settle before the
     * next appears. 20s was seen to time out mid-sequence, with
     * Notifications still not yet shown; widened to give the full
     * sequence room on a cold run.
     */
    private static final Duration DASHBOARD_APPEARS_WITHIN = Duration.ofSeconds(45);

    /**
     * Retries for the character-by-character fallback in typeInto() — a
     * late-arriving permission alert can cover the field mid-retry, so
     * one retry is not always enough to outlast it.
     */
    private static final int TYPE_ATTEMPTS = 3;

    public ShopperLoginPage waitForScreen() {

        waitForVisibility(ShopperLoginPageLocators.LOG_IN_BUTTON);
        WaitUtils.waitForPresence(ShopperLoginPageLocators.EMAIL_TEXTBOX);
        WaitUtils.waitForPresence(ShopperLoginPageLocators.PASSWORD_TEXTBOX);

        return this;
    }

    public ShopperLoginPage enterEmail(String email) {

        typeInto(ShopperLoginPageLocators.EMAIL_TEXTBOX, email, false);

        return this;
    }

    public ShopperLoginPage enterPassword(String password) {

        typeInto(ShopperLoginPageLocators.PASSWORD_TEXTBOX, password, true);

        return this;
    }

    /**
     * Just the tap on "Log in" — no prompt handling and no wait for the
     * dashboard, for a flow that answers each permission alert that
     * follows as its own explicit step (Single_Order_Complete's Phase 2:
     * confirmed live, the Notifications alert is the first thing up
     * after this tap, with "Don't Allow" and "Allow" as its buttons).
     * {@link #clickLogIn()} is the all-in-one form the other tests use.
     */
    public ShopperLoginPage tapLogIn() {

        WaitUtils.waitForEnabled(ShopperLoginPageLocators.LOG_IN_BUTTON);

        click(ShopperLoginPageLocators.LOG_IN_BUTTON);

        return this;
    }

    public ShopperLoginPage clickLogIn() {

        WaitUtils.waitForEnabled(ShopperLoginPageLocators.LOG_IN_BUTTON);

        SystemAlerts.grantAnyShowingNow();

        click(ShopperLoginPageLocators.LOG_IN_BUTTON);

        awaitDashboardPastPrompts();

        return this;
    }

    /**
     * Waits for the dashboard, granting whatever permission prompts turn
     * up along the way — notifications, location, and App Tracking
     * Transparency, in whatever order they arrive. All of them: per
     * instruction, this flow allows every permission the app asks for,
     * ATT included.
     *
     * Polls for both rather than a single race that stops the moment the
     * dashboard is found: with autoDismissAlerts=false (needed so a
     * caller can grant these instead of having them auto-denied) a real
     * alert is a genuine modal and can otherwise block the dashboard from
     * ever becoming interactable while this waits on it doing nothing
     * about the alert sitting on top.
     *
     * Delegates each pass to SystemAlerts.grantAnyShowingNow(), which
     * accepts through the driver's own alert API rather than locating and
     * clicking a button as a regular element — confirmed live that the
     * latter can find an alert's button and still silently fail to
     * register the tap, since the alert belongs to springboard, not the
     * app — the same helper ShopperBaseTest.awaitRelaunchFirstScreen()
     * polls with, so a prompt that happens to show up right after
     * relaunch rather than here is still answered before it can sit
     * there blocking later tests.
     *
     * Also answers "Go on shift?" — an app-level sheet, not a system
     * alert, so SystemAlerts never touches it. Confirmed live: a shopper
     * who logs in offline is shown this before anything else, and every
     * element behind it (including what ShopperHomePage.isDisplayedNow()
     * checks) reports visible == false for as long as it is up, so a
     * login that does not answer it times out here with no indication
     * why. Answered with "Go on shift" rather than "Skip for now" — an
     * offline shopper cannot receive or act on orders, which is the
     * entire point of this login.
     */
    private void awaitDashboardPastPrompts() {

        try {

            WaitUtils.waitUntil(driver -> {

                SystemAlerts.grantAnyShowingNow();

                if (WaitUtils.isVisibleNow(ShopperHomePageLocators.GO_ON_SHIFT_BUTTON)) {

                    driver.findElement(ShopperHomePageLocators.GO_ON_SHIFT_BUTTON).click();
                }

                return ShopperHomePage.isDisplayedNow() ? Boolean.TRUE : null;

            }, DASHBOARD_APPEARS_WITHIN);

        } catch (org.openqa.selenium.TimeoutException dashboardNeverAppeared) {

            dumpFailureState("dashboard-timeout");
            throw dashboardNeverAppeared;
        }
    }

    public ShopperLoginPage clickForgotPassword() {

        click(ShopperLoginPageLocators.FORGOT_PASSWORD);

        return this;
    }

    /**
     * Fills a field, verifies it landed, and only then falls back to a
     * character at a time.
     *
     * Borrowed from the Customer app's EmailLoginPage: a bulk sendKeys can
     * drop a character here too (confirmed — an email arrived as
     * "rtest@yopmail.com" instead of "rk.test@yopmail.com"), so a field is
     * always read back rather than trusted.
     *
     * @param masked true where the field hands back bullets instead of its
     *               contents, so only the count can be compared
     */
    private void typeInto(By locator, String text, boolean masked) {

        // A permission alert (e.g. notifications) can arrive at any point
        // here, seconds after this screen's own fields are already
        // visible, and silently swallow a click/sendKeys aimed at a field
        // it covers — confirmed live. Cleared before every attempt below.
        SystemAlerts.grantAnyShowingNow();

        WebElement field = WaitUtils.waitForPresence(locator);

        field.click();
        field.sendKeys(text);

        if (landed(field, text, masked)) {

            return;
        }

        System.out.println(">>> Bulk input did not land in the "
                + (masked ? "password" : "email")
                + " field — retyping one character at a time.");

        for (int attempt = 1; attempt <= TYPE_ATTEMPTS; attempt++) {

            // Refetched fresh every attempt, not reused across retries:
            // confirmed live that a WebElement handle captured once (as
            // this used to do) can go stale after the field re-renders
            // following a failed attempt, silently no-oping every
            // click()/sendKeys() call after that without ever throwing —
            // manually replaying the exact same bulk-then-char-by-char
            // sequence with a fresh find() before each step landed the
            // text correctly first try, where reusing one handle across
            // three full retries never landed a single character.
            SystemAlerts.grantAnyShowingNow();
            WebElement retryField = WaitUtils.waitForPresence(locator);

            retryField.click();
            retryField.clear();

            for (char character : text.toCharArray()) {

                SystemAlerts.grantAnyShowingNow();
                retryField.sendKeys(String.valueOf(character));
            }

            if (landed(retryField, text, masked)) {

                return;
            }

            System.out.println(">>> Char-by-char attempt " + attempt + " of " + TYPE_ATTEMPTS
                    + " did not land in the " + (masked ? "password" : "email") + " field.");
        }

        dumpFailureState(masked ? "password" : "email");

        throw new IllegalStateException(
                "Could not fill the " + (masked ? "password" : "email")
                        + " field even one character at a time — it reads \""
                        + WaitUtils.waitForPresence(locator).getAttribute("value") + "\"");
    }

    /**
     * A screenshot plus page source dump, taken at the exact moment this
     * page gives up on the dashboard or a field — checking afterward only
     * ever finds the app already backgrounded by the session's own
     * teardown, not whatever was actually on screen at the time.
     */
    private void dumpFailureState(String fieldName) {

        try {

            java.nio.file.Path outDir = java.nio.file.Paths.get("test-output/exploration");
            java.nio.file.Files.createDirectories(outDir);

            String source = DriverManager.getDriver().getPageSource();
            java.nio.file.Files.writeString(
                    outDir.resolve("shopper-login-" + fieldName + "-failure.xml"), source);

            java.io.File screenshot = DriverManager.getDriver()
                    .getScreenshotAs(org.openqa.selenium.OutputType.FILE);
            java.nio.file.Files.copy(screenshot.toPath(),
                    outDir.resolve("shopper-login-" + fieldName + "-failure.png"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            System.out.println(">>> Dumped failure state to "
                    + outDir.resolve("shopper-login-" + fieldName + "-failure.png").toAbsolutePath());

        } catch (Exception dumpFailed) {

            System.out.println(">>> Could not dump failure state: " + dumpFailed);
        }
    }

    private boolean landed(WebElement field, String text, boolean masked) {

        String value = field.getAttribute("value");

        if (value == null) {

            return false;
        }

        return masked ? value.length() == text.length() : value.equals(text);
    }

}
