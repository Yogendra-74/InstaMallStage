package com.instamall.pages.profile;

import com.instamall.base.BasePage;
import com.instamall.waits.WaitUtils;

public class AboutLegalPage extends BasePage {

    public AboutLegalPage waitForScreen() {

        waitForVisibility(AboutLegalPageLocators.TITLE);

        return this;
    }

    /**
     * Whether the "Turn off/on AI Try-On?" confirmation is up.
     */
    public boolean isConsentConfirmationShowing() {

        return isDisplayed(AboutLegalPageLocators.CONSENT_CONFIRMATION_TITLE);
    }

    /**
     * Answers the confirmation by keeping the consent as it is.
     */
    public AboutLegalPage keepConsentAsIs() {

        click(AboutLegalPageLocators.CONSENT_CONFIRMATION_KEEP);
        waitForInvisibility(AboutLegalPageLocators.CONSENT_CONFIRMATION_TITLE);

        return this;
    }

    public AboutLegalPage toggleVirtualTryOn() {

        click(AboutLegalPageLocators.VIRTUAL_TRY_ON_SWITCH);

        return this;
    }

    /** @return "1" when the consent is on, "0" when it is off. */
    public String getVirtualTryOnState() {

        return WaitUtils.waitForPresence(AboutLegalPageLocators.VIRTUAL_TRY_ON_SWITCH)
                .getAttribute("value");
    }

}
