package com.instamall.driver;

import com.instamall.config.ConfigReader;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * App-level state control, separate from the WebDriver session itself.
 *
 * BaseTest's terminate + activate gives a fresh screen but keeps the
 * app's stored data, so a signed-in session survives it. Tests that must
 * start signed out need the data wiped as well.
 */
public final class AppSession {

    private AppSession() {

    }

    /**
     * Grants the app the permissions the shopping flow depends on, before
     * it is next launched.
     *
     * This replaces tapping through iOS alerts. Letting the alerts appear
     * means racing them: a tap meant for the app lands on the alert
     * instead, and the flow silently goes nowhere. Granting up front means
     * no alert is ever raised.
     *
     * Never throws: on failure the caller simply meets the alerts, and the
     * SystemAlerts component is there for that.
     *
     * @return true if the permissions were set.
     */
    public static boolean grantLocationAndNotifications() {

        try {

            DriverManager.getDriver().executeScript(
                    "mobile: setPermission",
                    Map.of("bundleId", ConfigReader.get("bundleId"),
                            // Only "yes" / "no" / "unset" are accepted —
                            // "always" is rejected outright.
                            "access", Map.of(
                                    "location", "yes",
                                    "notifications", "yes")));

            return true;

        } catch (Exception exception) {

            System.out.println(">>> Could not pre-grant permissions ("
                    + exception.getMessage()
                    + ") — the app will ask for them instead.");

            return false;
        }
    }

    /**
     * Grants every permission named by the "autoGrantedPermissions" config
     * entry to the given app, before it is next launched — see that
     * property's own comment in config.properties for why. Generalizes
     * {@link #grantLocationAndNotifications()} to any installed app's
     * bundle id and any permission "mobile: setPermission" accepts, so a
     * fresh Shopper-app session gets the same treatment as the Customer
     * app's does.
     *
     * Does not reach App Tracking Transparency — iOS reserves that one
     * from automated granting regardless, same as SystemAlerts.
     * allowTracking()'s own doc comment notes; a real tap (grantAnyShowingNow())
     * still answers that alert when it shows.
     *
     * Never throws: on failure the caller simply meets the alerts, and the
     * SystemAlerts component is there for that.
     *
     * @return true if the permissions were set.
     */
    public static boolean grantConfiguredPermissions(String bundleId) {

        try {

            Map<String, String> access = Arrays.stream(
                            ConfigReader.get("autoGrantedPermissions").split(","))
                    .map(String::trim)
                    .filter(permission -> !permission.isEmpty())
                    .collect(Collectors.toMap(permission -> permission, permission -> "yes"));

            DriverManager.getDriver().executeScript(
                    "mobile: setPermission",
                    Map.of("bundleId", bundleId, "access", access));

            return true;

        } catch (Exception exception) {

            System.out.println(">>> Could not pre-grant permissions for " + bundleId + " ("
                    + exception.getMessage()
                    + ") — the app will ask for them instead.");

            return false;
        }
    }

    /**
     * Pins the simulator's GPS position, so serviceability and distances
     * do not depend on whatever location the machine happens to report.
     *
     * Never throws: a failure here only costs determinism, not the run.
     *
     * @return true if the position was set.
     */
    public static boolean setSimulatedLocation(double latitude, double longitude) {

        try {

            DriverManager.getDriver().executeScript(
                    "mobile: setSimulatedLocation",
                    Map.of("latitude", latitude, "longitude", longitude));

            return true;

        } catch (Exception exception) {

            System.out.println(">>> Could not set the simulated location ("
                    + exception.getMessage() + ") — carrying on with the device's own.");

            return false;
        }
    }

    /**
     * Wipes the app's data container (keychain-backed auth tokens
     * included) so the next launch starts at onboarding.
     *
     * Never throws: if the driver rejects the command, the caller gets a
     * warning and continues — tests handle the already-signed-in case on
     * their own.
     *
     * @return true if the data was cleared.
     */
    public static boolean clearAppData() {

        try {

            DriverManager.getDriver().executeScript(
                    "mobile: clearApp",
                    Map.of("bundleId", ConfigReader.get("bundleId")));

            return true;

        } catch (Exception exception) {

            System.out.println(">>> Could not clear app data ("
                    + exception.getMessage()
                    + ") — continuing with whatever state the app is in.");

            return false;
        }
    }

}
