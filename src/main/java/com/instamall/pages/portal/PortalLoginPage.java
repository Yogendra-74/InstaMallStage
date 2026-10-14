package com.instamall.pages.portal;

import com.instamall.base.PortalBasePage;
import com.instamall.waits.PortalWaitUtils;

import java.time.Duration;

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
     * How long a person gets to solve the reCAPTCHA by hand before the
     * Login tap is attempted — the plain click()'s 20s default was not
     * enough to reach the browser and finish a challenge. Kept under the
     * couple of minutes the portal gives a solved challenge before it
     * expires (see PortalLoginPageLocators.RECAPTCHA_EXPIRED_MESSAGE).
     */
    private static final Duration MANUAL_RECAPTCHA_TIMEOUT = Duration.ofMinutes(3);

    /**
     * Does not attempt the reCAPTCHA itself — that stays a manual, human
     * step every run (see PortalDriverFactory's own doc comment for why).
     * Waits up to {@link #MANUAL_RECAPTCHA_TIMEOUT} for a person to solve
     * it and the Login button to enable, then taps it.
     */
    public PortalLoginPage clickLogin() {

        System.out.println(">>> Solve the reCAPTCHA in the browser now — waiting up to "
                + MANUAL_RECAPTCHA_TIMEOUT.toMinutes() + " minutes for the Login button to enable.");

        PortalWaitUtils.waitForClickable(PortalLoginPageLocators.LOGIN_BUTTON,
                MANUAL_RECAPTCHA_TIMEOUT).click();

        return this;
    }

}
