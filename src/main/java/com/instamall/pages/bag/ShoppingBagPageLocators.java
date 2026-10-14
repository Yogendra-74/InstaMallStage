package com.instamall.pages.bag;

import com.instamall.components.navigation.HeaderLocators;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * "Your bag" screen. Verified against page source dumps of the redesigned
 * build.
 *
 * The bag no longer wraps each item in a row element of its own. Every
 * piece of every item — title, variant, price, stepper, Save for later,
 * Remove — is a direct child of one list container, one after another,
 * and the store-group header and its "Review delivery &amp; checkout"
 * button sit in the same list. So there is nothing here that names "the
 * row for product X"; ShoppingBagPage puts rows together from the
 * elements' positions instead, and the locators below are the pieces it
 * works from.
 */
public final class ShoppingBagPageLocators {

    private ShoppingBagPageLocators() {

    }

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'Your bag' AND accessible == 1"
            );

    // VERIFY: not seen on the redesigned build — the bag was never empty
    // while it was being mapped.
    public static final By EMPTY_BAG_MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "name CONTAINS[c] 'empty' AND accessible == 1"
            );

    /**
     * A store group's header, a Button named brand and location:
     * "Burberry Miami Design District".
     */
    public static By storeGroup(String storeName) {

        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name BEGINSWITH '" + storeName + "' AND accessible == 1"
        );
    }

    /**
     * Any amount on the screen — item prices, the delivery fee, the
     * summary rows. Which one is which comes from where it sits.
     */
    public static final String REMOVE_TEXT = "Remove";
    public static final String SAVE_FOR_LATER_TEXT = "Save for later";

    public static final By REMOVE_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND name == '" + REMOVE_TEXT + "' AND accessible == 1"
            );

    /**
     * What the app writes onto a line it cannot sell once a checkout has
     * been refused.
     *
     * VERIFY: carried over from the old build ("Item is out of stock.
     * Please update your cart to proceed.") and matched loosely; no
     * refusal was provoked on the redesigned build.
     */
    public static final String OUT_OF_STOCK_TEXT = "out of stock";

    public static final By OUT_OF_STOCK_MESSAGE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name CONTAINS[c] '" + OUT_OF_STOCK_TEXT + "' AND accessible == 1"
            );

    // ---------- Summary ----------


    /**
     * One per store group — each delivery checks out on its own — and no
     * amount in the label any more.
     */
    public static final String CHECKOUT_BUTTON_TEXT = "Review delivery & checkout";

    public static final By CHECKOUT_BUTTON =
            AppiumBy.iOSNsPredicateString(
                    "name == '" + CHECKOUT_BUTTON_TEXT + "' AND accessible == 1"
            );

    // The header X became the shared back chevron.
    public static final By CLOSE_BUTTON = HeaderLocators.BACK_CHEVRON;

    static String longestPlainRun(String productName) {

        String longest = "";

        for (String run : productName.split("[\\u200B-\\u200D\\uFEFF]")) {

            if (run.trim().length() > longest.length()) {

                longest = run.trim();
            }
        }

        return longest.isEmpty() ? productName : longest;
    }

}
