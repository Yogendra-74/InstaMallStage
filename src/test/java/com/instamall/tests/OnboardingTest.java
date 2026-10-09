package com.instamall.tests;

import com.instamall.base.BaseTest;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import org.testng.Assert;
import org.testng.annotations.Test;

public class OnboardingTest extends BaseTest {

    @Test(description = "All key elements are visible on the onboarding screen")
    public void verifyOnboardingScreenElementsAreDisplayed() {

        OnboardingPageValidator onboardingValidator = new OnboardingPageValidator();

        // Straight after a relaunch: give the cold start time to paint
        // before the short per-element checks below.
        new OnboardingPage().waitForScreen();

        Assert.assertTrue(onboardingValidator.isAppTitleDisplayed(),
                "App title is not displayed.");

        Assert.assertTrue(onboardingValidator.isAppTaglineDisplayed(),
                "App tagline is not displayed.");

        Assert.assertTrue(onboardingValidator.isLoginButtonDisplayed(),
                "Sign in button is not displayed.");

        Assert.assertTrue(onboardingValidator.isSignUpButtonDisplayed(),
                "Make it personal button is not displayed.");

        Assert.assertTrue(onboardingValidator.isExploreAsGuestDisplayed(),
                "Explore as a guest button is not displayed.");
    }

}