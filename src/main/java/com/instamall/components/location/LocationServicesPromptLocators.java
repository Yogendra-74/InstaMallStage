package com.instamall.components.location;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * The app's own "Location permission not enabled" sheet — not an iOS
 * alert. Verified
 * against a page source dump.
 *
 * Every locator here filters on visible == 1: the home screen underneath
 * carries its own "Enable Location" and "Select location" controls, and
 * they stay in the tree (visible == 0) while the sheet is up.
 */
public final class LocationServicesPromptLocators {

    private LocationServicesPromptLocators() {

    }

    // Titled "Location permission not enabled" when the app has been
    // refused location. "Enable Location" was the old title, kept for the
    // location-services-off variant, which was not seen in the new build.
    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText'"
                            + " AND name IN {'Location permission not enabled', 'Enable Location'}"
                            + " AND accessible == 1 AND visible == 1"
            );

    public static final By MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "(name BEGINSWITH 'Please enable location permission' OR name CONTAINS 'GPS/location services')"
                            + " AND accessible == 1 AND visible == 1"
            );

    public static final By OPEN_SETTINGS =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Open Settings' AND accessible == 1 AND visible == 1"
            );

    /**
     * Opens the "Deliver to" address sheet — the same one the home
     * header chip opens.
     */
    public static final By SELECT_LOCATION =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Select location' AND accessible == 1 AND visible == 1"
            );

    public static final By IGNORE =
            AppiumBy.iOSNsPredicateString(
                    "name == 'Ignore' AND accessible == 1 AND visible == 1"
            );

}
