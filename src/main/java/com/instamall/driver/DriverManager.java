package com.instamall.driver;

import io.appium.java_client.ios.IOSDriver;

public final class DriverManager {

    private DriverManager() {
    }

    private static final ThreadLocal<IOSDriver> DRIVER = new ThreadLocal<>();

    public static IOSDriver getDriver() {
        return DRIVER.get();
    }

    public static void setDriver(IOSDriver driver) {
        DRIVER.set(driver);
    }

    public static void quitDriver() {

        IOSDriver driver = DRIVER.get();

        if (driver != null) {
            driver.quit();
            DRIVER.remove();
        }
    }
}