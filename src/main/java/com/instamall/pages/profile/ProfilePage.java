package com.instamall.pages.profile;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.By;

import java.time.Duration;
import java.util.Map;

public class ProfilePage extends BasePage {

    /**
     * The landing screen paints from the network (name, email, stats,
     * referral code), so its first appearance gets the longer timeout.
     */
    private static final Duration FIRST_PAINT_TIMEOUT = Duration.ofSeconds(30);

    public ProfilePage waitForScreen() {

        // Presence, not visibility: coming back to the tab keeps the list's
        // scroll position, so "My profile" is often off the top of the
        // screen while the screen itself is plainly Account.
        WaitUtils.waitForPresence(
                ProfilePageLocators.MENU_MY_PROFILE, FIRST_PAINT_TIMEOUT);

        return this;
    }

    // ---------- Menu navigation ----------

    public ProfilePage openMyProfile() {

        return openRow("My profile");
    }

    public ProfilePage openManageAddresses() {

        return openRow("Manage addresses");
    }

    /**
     * ACCOUNT SETTINGS sits below the fold, so it is scrolled to first.
     */
    public ProfilePage openAccountSettings() {

        scrollToRow("Account settings");

        click(ProfilePageLocators.visibleMenuRow("Account settings"));

        return this;
    }

    public ProfilePage openHelpAndSupport() {

        return openRow("Help & support");
    }

    public ProfilePage openAboutAndLegal() {

        return openRow("About & legal");
    }

    /**
     * Scrolls a menu row into view and taps it. On the redesigned Account
     * screen everything from "Manage addresses" down starts under the tab
     * bar, where a tap cannot reach it.
     */
    private ProfilePage openRow(String title) {

        scrollToRow(title);
        click(ProfilePageLocators.visibleMenuRow(title));

        return this;
    }

    /**
     * Opens the logout confirmation dialog ("Are you sure you want to log
     * out?"). Nothing is ended yet — follow with {@link #confirmLogout()}
     * or {@link #cancelLogout()}.
     */
    public ProfilePage tapLogout() {

        scrollToRow("Log out");

        click(ProfilePageLocators.visibleMenuRow("Log out"));

        return this;
    }

    /**
     * Ends the session. The caller owns the consequences — every test
     * after this one meets the signed-out onboarding screen.
     */
    public ProfilePage confirmLogout() {

        click(ProfilePageLocators.LOGOUT_CONFIRM);

        return this;
    }

    public ProfilePage cancelLogout() {

        click(ProfilePageLocators.LOGOUT_CANCEL);

        return this;
    }

    // ---------- Referral card ----------

    public String getReferralCode() {

        return getText(ProfilePageLocators.REFERRAL_CODE);
    }

    public ProfilePage copyReferralCode() {

        click(ProfilePageLocators.COPY_REFERRAL_CODE_BUTTON);

        return this;
    }

    // ---------- Stats ----------

    /**
     * @return the wallet tile's full text, e.g. "$0.00 WALLET".
     */
    public String getWalletTileText() {

        return waitForVisibility(ProfilePageLocators.STAT_WALLET)
                .getAttribute("name");
    }

    public String getWishlistTileText() {

        return waitForVisibility(ProfilePageLocators.STAT_WISHLIST)
                .getAttribute("name");
    }

    public String getOrdersTileText() {

        return waitForVisibility(ProfilePageLocators.STAT_ORDERS)
                .getAttribute("name");
    }

    public ProfilePage openOrders() {

        click(ProfilePageLocators.STAT_ORDERS);

        return this;
    }

    // ---------- Scrolling ----------

    /**
     * Swipes until the named menu row reports itself visible. Two swipes
     * cover this screen end to end today; the bound is there so a renamed
     * row fails with a message instead of scrolling forever.
     */
    public ProfilePage scrollToRow(String title) {

        By visibleRow = ProfilePageLocators.visibleMenuRow(title);

        for (int swipes = 0; swipes < 4; swipes++) {

            if (WaitUtils.isVisibleNow(visibleRow)) {

                return this;
            }

            swipeUp();
        }

        if (!WaitUtils.isVisible(visibleRow, Duration.ofSeconds(2))) {

            throw new IllegalStateException(
                    "Menu row \"" + title + "\" not found after scrolling the profile screen");
        }

        return this;
    }

    private void swipeUp() {

        DriverManager.getDriver().executeScript(
                "mobile: swipe", Map.of("direction", "up"));
    }

}
