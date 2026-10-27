package com.instamall.pages.login;

import com.instamall.base.BasePage;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public class LoginPage extends BasePage {

    // The default 20s wait was confirmed live, repeatedly, to be too
    // short for this specific transition under current staging
    // conditions — this screen opens off a navigation, not a cold start,
    // so it was never given the longer allowance a first paint gets, but
    // timed out here just the same even with the tap itself confirmed
    // landing (pressAndRelease, no change in outcome). Matches the same
    // order of magnitude as a first-paint allowance rather than inventing
    // a new one.
    private static final Duration SCREEN_READY_TIMEOUT = Duration.ofSeconds(45);

    /**
     * Reliable "screen is ready" gate. Send code is unique + accessible=true,
     * so it's the best anchor. The TextField's WDA visibility flag is flaky,
     * so we only require presence for it.
     *
     * Polls for App Tracking Transparency while it waits — confirmed live,
     * twice, that it can arrive right as this screen is opening and sit
     * there uncleared, the same shape BaseTest.awaitRelaunchFirstScreen()'s
     * own comment describes for a fresh relaunch. Left unanswered it blocks this wait for its own
     * full timeout with no indication why.
     */
    public LoginPage waitForScreen() {

        WaitUtils.waitUntil(driver -> {

            SystemAlerts.grantAnyShowingNow();

            return WaitUtils.isVisibleNow(LoginPageLocators.SEND_OTP_BUTTON) ? Boolean.TRUE : null;

        }, SCREEN_READY_TIMEOUT);

        WaitUtils.waitForPresence(LoginPageLocators.PHONE_NUMBER_TEXTBOX);

        return this;
    }

    public LoginPage enterPhoneNumber(String phoneNumber) {

        // Presence (not visibility) — WDA reports visible=true unreliably
        // for this nested RN input even when it is on screen.
        WebElement field =
                WaitUtils.waitForPresence(LoginPageLocators.PHONE_NUMBER_TEXTBOX);

        // One digit per call. The masked input drops digits when a whole
        // number arrives in a single sendKeys — an open app bug. Typing
        // digit by digit is what a real user does anyway, so the tests
        // measure screen behaviour instead of failing on it.
        for (char digit : phoneNumber.toCharArray()) {

            field.sendKeys(String.valueOf(digit));
        }

        return this;
    }

    public LoginPage clearPhoneNumber() {

        WaitUtils.waitForPresence(LoginPageLocators.PHONE_NUMBER_TEXTBOX)
                .clear();

        return this;
    }

    /**
     * Ticks the messaging-consent box, leaving it alone if it is already
     * ticked. Send code stays disabled without it, whatever the number.
     */
    public LoginPage acceptConsent() {

        WebElement checkbox =
                WaitUtils.waitForPresence(LoginPageLocators.CONSENT_CHECKBOX);

        if (!isChecked(checkbox)) {

            // A plain click() was confirmed live to report success
            // without ever actually toggling this control — see
            // BasePage.pressAndRelease()'s own doc comment.
            pressAndRelease(LoginPageLocators.CONSENT_CHECKBOX);
        }

        return this;
    }

    static boolean isChecked(WebElement checkbox) {

        String value = checkbox.getAttribute("value");

        return value != null && value.endsWith(", checked");
    }

    public LoginPage clickContinueWithEmail() {

        click(LoginPageLocators.CONTINUE_WITH_EMAIL);

        return this;
    }

    public LoginPage clickGoBack() {

        click(LoginPageLocators.GO_BACK);

        return this;
    }

}