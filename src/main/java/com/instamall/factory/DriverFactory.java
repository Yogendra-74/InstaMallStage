package com.instamall.factory;

import com.instamall.config.ConfigReader;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class DriverFactory {

    private DriverFactory() {

    }

    public static IOSDriver initializeDriver() {

        return initializeDriver(true, ConfigReader.get("appPath"));
    }

    /**
     * @param autoDismissAlerts true keeps the suite stable by letting iOS
     *                          answer system alerts on its own. Pass false
     *                          when a flow needs a permission actually
     *                          granted — the shopping journey does, since
     *                          the app will not show a store feed without
     *                          location access.
     */
    public static IOSDriver initializeDriver(boolean autoDismissAlerts) {

        return initializeDriver(autoDismissAlerts, ConfigReader.get("appPath"));
    }

    /**
     * @param appPath which app's .app bundle to install and launch —
     *                the Customer app and the Shopper app are both
     *                installed on the same simulator, switched between by
     *                which one a session is started with.
     */
    public static IOSDriver initializeDriver(boolean autoDismissAlerts, String appPath) {

        return initializeDriver(autoDismissAlerts, appPath, List.of());
    }

    /**
     * @param processArguments extra launch arguments the app can read off
     *                         {@code ProcessInfo.processInfo.arguments} —
     *                         e.g. a flag a debug/staging build checks for
     *                         to swap out a piece of system UI the
     *                         Simulator cannot drive (no camera hardware)
     *                         for a fixture. Left empty for every class
     *                         that does not ask for one, so this changes
     *                         nothing for them; an app build that does not
     *                         itself check for the flag also sees nothing
     *                         change.
     */
    public static IOSDriver initializeDriver(boolean autoDismissAlerts, String appPath,
                                              List<String> processArguments) {

        try {

            XCUITestOptions options = new XCUITestOptions();

            options.setPlatformName(ConfigReader.get("platformName"));
            options.setAutomationName(ConfigReader.get("automationName"));
            options.setDeviceName(ConfigReader.get("deviceName"));
            options.setPlatformVersion(ConfigReader.get("platformVersion"));
            options.setApp(appPath);

            // Auto-dismissing keeps screen tests stable, but it answers
            // the location prompt with a refusal — and the app then shows
            // "Instamall is not live in your area yet" instead of a store
            // feed. Flows that need the permission granted opt out and
            // handle the alerts through the SystemAlerts component.
            options.setAutoDismissAlerts(autoDismissAlerts);

            // Reuse WebDriverAgent for faster session startup.
            options.setUseNewWDA(false);

            // The first session on a freshly booted simulator has to build
            // and install WebDriverAgent for that runtime, which runs well
            // past the driver's one-minute default — seen on iOS 27.0,
            // where the first attempt failed with "WebDriverAgent is not
            // initialized". Give it room, and one retry.
            options.setWdaLaunchTimeout(Duration.ofMinutes(4));
            options.setWdaStartupRetries(2);

            if (!processArguments.isEmpty()) {

                options.setCapability("appium:processArguments",
                        Map.of("args", processArguments));
            }

            // Left at WebDriverAgent's default on purpose. Setting
            // waitForIdleTimeout to zero is the usual advice for React
            // Native, and it was measured here: the home assertions, which
            // are pure element lookups with no app work in them, came out
            // at 6.5s against 6.3s with it left alone, and every other
            // segment landed inside the noise. The per-lookup cost in this
            // app is the accessibility-tree snapshot, which this setting
            // does not touch — so it buys nothing and only makes taps more
            // likely to land mid-animation.

            return new IOSDriver(
                    new URL(ConfigReader.get("appiumServerURL")),
                    options
            );

        } catch (Exception e) {

            throw new RuntimeException("Unable to initialize iOS Driver", e);
        }
    }

}