package com.instamall.pages.profile;

import com.instamall.base.BasePage;

public class AccountSettingsPageValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(AccountSettingsPageLocators.TITLE);
    }

    public boolean isEmailShown(String email) {

        return isDisplayed(AccountSettingsPageLocators.EMAIL_LABEL)
                && isDisplayed(AccountSettingsPageLocators.emailValue(email));
    }

    public boolean isPhoneShown() {

        return isDisplayed(AccountSettingsPageLocators.PHONE_LABEL)
                && isDisplayed(AccountSettingsPageLocators.PHONE_VALUE);
    }

    public boolean areAllSectionsDisplayed() {

        return isDisplayed(AccountSettingsPageLocators.SECTION_YOUR_ACCOUNT)
                && isDisplayed(AccountSettingsPageLocators.SECTION_PRIVACY)
                && isDisplayed(AccountSettingsPageLocators.SECTION_ACCOUNT_ACTIONS);
    }

    public boolean isDeleteAccountRowDisplayed() {

        return isDisplayed(AccountSettingsPageLocators.DELETE_ACCOUNT_ROW);
    }

    public boolean isAppVersionDisplayed() {

        // Presence: the footer sits below the fold.
        return isPresent(AccountSettingsPageLocators.APP_VERSION);
    }

}
