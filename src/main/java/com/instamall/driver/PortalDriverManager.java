package com.instamall.driver;

import org.openqa.selenium.WebDriver;

/**
 * Same shape as DriverManager, kept separate rather than made generic: the
 * Delivery Portal is a browser session (WebDriver), not a simulator one
 * (IOSDriver), and the two run side by side in flows that need both — the
 * portal to assign a driver, the Shopper app to act on that assignment.
 * One thread-local per kind keeps them from colliding.
 */
public final class PortalDriverManager {

    private PortalDriverManager() {
    }

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    public static WebDriver getDriver() {
        return DRIVER.get();
    }

    public static void setDriver(WebDriver driver) {
        DRIVER.set(driver);
    }

    public static void quitDriver() {

        WebDriver driver = DRIVER.get();

        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}
