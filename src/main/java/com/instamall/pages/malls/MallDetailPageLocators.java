package com.instamall.pages.malls;

import com.instamall.components.navigation.HeaderLocators;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * The per-mall store directory reached by tapping a mall card under
 * Home's own "EXPLORE MALLS" section (see HomePageLocators.mallCard()) —
 * titled "Explore the mall", e.g. for Miami Design District: Moncler,
 * Prada, Burberry, each with its own delivery estimate.
 *
 * Confirmed live that this path genuinely opens a per-mall directory,
 * which is what this class addresses.
 */
public final class MallDetailPageLocators {

    private MallDetailPageLocators() {

    }

    public static final By BACK_BUTTON = HeaderLocators.BACK_CHEVRON;

    // Confirmed against a live page source dump: this title is a plain
    // StaticText, not an Other like most of this screen's other rows —
    // the wrong type here previously matched nothing at all, so
    // waitForScreen() timed out every time despite the screen being
    // genuinely open underneath it the whole time.
    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Explore the mall' AND accessible == 1"
            );

    /**
     * A store row in this directory, addressed by brand name.
     *
     * Its accessible name fuses a leading single-letter prefix from the
     * brand's own logo image into the same string ahead of the real
     * name — confirmed live: "B Burberry Miami Design District 1 mi ·
     * Miami 14-24 mins" for the Burberry row. BEGINSWITH therefore
     * cannot match the brand reliably; CONTAINS can.
     */
    public static By storeRow(String storeName) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeOther' AND name CONTAINS '" + storeName + "'"
                        + " AND accessible == 1"
        );
    }

}
