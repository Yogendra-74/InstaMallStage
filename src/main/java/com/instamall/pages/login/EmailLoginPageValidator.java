package com.instamall.pages.login;

import com.instamall.base.BasePage;

public class EmailLoginPageValidator extends BasePage {

    public boolean isEmailFieldDisplayed() {

        // Presence, not visibility — nested RN input.
        return isPresent(EmailLoginPageLocators.EMAIL_TEXTBOX);
    }

    public boolean isPasswordFieldDisplayed() {

        return isPresent(EmailLoginPageLocators.PASSWORD_TEXTBOX);
    }

}
