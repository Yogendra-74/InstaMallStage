package com.instamall.components.permissions;

import com.instamall.actions.ElementActions;
import com.instamall.base.BasePage;

public class PermissionPopupValidator extends BasePage {

    public boolean isDisplayed() {

        return ElementActions.isDisplayed(
                PermissionPopupLocators.ALLOW
        );
    }
}