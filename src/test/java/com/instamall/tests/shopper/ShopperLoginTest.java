package com.instamall.tests.shopper;

import com.instamall.base.ShopperBaseTest;
import com.instamall.config.ConfigReader;
import com.instamall.pages.shopper.home.ShopperHomePage;
import com.instamall.pages.shopper.home.ShopperHomePageValidator;
import com.instamall.pages.shopper.login.ShopperLoginPage;
import com.instamall.pages.shopper.login.ShopperLoginPageValidator;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ShopperLoginTest extends ShopperBaseTest {

    private final ShopperLoginPage loginPage = new ShopperLoginPage();
    private final ShopperLoginPageValidator loginValidator = new ShopperLoginPageValidator();
    private final ShopperHomePage homePage = new ShopperHomePage();
    private final ShopperHomePageValidator homeValidator = new ShopperHomePageValidator();

    @Test(description = "All key elements are visible on the Shopper login screen")
    public void verifyLoginScreenElementsAreDisplayed() {

        loginPage.waitForScreen();

        Assert.assertTrue(loginValidator.isEmailFieldDisplayed(),
                "Email field is not displayed");

        Assert.assertTrue(loginValidator.isPasswordFieldDisplayed(),
                "Password field is not displayed");

        Assert.assertTrue(loginValidator.isLogInButtonDisplayed(),
                "Log in button is not displayed");
    }

    @Test(description = "A certified Shopper can log in and reach the dashboard")
    public void verifyShopperCanLogIn() {

        loginPage.waitForScreen()
                 .enterEmail(ConfigReader.get("shopperEmail"))
                 .enterPassword(ConfigReader.get("shopperPassword"))
                 .clickLogIn();

        homePage.waitForScreen();

        Assert.assertTrue(homeValidator.isActiveTabDisplayed(),
                "Dashboard's ACTIVE tab is not displayed after login");

        Assert.assertTrue(homeValidator.isHistoryTabDisplayed(),
                "Dashboard's HISTORY tab is not displayed after login");

        Assert.assertTrue(homeValidator.areAllTabsDisplayed(),
                "Bottom tab bar is incomplete on the dashboard");
    }

}
