package com.instamall.components.permissions;

import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Tolerant handling of the iOS permission alerts the app raises right
 * after login (location, then notifications).
 *
 * Every method here is a no-op when the alert is not on screen. That is
 * deliberate: DriverFactory sets autoDismissAlerts=true, so on the
 * default configuration iOS never shows these alerts and the flow must
 * not fail for that reason. Set autoDismissAlerts=false when a test needs
 * to assert on the alerts themselves.
 */
public final class SystemAlerts {

    // Alerts appear within a second or two, or not at all — a long wait
    // here would add dead time to every run.
    private static final Duration ALERT_TIMEOUT = Duration.ofSeconds(5);

    private SystemAlerts() {

    }

    /**
     * Grants location while the app is in use, which is what the shopping
     * flow needs for nearby stores and delivery.
     */
    public static boolean allowLocationWhileUsingApp() {

        return tap(PermissionPopupLocators.ALLOW_WHILE_USING_APP);
    }

    public static boolean allowLocationOnce() {

        return tap(PermissionPopupLocators.ALLOW_ONCE);
    }

    public static boolean denyLocation() {

        return tap(PermissionPopupLocators.DONT_ALLOW);
    }

    public static boolean allowNotifications() {

        return tap(PermissionPopupLocators.ALLOW);
    }

    public static boolean denyNotifications() {

        return tap(PermissionPopupLocators.DONT_ALLOW);
    }

    /**
     * Camera access prompt ("Allow ... to access your camera?") — same
     * "Allow" button as {@link #allowNotifications()}, named separately
     * for readability at the call site.
     */
    public static boolean allowCamera() {

        return tap(PermissionPopupLocators.ALLOW);
    }

    /**
     * "Save Password?" — offered right after a sign in, on every app
     * alike. Tapped here rather than "Not Now" so credentials are saved,
     * per instruction to allow save-password prompts.
     */
    public static boolean savePassword() {

        return tap(PermissionPopupLocators.SAVE_PASSWORD);
    }

    /**
     * App Tracking Transparency prompt ("Allow ... to track your
     * activity..."). Unlike the other alerts here, autoDismissAlerts does
     * not answer this one — iOS reserves ATT consent from automated
     * dismissal — so it needs a real tap regardless of that setting.
     *
     * Allows rather than denies, per instruction to grant every
     * permission prompt this flow can raise.
     */
    public static boolean allowTracking() {

        return tap(PermissionPopupLocators.ALLOW);
    }

    /**
     * @return true if the alert was there and got tapped, false if there
     *         was nothing to handle.
     */
    private static boolean tap(By button) {

        if (!WaitUtils.isVisible(button, ALERT_TIMEOUT)) {

            return false;
        }

        WaitUtils.waitForClickable(button, ALERT_TIMEOUT).click();

        return true;
    }

    /**
     * Every button wording seen offering to grant something, most
     * specific first — "Allow While Using App" is what the shopping flow
     * actually needs (persistent, not one-time) for location, so it is
     * preferred over "Allow Once" when a location alert offers both.
     */
    private static final List<String> GRANTING_BUTTONS = List.of(
            "Allow While Using App", "Allow Once", "Save", "Allow"
    );

    /**
     * Answers whatever permission alert is on screen right now, if any —
     * a single no-wait pass, meant for a caller already polling in a loop
     * of its own. Grants every prompt this flow can raise (notifications,
     * location, App Tracking Transparency, save password), per
     * instruction to allow every permission the app asks for.
     *
     * Goes through Appium's "mobile: alert" rather than locating one of
     * PermissionPopupLocators' buttons and calling click() on it as a
     * regular element — confirmed live and reproducible that the latter
     * can find the button (present in findElements(), name="Allow",
     * visible="true") and still silently fail to register the tap: a
     * system alert belongs to springboard, not the app, and a synthetic
     * touch aimed at one of its buttons through the app's own element
     * tree does not reliably land the way it does for the app's own UI.
     * "mobile: alert" asks WDA to answer the alert through the same
     * channel autoDismissAlerts itself uses, which does not have that
     * problem — and, unlike the plain W3C accept() this replaced, lets a
     * specific button be named rather than whatever iOS treats as the
     * alert's default, which for a three-button location alert is not
     * reliably "Allow While Using App".
     *
     * A permission alert not answered here can otherwise sit on screen
     * indefinitely — it belongs to springboard, not the app, so it
     * survives the app being terminated and relaunched underneath it,
     * silently blocking every element lookup on every screen after it.
     *
     * @return true if an alert was there and got answered
     */
    public static boolean grantAnyShowingNow() {

        List<String> buttons;

        try {

            @SuppressWarnings("unchecked")
            List<String> found = (List<String>) DriverManager.getDriver()
                    .executeScript("mobile: alert", Map.of("action", "getButtons"));
            buttons = found;

        } catch (Exception noAlertOrUnsupported) {

            return false;
        }

        if (buttons == null || buttons.isEmpty()) {

            return false;
        }

        String toTap = GRANTING_BUTTONS.stream()
                .filter(buttons::contains)
                .findFirst()
                .orElse(null);

        if (toTap == null) {

            // An alert is up but offers none of the granting wordings
            // this looks for — leave it for whatever handles that
            // wording specifically, rather than tapping something blind.
            return false;
        }

        try {

            DriverManager.getDriver().executeScript("mobile: alert",
                    Map.of("action", "accept", "buttonLabel", toTap));
            return true;

        } catch (Exception acceptFailed) {

            // Same tolerance as everywhere else here: a poll loop tries
            // again next pass rather than failing on a transient issue
            // (the alert clearing itself between the check and the
            // accept, say).
            return false;
        }
    }

}
