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
     * flow needs for nearby stores and delivery. Waits up to
     * {@link #ALERT_TIMEOUT} for the alert, then delegates the actual
     * accept to {@link #grantLocationIfShowingNow()}.
     */
    public static boolean allowLocationWhileUsingApp() {

        return WaitUtils.isVisible(PermissionPopupLocators.ALLOW_WHILE_USING_APP, ALERT_TIMEOUT)
                && grantLocationIfShowingNow();
    }

    /**
     * Waits up to {@link #ALERT_TIMEOUT} for the Notification permission
     * alert, then delegates the accept to
     * {@link #grantNotificationIfShowingNow()}.
     */
    public static boolean allowNotifications() {

        return WaitUtils.isVisible(PermissionPopupLocators.ALLOW, ALERT_TIMEOUT)
                && grantNotificationIfShowingNow();
    }

    /**
     * Single no-wait pass at the Notification permission alert — accepts
     * it if that alert is on screen right now, otherwise does nothing.
     * Meant for a caller already polling in a loop of its own, where the
     * two permission alerts this app can raise after sign-in are not
     * guaranteed to arrive in any fixed order, so a tight loop needs an
     * instant check for each rather than a blocking wait on either one.
     * See {@link #acceptAlertButtonIfShowingNow} for the mechanism.
     *
     * @return true if the alert was there and got tapped
     */
    public static boolean grantNotificationIfShowingNow() {

        return acceptAlertButtonIfShowingNow("Allow");
    }

    /**
     * Single no-wait pass at the Location permission alert — same shape
     * as {@link #grantNotificationIfShowingNow()}. See
     * {@link #acceptAlertButtonIfShowingNow} for the mechanism, and its
     * own doc comment for why this goes through "mobile: alert" rather
     * than an element-targeted click()/tap(): confirmed live, several
     * element-targeted approaches tried here previously (plain click(),
     * pressAndRelease(), a coordinate tap derived from the element's own
     * rect) all either silently failed to register or were simply
     * unproven guesses; "mobile: alert" with an explicit buttonLabel was
     * then confirmed live, directly and repeatably — getButtons()
     * returning the real three button labels, accept() with
     * "Allow While Using App" actually clearing the alert (status bar's
     * location arrow lighting up as proof, not just the alert
     * disappearing) — against this exact alert, including freshly
     * reproduced (xcrun simctl privacy reset location, relaunch) rather
     * than assumed from an old attempt elsewhere in this class' history.
     *
     * @return true if the alert was there and got tapped
     */
    public static boolean grantLocationIfShowingNow() {

        return acceptAlertButtonIfShowingNow("Allow While Using App");
    }

    /**
     * Single no-wait pass at the App Tracking Transparency alert — same
     * shape as {@link #grantNotificationIfShowingNow()}, same "Allow"
     * button. Named separately for readability at the call site, same
     * reasoning as {@link #allowCamera()} alongside {@link
     * #allowNotifications()}.
     *
     * The one meant for a caller that has already been polling with
     * {@link #grantAnyShowingNow()} while waiting for the app's first
     * screen to show (both apps' first-screen waits do): by the time that
     * loop exits, this alert — which shows at launch, within a second or
     * two of it or not at all — has very likely already been caught and
     * cleared there, which is exactly why this is a no-wait check rather
     * than another blocking {@link #ALERT_TIMEOUT}-long probe on top of
     * it.
     *
     * @return true if the alert was there and got tapped
     */
    public static boolean grantTrackingIfShowingNow() {

        return acceptAlertButtonIfShowingNow("Allow");
    }

    /**
     * Single no-wait pass at whatever alert is on screen right now,
     * accepting it only if it offers the exact button given — leaves any
     * other alert alone rather than tapping a wrong button blind. Shares
     * its "mobile: alert" mechanism with {@link #grantAnyShowingNow()};
     * the difference is this one is for a caller that needs to know
     * specifically whether its own alert got answered, not just that
     * something did.
     */
    private static boolean acceptAlertButtonIfShowingNow(String buttonLabel) {

        List<String> buttons;

        try {

            @SuppressWarnings("unchecked")
            List<String> found = (List<String>) DriverManager.getDriver()
                    .executeScript("mobile: alert", Map.of("action", "getButtons"));
            buttons = found;

        } catch (Exception noAlertOrUnsupported) {

            return false;
        }

        if (buttons == null || !buttons.contains(buttonLabel)) {

            return false;
        }

        try {

            DriverManager.getDriver().executeScript("mobile: alert",
                    Map.of("action", "accept", "buttonLabel", buttonLabel));
            return true;

        } catch (Exception acceptFailed) {

            // Same tolerance as grantAnyShowingNow(): a poll loop tries
            // again next pass rather than failing on a transient issue.
            return false;
        }
    }

    /**
     * Camera access prompt ("Allow ... to access your camera?") — same
     * "Allow" button as {@link #allowNotifications()}, named separately
     * for readability at the call site.
     */
    public static boolean allowCamera() {

        // Confirmed live (Single_Order_Complete Phase 2, step by step):
        // "'IM Shopper Stage' would like to access the Camera", buttons
        // "Don't Allow" / "Allow", raised the moment Confirm payment is
        // tapped. Answered through the alert API like every other grant
        // here, not by tapping the "Allow" element (see the class doc
        // comment for why that can silently miss on a system alert).
        return WaitUtils.isVisible(PermissionPopupLocators.ALLOW, ALERT_TIMEOUT)
                && acceptAlertButtonIfShowingNow("Allow");
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

        // Waits for the alert, then answers it through the alert API
        // rather than tap(): confirmed live (Single_Order_Complete Phase
        // 2, step by step) that "mobile: alert" with buttonLabel "Allow"
        // is what reliably lands on this springboard alert — the same
        // mechanism allowNotifications() already uses, for the same
        // reason the class doc comment gives.
        return WaitUtils.isVisible(PermissionPopupLocators.ALLOW, ALERT_TIMEOUT)
                && grantTrackingIfShowingNow();
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
