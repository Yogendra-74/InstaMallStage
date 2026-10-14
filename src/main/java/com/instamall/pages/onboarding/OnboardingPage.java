package com.instamall.pages.onboarding;

import com.instamall.base.BasePage;
import com.instamall.waits.WaitUtils;

import java.time.Duration;

public class OnboardingPage extends BasePage {

    /**
     * A cold start loads the bundle and shows the splash before this
     * screen paints — several seconds, longer than the framework's short
     * glance. Anything that reads the screen straight after a relaunch
     * waits on this first.
     */
    private static final Duration FIRST_PAINT_TIMEOUT = Duration.ofSeconds(45);

    public OnboardingPage waitForScreen() {

        WaitUtils.waitForVisibility(OnboardingPageLocators.LOGIN_BUTTON, FIRST_PAINT_TIMEOUT);

        return this;
    }

    public void clickLogin() {

        // One of the few places that genuinely needs the button to have
        // stopped moving first. Tapping the moment it appears — the default
        // everywhere else — is swallowed by the entry animation here, and
        // the run then sits waiting twenty seconds for a login screen that
        // was never opened. Measured twice: once when the poll interval was
        // shortened, once when settle stopped waiting for stability.
        settleUntilStill(OnboardingPageLocators.LOGIN_BUTTON);

        click(OnboardingPageLocators.LOGIN_BUTTON);

        System.out.println("Login button clicked");
    }

    /**
     * The "Make it personal" entry point — same destination as clickLogin()
     * (the phone-number screen, then "Continue with email instead" leads
     * to the same EmailLoginPage either way), just reached by a different
     * button. Confirmed live: an email already registered signs straight
     * in here rather than erroring as a duplicate account, which is what
     * makes this a second, equally valid way to reach an existing login
     * — preferred over clickLogin() for a run that specifically wants to
     * exercise this entry point (its EmailLoginPage headline reads
     * differently — see EmailLoginPageLocators.HEADLINE's own comment).
     */
    public void clickSignUp() {

        settleUntilStill(OnboardingPageLocators.SIGNUP_BUTTON);

        // A plain click() was confirmed live to report success without
        // ever actually navigating anywhere — the "Make it personal" tap
        // prints as sent but the phone-number screen that should follow
        // never opens, same control class as EmailLoginPage's consent
        // checkbox and ConsentPrompts' Agree & Continue button — see
        // ElementActions.pressAndRelease()'s own doc comment.
        pressAndRelease(OnboardingPageLocators.SIGNUP_BUTTON);

        System.out.println("Make it personal clicked");
    }

    /**
     * Skips sign-in entirely — confirmed live this still reaches a fully
     * usable Home feed (location and notifications prompts follow the
     * same as a signed-in session), just with no saved account/addresses
     * behind it.
     */
    public void clickExploreAsGuest() {

        settleUntilStill(OnboardingPageLocators.EXPLORE_AS_GUEST);

        click(OnboardingPageLocators.EXPLORE_AS_GUEST);

        System.out.println("Explore as a guest clicked");
    }

}