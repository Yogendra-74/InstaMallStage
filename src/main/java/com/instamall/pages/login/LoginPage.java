package com.instamall.pages.login;

import com.instamall.base.BasePage;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebElement;

public class LoginPage extends BasePage {

    /**
     * Reliable "screen is ready" gate. Send code is unique + accessible=true,
     * so it's the best anchor. The TextField's WDA visibility flag is flaky,
     * so we only require presence for it.
     *
     * Polls for App Tracking Transparency while it waits — confirmed live,
     * twice, that it can arrive right as this screen is opening (straight
     * after OnboardingPage.clickLogin()) and sit there uncleared, the same
     * shape BaseTest.awaitRelaunchFirstScreen()'s own comment describes for
     * a fresh relaunch. Left unanswered it blocks this wait for its own
     * full timeout with no indication why.
     */
    public LoginPage waitForScreen() {

        WaitUtils.waitUntil(driver -> {

            SystemAlerts.grantAnyShowingNow();

            return WaitUtils.isVisibleNow(LoginPageLocators.SEND_OTP_BUTTON) ? Boolean.TRUE : null;
        });

        WaitUtils.waitForPresence(LoginPageLocators.PHONE_NUMBER_TEXTBOX);

        return this;
    }

    public LoginPage enterPhoneNumber(String phoneNumber) {

        // Presence (not visibility) — WDA reports visible=true unreliably
        // for this nested RN input even when it is on screen.
        WebElement field =
                WaitUtils.waitForPresence(LoginPageLocators.PHONE_NUMBER_TEXTBOX);

        // One digit per call. The masked input drops digits when a whole
        // number arrives in a single sendKeys — that is an open app bug,
        // tracked by reproPhoneFieldDropsDigitOnFastInput in LoginTest.
        // Typing digit by digit is what a real user does anyway, so the
        // other tests measure screen behaviour instead of failing on it.
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

    public LoginPage clickSendOTP() {

        // Button starts disabled (traits="NotEnabled") until a valid
        // number is entered and the consent box is ticked — wait for it
        // to activate before tapping.
        WaitUtils.waitForEnabled(LoginPageLocators.SEND_OTP_BUTTON);

        click(LoginPageLocators.SEND_OTP_BUTTON);

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

            checkbox.click();
        }

        return this;
    }

    static boolean isChecked(WebElement checkbox) {

        String value = checkbox.getAttribute("value");

        return value != null && value.endsWith(", checked");
    }

    public LoginPage clickCountryCodePicker() {

        click(LoginPageLocators.COUNTRY_CODE);

        return this;
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