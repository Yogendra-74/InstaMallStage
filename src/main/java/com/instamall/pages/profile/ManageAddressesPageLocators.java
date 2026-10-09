package com.instamall.pages.profile;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * MANAGE ADDRESSES screen ("SAVED ADDRESSES"). Verified against
 * test-output/exploration/profile-manage-addresses.xml.
 *
 * This screen covers the tab bar — the way back to Profile is the back
 * control at the top left, which carries no name. See
 * ProfileNavigator#returnToProfile.
 */
public final class ManageAddressesPageLocators {

    private ManageAddressesPageLocators() {

    }

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Manage addresses' AND accessible == 1");

    public static final By ADD_NEW_ADDRESS =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == 'Add new address' AND accessible == 1");

    /**
     * A saved-address card. Its accessible name is the label followed by
     * the full street address — "Z9 Ventures 38 NW 24th St, Miami, FL
     * 33127, USA" — so the label is matched as a prefix.
     */
    public static By addressCard(String label) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH[c] '" + label + "' AND accessible == 1");
    }

}
