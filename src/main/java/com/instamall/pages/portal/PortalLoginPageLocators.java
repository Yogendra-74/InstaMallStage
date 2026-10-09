package com.instamall.pages.portal;

import org.openqa.selenium.By;

/**
 * The Delivery Portal's login screen (an Angular app, hash-routed at
 * #/login). Verified against a live page source dump.
 *
 * The reCAPTCHA on this screen is a real, human-solved challenge every
 * time — see PortalBaseTest/PortalDriverFactory's own doc comments. No
 * locator here is used to answer it; only to detect that it is there.
 */
public final class PortalLoginPageLocators {

    private PortalLoginPageLocators() {

    }

    public static final By EMAIL_FIELD =
            By.cssSelector("input[type='email']");

    public static final By PASSWORD_FIELD =
            By.cssSelector("input[type='password']");

    /**
     * Disabled until the reCAPTCHA below is solved — see
     * ExpectedConditions.elementToBeClickable, which checks enabled state
     * as well as visibility.
     */
    public static final By LOGIN_BUTTON =
            By.cssSelector("button[type='submit']");

    /**
     * The reCAPTCHA widget's own host element (a custom
     * {@code <re-captcha>} tag wrapping Google's iframe) — present whether
     * or not it has been solved, so this alone does not tell you which.
     */
    public static final By RECAPTCHA_CONTAINER =
            By.cssSelector("re-captcha");

    /**
     * "Verification expired. Check the checkbox again." — shown in red
     * next to the checkbox after a solved challenge times out (the portal
     * appears to require the login click within a couple of minutes of
     * solving it).
     */
    public static final By RECAPTCHA_EXPIRED_MESSAGE =
            By.xpath("//*[contains(text(), 'Verification expired')]");

    /**
     * "Error! Suspicious reCAPTCHA score" — the portal's own server-side
     * risk-score rejection, distinct from the visible challenge itself.
     * Confirmed live: a correctly-solved challenge can still be rejected
     * with this message when the browser exposes obvious WebDriver
     * automation fingerprints — see PortalDriverFactory's own doc comment
     * for what this suite does about that (and does not attempt: solving
     * or defeating the CAPTCHA challenge itself is always a manual, human
     * step).
     */
    public static final By SUSPICIOUS_RECAPTCHA_ERROR =
            By.xpath("//*[contains(text(), 'Suspicious reCAPTCHA score')]");

    public static final By COOKIE_BANNER_CLOSE =
            By.cssSelector(".cookie-toast-close-btn");

}
