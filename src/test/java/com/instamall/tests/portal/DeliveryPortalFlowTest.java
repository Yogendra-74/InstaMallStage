package com.instamall.tests.portal;

import com.instamall.base.PortalBaseTest;
import com.instamall.config.ConfigReader;
import com.instamall.pages.portal.PortalLoginPage;
import com.instamall.pages.portal.PortalOrdersPage;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Real (non-exploratory) steps for the Delivery Portal, built up
 * incrementally as each one is confirmed against the live sandbox. Not yet
 * wired into testng.xml.
 */
public class DeliveryPortalFlowTest extends PortalBaseTest {

    @Test
    @Parameters("orderIdSuffix")
    public void runFlow(@Optional("AE078") String orderIdSuffix) {

        PortalLoginPage loginPage = new PortalLoginPage()
                .waitForScreen()
                .enterEmail(ConfigReader.get("deliveryPortalUsername"))
                .enterPassword(ConfigReader.get("deliveryPortalPassword"));

        // reCAPTCHA stays a manual, human step — clickLogin() waits up to
        // PortalWaitUtils' default timeout for the button to become
        // clickable, which only happens once it's solved.
        loginPage.clickLogin();

        PortalOrdersPage ordersPage = new PortalOrdersPage().openOrdersFromSidebar();

        List<String> orderIds = ordersPage.getAllOrderIds();
        System.out.println(">>> Order ids on page (" + orderIds.size() + "): " + orderIds);

        ordersPage.openOrderEndingWith(orderIdSuffix);
    }

}
