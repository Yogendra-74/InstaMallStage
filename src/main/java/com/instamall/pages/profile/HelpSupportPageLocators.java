package com.instamall.pages.profile;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * HELP & SUPPORT screen — an FAQ accordion in five sections. Verified
 * against test-output/exploration/profile-help-support.xml.
 */
public final class HelpSupportPageLocators {

    private HelpSupportPageLocators() {

    }

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Help & support' AND accessible == 1");

    public static By section(String title) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name ==[c] '" + title + "' AND accessible == 1");
    }

    /**
     * An FAQ question row, e.g. "How do I track my order?". Class chain,
     * not a predicate: several questions carry an apostrophe ("What if my
     * order doesn't arrive?"), which would end a single-quoted predicate.
     */
    public static By question(String text) {

        // Questions are Buttons now (they were Others).
        return AppiumBy.iOSNsPredicateString(
                "type IN {'XCUIElementTypeButton', 'XCUIElementTypeOther'} AND name == '" + text + "'"
                        + " AND accessible == 1");
    }

}
