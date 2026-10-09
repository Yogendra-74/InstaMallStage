package com.instamall.pages.profile;

import com.instamall.base.BasePage;

public class ProfilePageValidator extends BasePage {

    // Presence, not visibility: after scrolling down to Log out the top
    // rows are off the top of the screen, but the screen is still Account.
    public boolean isDisplayed() {

        return isPresent(ProfilePageLocators.MENU_MY_PROFILE);
    }

    // ---------- Header ----------

    public boolean isUserNameDisplayed(String displayName) {

        return isDisplayed(ProfilePageLocators.userName(displayName));
    }

    public boolean isUserEmailDisplayed(String email) {

        return isDisplayed(ProfilePageLocators.userEmail(email));
    }

    // ---------- Stats ----------

    public boolean areStatTilesDisplayed() {

        return isDisplayed(ProfilePageLocators.STAT_WISHLIST)
                && isDisplayed(ProfilePageLocators.STAT_ORDERS)
                && isDisplayed(ProfilePageLocators.STAT_WALLET);
    }

    // ---------- Referral card ----------

    public boolean isReferralCardDisplayed() {

        return isDisplayed(ProfilePageLocators.REFERRAL_TITLE)
                && isDisplayed(ProfilePageLocators.REFERRAL_CODE);
    }

    public boolean areReferralActionsDisplayed() {

        return isDisplayed(ProfilePageLocators.COPY_REFERRAL_CODE_BUTTON)
                && isDisplayed(ProfilePageLocators.INVITE_FRIENDS_BUTTON)
                && isDisplayed(ProfilePageLocators.REFERRAL_QR_CODE_BUTTON);
    }

    // ---------- Menu ----------

    /**
     * The rows the screen shows without scrolling. ACCOUNT SETTINGS and
     * LOGOUT are below the fold — assert those via presence, or scroll
     * first with {@link ProfilePage#scrollToRow(String)}.
     */
    // Presence: only "My profile" fits above the tab bar without scrolling.
    public boolean areTopMenuRowsDisplayed() {

        return isPresent(ProfilePageLocators.MENU_MY_PROFILE)
                && isPresent(ProfilePageLocators.MENU_MANAGE_ADDRESSES)
                && isPresent(ProfilePageLocators.MENU_HELP_SUPPORT)
                && isPresent(ProfilePageLocators.MENU_ABOUT_LEGAL);
    }

    public boolean isMenuRowPresent(String title) {

        return isPresent(ProfilePageLocators.menuRow(title));
    }

    // ---------- Logout confirmation ----------

    public boolean isLogoutConfirmationDisplayed() {

        return isDisplayed(ProfilePageLocators.LOGOUT_CONFIRM_MESSAGE)
                && isDisplayed(ProfilePageLocators.LOGOUT_CANCEL)
                && isDisplayed(ProfilePageLocators.LOGOUT_CONFIRM);
    }

}
