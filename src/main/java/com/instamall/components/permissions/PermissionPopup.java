package com.instamall.components.permissions;
import com.instamall.base.BasePage;

import com.instamall.actions.ElementActions;

public class PermissionPopup extends BasePage {

    public void allowTracking() {

        ElementActions.click(
                PermissionPopupLocators.ALLOW
        );
    }

    public void denyTracking() {

        ElementActions.click(
                PermissionPopupLocators.ASK_APP_NOT_TO_TRACK
        );
    }
}