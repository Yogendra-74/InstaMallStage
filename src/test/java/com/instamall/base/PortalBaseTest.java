package com.instamall.base;

import com.instamall.config.ConfigReader;
import com.instamall.driver.PortalDriverManager;
import com.instamall.factory.PortalDriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Base for tests driving the Delivery Portal (a website, not the Customer
 * or Shopper apps) — BaseTest's counterpart for a browser session instead
 * of a simulator one.
 */
public class PortalBaseTest {

    protected WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void startSession() {

        driver = PortalDriverFactory.initializeDriver();
        PortalDriverManager.setDriver(driver);

        driver.get(ConfigReader.get("deliveryPortalUrl"));
    }

    @AfterClass(alwaysRun = true)
    public void endSession() {

        PortalDriverManager.quitDriver();
    }
}
