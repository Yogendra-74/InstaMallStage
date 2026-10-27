package com.instamall.driver;

import com.instamall.config.ConfigReader;

import java.time.Duration;
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
     * How long to wait for "mobile: clearApp" to actually finish putting
     * the app back on the device before giving up and moving on anyway.
     */
    private static final Duration REINSTALL_TIMEOUT = Duration.ofSeconds(40);

    /**
     * Wipes the app's data container (keychain-backed auth tokens
     * included) so the next launch starts signed out.
     *
     * "mobile: clearApp" is not actually a data wipe on iOS — confirmed
     * against the simulator's own system log — it is a real uninstall
     * followed by a reinstall from the "app" capability's own path, and
     * it returns before that finishes: the OS keeps working through it
     * asynchronously for a further ~20s afterward (uninstall, then
     * reinstall, each with its own cascade of system-service
     * notifications). A caller that immediately terminates/activates the
     * app the moment this method returns can land mid-reinstall, which
     * was seen live to corrupt the next launch — a black screen that
     * eventually recovers onto the signed-out landing screen rather than
     * wherever the test actually left off, with nothing past that point
     * ever appearing.
     * Polling isAppInstalled() here, rather than trusting the call to
     * have finished when it returns, is what actually waits out that
     * window.
     *
     * Never throws: if the driver rejects the command, the caller gets a
     * warning and continues — tests handle the already-signed-in case on
     * their own.
     *
     * @return true if the data was cleared and the app confirmed
     *         reinstalled before the timeout; false on either failure.
     */
    public static boolean clearAppData() {

        String bundleId = ConfigReader.get("bundleId");

        try {

            DriverManager.getDriver().executeScript(
                    "mobile: clearApp",
                    Map.of("bundleId", bundleId));

        } catch (Exception exception) {

            System.out.println(">>> Could not clear app data ("
                    + exception.getMessage()
                    + ") — continuing with whatever state the app is in.");

            return false;
        }

        long deadline = System.currentTimeMillis() + REINSTALL_TIMEOUT.toMillis();

        while (System.currentTimeMillis() < deadline) {

            try {

                if (DriverManager.getDriver().isAppInstalled(bundleId)) {

                    return true;
                }

            } catch (Exception ignoredAndRetried) {

                // Transient — the driver/WDA proxy itself can be mid-churn
                // while the OS is still tearing the app down, same window
                // this method exists to wait out.
            }

            try {

                Thread.sleep(500);

            } catch (InterruptedException interrupted) {

                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println(">>> " + bundleId + " did not confirm reinstalled within "
                + REINSTALL_TIMEOUT.toSeconds() + "s — continuing anyway.");

        return false;
    }

}
