package com.instamall.components.permissions;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Buttons on iOS system permission alerts. These belong to springboard,
 * not to the app, so accessibility ids are stable across app builds.
 */
public final class PermissionPopupLocators {

    private PermissionPopupLocators(){}

    public static final By ALLOW =
            AppiumBy.accessibilityId("Allow");

    public static final By ASK_APP_NOT_TO_TRACK =
            AppiumBy.accessibilityId("Ask App Not to Track");

    public static final By ALLOW_WHILE_USING_APP =
            AppiumBy.accessibilityId("Allow While Using App");
}
