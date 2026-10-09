package com.instamall.pages.malls;

import com.instamall.components.navigation.HeaderLocators;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * The dedicated "Malls" list, reached from Home's EXPLORE MALLS section via
 * its "See all" link.
 *
 * A different screen from the Search screen's own Malls tab — this one has
 * no search field and no tabs, just the full list.
 *
 * VERIFY: mapped from a screenshot and a live tap during a manual session,
 * not yet from a captured page source — the row's exact element shape
 * (one accessible name combining the title and "Explore stores and
 * pieces", or two separate elements) is unconfirmed.
 */
public final class MallsListPageLocators {

    private MallsListPageLocators() {

    }

    public static final By BACK_BUTTON = HeaderLocators.BACK_CHEVRON;

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Malls' AND accessible == 1"
            );

    /**
     * A mall row, e.g. "Aventura Mall" — matched by prefix since the full
     * accessible name is just the mall's title (no address/distance on
     * this list, unlike the mall card on Home).
     */
    public static By mallRow(String mallName) {

        return AppiumBy.iOSNsPredicateString(
                "name BEGINSWITH '" + mallName + "' AND accessible == 1"
        );
    }

}
