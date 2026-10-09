package com.instamall.pages.profile;

import com.instamall.base.BasePage;

public class AccountSettingsPage extends BasePage {

    public AccountSettingsPage waitForScreen() {

        waitForVisibility(AccountSettingsPageLocators.TITLE);

        return this;
    }

    public AccountSettingsPage openNotificationsAndAnalytics() {

        click(AccountSettingsPageLocators.NOTIFICATIONS_ROW);

        return this;
    }

}
