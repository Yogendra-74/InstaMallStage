package com.instamall.pages.location;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * "Deliver to" address sheet (it was titled SELECT LOCATION before the
 * redesign). Verified against a page source dump.
 *
 * The sheet is presented on top of the home screen — so home's elements
 * are still in the tree while it is up. Locators here are specific enough
 * not to collide with them.
 */
public final class SelectLocationSheetLocators {

    private SelectLocationSheetLocators() {

    }

    // The sheet's header reads "Deliver to", but so do the home chip and
    // the checkout screen — the headline under it is unique to the sheet.
    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Where are we delivering?' AND accessible == 1"
            );

    // ---------- Quick slots ----------

    // Each slot's name is its label plus what it holds: "Home Set up"
    // while empty, "Work Stellar Okas, 1423, …" once set.
    public static final By SET_HOME =
            AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeButton[`name BEGINSWITH \"Home \"`][1]"
            );

    // First match only: a saved address labelled Work further down the
    // sheet starts with the same word.
    public static final By SET_WORK =
            AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeButton[`name BEGINSWITH \"Work \"`][1]"
            );

    // Name is "Other " plus the address currently in that slot.
    public static final By OTHER =
            AppiumBy.iOSClassChain(
                    "**/XCUIElementTypeButton[`name BEGINSWITH \"Other \"`][1]"
            );

    // ---------- Saved addresses ----------

    public static final By SAVED_ADDRESSES_HEADER =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Saved addresses' AND accessible == 1"
            );

    /**
     * A saved address row. Its name is the label and the street line —
     * "Z9 Ventures 38 NW 24th St, Miami, FL 33127, USA" — and the label's
     * case differs from the home chip ("Z9 VENTURES"), hence [c].
     */
    public static By savedAddress(String addressName) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH[c] '" + addressName + "'"
                        + " AND accessible == 1"
        );
    }

    public static By addressContaining(String addressFragment) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS '" + addressFragment + "'"
                        + " AND accessible == 1"
        );
    }

    // The "<" at the top left of the sheet: an unnamed 26x26 Button.
    public static final By CLOSE_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == nil AND accessible == 1"
                            + " AND rect.width == 26 AND rect.height == 26"
            );

}
