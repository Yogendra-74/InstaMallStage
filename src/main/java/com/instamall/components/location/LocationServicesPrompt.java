package com.instamall.components.location;

import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * The app's "Enable Location" sheet, raised when it finds location
 * services switched off.
 *
 * On a simulator this is the normal path rather than an edge case:
 * DriverFactory sets autoDismissAlerts=true, so iOS's own permission
 * alert is answered before the app ever sees a grant — and the app then
 * asks for an address itself.
 *
 * Every method is tolerant: nothing here fails when the sheet is absent,
 * so a flow can call it unconditionally.
 */
public final class LocationServicesPrompt {

    // The sheet animates in right after the home screen mounts.
    private static final Duration PROMPT_TIMEOUT = Duration.ofSeconds(15);

    private LocationServicesPrompt() {

    }

    public static boolean isShowing() {

        return WaitUtils.isVisible(
                LocationServicesPromptLocators.TITLE, PROMPT_TIMEOUT);
    }

    /**
     * Up at this instant, without waiting.
     *
     * Lets a caller watch for this sheet alongside the screen it would
     * otherwise cover, instead of paying its whole deadline to learn it
     * was never coming.
     */
    public static boolean isShowingNow() {

        return WaitUtils.isVisibleNow(LocationServicesPromptLocators.TITLE);
    }

    /**
     * Takes the "Select location" route on a sheet already known to be up,
     * so no time is spent re-establishing that it is there.
     */
    public static void selectLocationNow() {

        WaitUtils.waitForClickable(
                LocationServicesPromptLocators.SELECT_LOCATION, PROMPT_TIMEOUT).click();
    }

    /**
     * Takes the "Select location" route, which opens the SELECT LOCATION
     * address sheet.
     *
     * @return true if the sheet was there and got tapped, false if there
     *         was nothing to handle.
     */
    public static boolean chooseSelectLocation() {

        return tap(LocationServicesPromptLocators.SELECT_LOCATION);
    }

    /**
     * Dismisses the sheet and leaves the home screen without a location —
     * the store feed stays empty in that state.
     */
    public static boolean ignore() {

        return tap(LocationServicesPromptLocators.IGNORE);
    }

    private static boolean tap(By button) {

        if (!WaitUtils.isVisible(LocationServicesPromptLocators.TITLE, PROMPT_TIMEOUT)) {

            return false;
        }

        WaitUtils.waitForClickable(button, PROMPT_TIMEOUT).click();

        return true;
    }

}
