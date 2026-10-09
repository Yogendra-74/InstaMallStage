package com.instamall.pages.profile;

import com.instamall.base.BasePage;

public class AboutLegalPageValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(AboutLegalPageLocators.TITLE);
    }

    // Presence: the Documents list sits below the fold.
    public boolean areLegalLinksDisplayed() {

        return isPresent(AboutLegalPageLocators.TERMS_OF_USE)
                && isPresent(AboutLegalPageLocators.PRIVACY_POLICY);
    }

    public boolean isConsentsSectionDisplayed() {

        return isDisplayed(AboutLegalPageLocators.SECTION_YOUR_CONSENTS)
                && isDisplayed(AboutLegalPageLocators.CONSENT_TERMS_ROW);
    }

    public boolean isVirtualTryOnRowDisplayed() {

        return isDisplayed(AboutLegalPageLocators.VIRTUAL_TRY_ON_LABEL)
                && isPresent(AboutLegalPageLocators.VIRTUAL_TRY_ON_SWITCH);
    }

}
