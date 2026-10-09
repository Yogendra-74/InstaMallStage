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

}