package com.instamall.pages.login;

import com.instamall.base.BasePage;
import com.instamall.components.consent.ConsentPrompts;
import com.instamall.pages.home.HomePageLocators;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class EmailLoginPage extends BasePage {

    /**
     * Continue is the only unique accessible anchor on this screen, so it
     * is the readiness gate. Both input fields are only checked for presence
     * — WDA's visible flag is unreliable for nested RN inputs.
     */
    public EmailLoginPage waitForScreen() {

        waitForVisibility(EmailLoginPageLocators.NEXT_BUTTON);
        WaitUtils.waitForPresence(EmailLoginPageLocators.EMAIL_TEXTBOX);
        WaitUtils.waitForPresence(EmailLoginPageLocators.PASSWORD_TEXTBOX);

        return this;
    }

    public EmailLoginPage enterEmail(String email) {

        typeInto(EmailLoginPageLocators.EMAIL_TEXTBOX, email, false);

        return this;
    }

    public EmailLoginPage enterPassword(String password) {

        typeInto(EmailLoginPageLocators.PASSWORD_TEXTBOX, password, true);

        return this;
    }

    public EmailLoginPage clickNext() {

        submitCredentials();

        // A fresh sign in is followed by the Save Password alert and the
        // two AI consent sheets, all of which sit on top of home — and the
        // sign in itself is slow, so this waits for home to be clear.
        ConsentPrompts.acceptAfterSignIn(HomePageLocators.TAB_HOME);

        return this;
    }

    /**
     * Submits the sign-in form and returns immediately — none of what
     * can follow (Save Password alert, Notification and Location
     * permission alerts in no fixed order, up to three consent sheets)
     * is handled here, unlike {@link #clickNext()}. For a caller that
     * needs to run its own logic in between submitting and whatever
     * comes after — e.g. a permissions loop of its own — before
     * finishing with {@link ConsentPrompts#acceptAfterSignIn}.
     */
    public EmailLoginPage submitCredentials() {

        // The keyboard is still up from the password field and covers
        // both the consent box and Continue, so put it away first.
        dismissKeyboard();

        acceptConsent();

        // Button stays disabled until both fields are filled and the
        // consent box is ticked.
        WaitUtils.waitForEnabled(EmailLoginPageLocators.NEXT_BUTTON);

        click(EmailLoginPageLocators.NEXT_BUTTON);

        return this;
    }

    /**
     * Ticks the messaging-consent box, leaving it alone if it is already
     * ticked. It only appears once both fields hold text.
     */
    public EmailLoginPage acceptConsent() {

        WebElement checkbox =
                WaitUtils.waitForPresence(EmailLoginPageLocators.CONSENT_CHECKBOX);

        if (!LoginPage.isChecked(checkbox)) {

            // A plain click() was confirmed live to report success
            // without ever actually toggling this control — see
            // BasePage.pressAndRelease()'s own doc comment.
            pressAndRelease(EmailLoginPageLocators.CONSENT_CHECKBOX);
        }

        return this;
    }

    public EmailLoginPage dismissKeyboard() {

        click(EmailLoginPageLocators.HEADLINE);

        return this;
    }

    public EmailLoginPage clickForgotPassword() {

        click(EmailLoginPageLocators.FORGOT_PASSWORD);

        return this;
    }

    public EmailLoginPage clickGoBack() {

        click(EmailLoginPageLocators.GO_BACK);

        return this;
    }

    /**
     * Fills a field in one call, checks it landed, and only then falls
     * back to a character at a time.
     *
     * A character at a time was the standing approach here, borrowed from
     * the phone screen, where a masked input really does drop a digit when
     * ten arrive at once (reproPhoneFieldDropsDigitOnFastInput). But these
     * two fields are not masked, and re-formatting on every keystroke is
     * the mechanism that loses characters — so the assumption was worth
     * testing rather than inheriting. An email and a password cost
     * thirty-nine round trips this way, the better part of twenty seconds.
     *
     * Verified rather than trusted, at both speeds: whatever happens, the
     * field is read back, and a field that cannot be filled correctly says
     * so instead of leaving Continue mysteriously disabled.
     *
     * @param masked true where the field hands back bullets instead of its
     *               contents, so only the count can be compared
     */
    private void typeInto(By locator, String text, boolean masked) {

        WebElement field = WaitUtils.waitForPresence(locator);

        field.click();

        field.sendKeys(text);

        if (landed(field, text, masked)) {

            return;
        }

        System.out.println(">>> Bulk input did not land in the "
                + (masked ? "password" : "email")
                + " field — retyping one character at a time.");

        field.clear();

        for (char character : text.toCharArray()) {

            field.sendKeys(String.valueOf(character));
        }

        if (!landed(field, text, masked)) {

            throw new IllegalStateException(
                    "Could not fill the " + (masked ? "password" : "email")
                            + " field even one character at a time — it reads \""
                            + field.getAttribute("value") + "\"");
        }
    }

    /**
     * Whether what was typed is what the field now holds.
     *
     * A masked field returns bullets, so there the only thing worth
     * comparing is how many — and a wrong password of the right length
     * fails visibly at sign in rather than passing quietly.
     */
    private boolean landed(WebElement field, String text, boolean masked) {

        String value = field.getAttribute("value");

        if (value == null) {

            return false;
        }

        return masked ? value.length() == text.length() : value.equals(text);
    }

}
