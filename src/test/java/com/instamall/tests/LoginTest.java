package com.instamall.tests;

import com.instamall.base.BaseTest;
import com.instamall.pages.login.LoginPage;
import com.instamall.pages.login.LoginPageLocators;
import com.instamall.pages.login.LoginPageValidator;
import com.instamall.pages.onboarding.OnboardingPage;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.utils.TestDataGenerator;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    private OnboardingPage onboardingPage;
    private LoginPage loginPage;
    private LoginPageValidator loginPageValidator;

    @BeforeMethod
    public void navigateToLoginScreen() {

        onboardingPage = new OnboardingPage();
        loginPage = new LoginPage();
        loginPageValidator = new LoginPageValidator();

        onboardingPage.clickLogin();
        loginPage.waitForScreen();
    }

    // ---------- Screen layout (also covers onboarding -> login navigation) ----------

    @Test(description = "All key elements are visible on the login screen")
    public void verifyLoginScreenElementsAreDisplayed() {

        Assert.assertTrue(loginPageValidator.isPhoneNumberFieldDisplayed(),
                "Phone number field is not displayed");

        Assert.assertTrue(loginPageValidator.isSendOTPButtonDisplayed(),
                "Send code button is not displayed");

        Assert.assertTrue(loginPageValidator.isContinueWithEmailDisplayed(),
                "Continue with email link is not displayed");

        Assert.assertTrue(loginPageValidator.isGoBackDisplayed(),
                "Back chevron is not displayed");
    }

    // ---------- Send code enable/disable behavior ----------
    //
    // Send code needs both a full number and the consent box ticked. The
    // number tests tick the box first, so the number is the only thing
    // deciding the outcome.

    @Test(description = "Send code is disabled when phone number is empty")
    public void verifySendOTPDisabledWithEmptyPhoneNumber() {

        loginPage.acceptConsent();

        Assert.assertFalse(loginPageValidator.isSendOTPButtonEnabled(),
                "Send code should be disabled when phone number is empty");
    }

    @Test(description = "Send code stays disabled for an incomplete phone number")
    public void verifySendOTPDisabledWithShortPhoneNumber() {

        loginPage.acceptConsent()
                 .enterPhoneNumber(TestDataGenerator.shortPhoneNumber());

        Assert.assertFalse(loginPageValidator.isSendOTPButtonEnabled(),
                "Send code should remain disabled for an incomplete phone number");
    }

    @Test(description = "Send code stays disabled for a valid number until consent is given")
    public void verifySendOTPDisabledWithoutConsent() {

        loginPage.enterPhoneNumber(TestDataGenerator.randomPhoneNumber());

        Assert.assertFalse(loginPageValidator.isConsentChecked(),
                "Consent box should start unticked");

        Assert.assertFalse(loginPageValidator.isSendOTPButtonEnabled(),
                "Send code should stay disabled until the consent box is ticked");
    }

    @Test(description = "Send code becomes enabled after entering a valid phone number")
    public void verifySendOTPEnabledWithValidPhoneNumber() {

        loginPage.acceptConsent()
                 .enterPhoneNumber(TestDataGenerator.randomPhoneNumber());

        Assert.assertTrue(loginPageValidator.isSendOTPButtonEnabled(),
                "Send code should be enabled after entering a valid phone number");
    }

    @Test(description = "Send code is disabled again after clearing the phone number")
    public void verifySendOTPDisabledAfterClearingPhoneNumber() {

        loginPage.acceptConsent()
                 .enterPhoneNumber(TestDataGenerator.randomPhoneNumber())
                 .clearPhoneNumber();

        Assert.assertFalse(loginPageValidator.isSendOTPButtonEnabled(),
                "Send code should be disabled again after clearing the phone number");
    }

    // ---------- Navigation ----------

    @Test(description = "The back chevron returns the user to the onboarding screen")
    public void verifyGoBackReturnsToOnboardingScreen() {

        OnboardingPageValidator onboardingPageValidator = new OnboardingPageValidator();

        loginPage.clickGoBack();

        Assert.assertTrue(onboardingPageValidator.isLoginButtonDisplayed(),
                "Onboarding screen is not displayed after tapping back");
    }

    // ---------- Bug repro (keep disabled while dev ticket is open) ----------

    /**
     * BUG: RN masked input drops a digit when 10 digits are sent as one
     * fast sendKeys call. Field ends up with 9 digits, e.g. "(421) 728-540",
     * app shows "Please enter a valid number", SEND OTP stays disabled.
     *
     * Enable this test to reproduce / verify the dev fix.
     * Runs 5 times (invocationCount) — the pass/fail ratio is the
     * reproduction rate to report in the bug ticket.
     */
    @Test(enabled = false,
          invocationCount = 5,
          description = "BUG REPRO: fast sendKeys drops a digit in the phone field")
    public void reproPhoneFieldDropsDigitOnFastInput() {

        String input = TestDataGenerator.randomPhoneNumber(); // 10 digits

        WebElement field = WaitUtils.waitForPresence(
                LoginPageLocators.PHONE_NUMBER_TEXTBOX);

        // Whole string in one sendKeys — this is what triggers the race.
        field.sendKeys(input);

        String digitsInField = field.getAttribute("value")
                .replaceAll("[^0-9]", "");

        Assert.assertEquals(digitsInField.length(), input.length(),
                "Typed " + input.length() + " digits but field contains \""
                + digitsInField + "\" (" + digitsInField.length() + " digits)");
    }

}