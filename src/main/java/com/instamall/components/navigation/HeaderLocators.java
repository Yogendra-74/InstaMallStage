package com.instamall.components.navigation;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Pieces of the screen header shared by every pushed screen.
 */
public final class HeaderLocators {

    private HeaderLocators() {

    }

    /**
     * The "<" at the top left of a pushed screen.
     *
     * It has no name, label or id, so it is matched by being the only
     * unnamed accessible Other of its size — a 12x15 glyph, the same on
     * every screen that has one (login, store, product, bag, checkout,
     * profile pages). An accessibility id from the app team would replace
     * this.
     */
    public static final By BACK_CHEVRON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND accessible == 1 AND name == nil"
                            + " AND rect.width == 12 AND rect.height == 15"
            );

}
