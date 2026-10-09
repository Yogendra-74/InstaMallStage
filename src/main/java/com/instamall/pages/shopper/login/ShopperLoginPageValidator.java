package com.instamall.pages.shopper.login;

import com.instamall.base.BasePage;

public class ShopperLoginPageValidator extends BasePage {

    public boolean isEmailFieldDisplayed() {

        return isPresent(ShopperLoginPageLocators.EMAIL_TEXTBOX);
    }

    public boolean isPasswordFieldDisplayed() {

        return isPresent(ShopperLoginPageLocators.PASSWORD_TEXTBOX);
    }

    public boolean isLogInButtonDisplayed() {

        return isDisplayed(ShopperLoginPageLocators.LOG_IN_BUTTON);
    }

}
