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

    /**
     * The bag icon in the top-right corner of this screen's header.
     *
     * Neither this nor the overflow ("3 dots") menu beside it has an
     * accessibility id, and the icon glyph itself is nameless — so there is
     * no literal id to match. What is unique, confirmed against a live page
     * source dump, is that the bag carries its item-count badge as the
     * element's own name (e.g. "6"), and it is the ONLY element on the whole
     * mall screen whose name is purely digits — the overflow menu has no
     * name at all, and every other number on screen ("8 mi", "42-52 mins")
     * is part of a longer label. So the bag is matched by that numeric name
     * rather than by screen coordinates.
     *
     * The one assumption is that the bag is non-empty (the badge is only
     * shown when it holds items), which every caller here has already
     * ensured by adding products before opening it. An accessibility id
     * from the app team would replace this.
     */
    public static final By BAG_ICON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeOther' AND accessible == 1 AND name MATCHES '[0-9]+'"
            );

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
