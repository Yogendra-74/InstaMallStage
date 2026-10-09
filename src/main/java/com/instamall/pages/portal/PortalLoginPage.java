package com.instamall.pages.portal;

import com.instamall.base.PortalBasePage;

public class PortalLoginPage extends PortalBasePage {

    public PortalLoginPage waitForScreen() {

        waitForVisibility(PortalLoginPageLocators.EMAIL_FIELD);
        waitForVisibility(PortalLoginPageLocators.PASSWORD_FIELD);

        return this;
    }

    public PortalLoginPage enterEmail(String email) {

        type(PortalLoginPageLocators.EMAIL_FIELD, email);

        return this;
    }

    public PortalLoginPage enterPassword(String password) {

        type(PortalLoginPageLocators.PASSWORD_FIELD, password);

        return this;
    }

    /**
     * Does not attempt the reCAPTCHA itself — that stays a manual, human
     * step every run (see PortalDriverFactory's own doc comment for why).
     * Call this only once a person has solved it and the Login button
     * has actually enabled.
     */
    public PortalLoginPage clickLogin() {

        click(PortalLoginPageLocators.LOGIN_BUTTON);

        return this;
    }

    public boolean isRecaptchaShowing() {

        return isDisplayedNow(PortalLoginPageLocators.RECAPTCHA_CONTAINER);
    }

    public boolean isSuspiciousRecaptchaErrorShowing() {

        return isDisplayedNow(PortalLoginPageLocators.SUSPICIOUS_RECAPTCHA_ERROR);
    }

}
