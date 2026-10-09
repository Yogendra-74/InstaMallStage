package com.instamall.pages.login;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;

public class EmailLoginPageValidator extends BasePage {

    public boolean isEmailFieldDisplayed() {

        // Presence, not visibility — nested RN input.
        return isPresent(EmailLoginPageLocators.EMAIL_TEXTBOX);
    }

    public boolean isPasswordFieldDisplayed() {

        return isPresent(EmailLoginPageLocators.PASSWORD_TEXTBOX);
    }

    public boolean isNextButtonDisplayed() {

        return isDisplayed(EmailLoginPageLocators.NEXT_BUTTON);
    }

    public boolean isNextButtonEnabled() {

        return DriverManager.getDriver()
                .findElement(EmailLoginPageLocators.NEXT_BUTTON)
                .isEnabled();
    }

    public boolean isContinueWithPhoneDisplayed() {

        return isDisplayed(EmailLoginPageLocators.CONTINUE_WITH_PHONE);
    }

    public boolean isInvalidCredentialsErrorDisplayed() {

        return isDisplayed(EmailLoginPageLocators.INVALID_CREDENTIALS_ERROR);
    }

}
