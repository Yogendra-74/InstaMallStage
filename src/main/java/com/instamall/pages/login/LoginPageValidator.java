package com.instamall.pages.login;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;


public class LoginPageValidator extends BasePage {

    public boolean isPhoneNumberFieldDisplayed() {

        // Presence, not visibility — WDA's visible flag is unreliable
        // for this nested RN TextField.
        return isPresent(LoginPageLocators.PHONE_NUMBER_TEXTBOX);
    }

    public boolean isSendOTPButtonDisplayed() {

        return isDisplayed(LoginPageLocators.SEND_OTP_BUTTON);
    }

    public boolean isSendOTPButtonEnabled() {

        return DriverManager.getDriver()
                .findElement(LoginPageLocators.SEND_OTP_BUTTON)
                .isEnabled();
    }

    public boolean isConsentChecked() {

        return LoginPage.isChecked(
                WaitUtils.waitForPresence(LoginPageLocators.CONSENT_CHECKBOX));
    }

    public boolean isContinueWithEmailDisplayed() {

        return isDisplayed(LoginPageLocators.CONTINUE_WITH_EMAIL);
    }

    public boolean isGoBackDisplayed() {

        return isDisplayed(LoginPageLocators.GO_BACK);
    }

}