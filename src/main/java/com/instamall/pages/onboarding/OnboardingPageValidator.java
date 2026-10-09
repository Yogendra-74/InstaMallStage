package com.instamall.pages.onboarding;

import com.instamall.base.BasePage;

public class OnboardingPageValidator extends BasePage {

    public boolean isLoginButtonDisplayed() {

        return isDisplayed(OnboardingPageLocators.LOGIN_BUTTON);
    }

    /**
     * On screen at this instant, without waiting.
     *
     * For deciding whether the app came up signed out, where the answer
     * has to be raced against the signed-in screen rather than guessed
     * from a short glance — a cold start that has not finished painting
     * is not the same thing as an active session.
     */
    public boolean isLoginButtonDisplayedNow() {

        return isDisplayedNow(OnboardingPageLocators.LOGIN_BUTTON);
    }

    public boolean isSignUpButtonDisplayed() {

        return isDisplayed(OnboardingPageLocators.SIGNUP_BUTTON);
    }

    public boolean isExploreAsGuestDisplayed() {

        return isDisplayed(OnboardingPageLocators.EXPLORE_AS_GUEST);
    }

    public boolean isAppTitleDisplayed() {

        return isDisplayed(OnboardingPageLocators.APP_TITLE);
    }

    public boolean isAppTaglineDisplayed() {

        return isDisplayed(OnboardingPageLocators.APP_TAGLINE);
    }

}