package com.instamall.tests;

import com.instamall.base.BaseTest;
import com.instamall.config.ConfigReader;
import com.instamall.driver.AppSession;
import com.instamall.driver.DriverManager;
import com.instamall.flows.ProfileEntryFlow;
import com.instamall.pages.location.SelectLocationSheet;
import com.instamall.pages.location.SelectLocationSheetValidator;
import com.instamall.pages.onboarding.OnboardingPageValidator;
import com.instamall.pages.profile.AboutLegalPage;
import com.instamall.pages.profile.AboutLegalPageValidator;
import com.instamall.pages.profile.AccountSettingsPage;
import com.instamall.pages.profile.AccountSettingsPageValidator;
import com.instamall.pages.profile.EditAccountPage;
import com.instamall.pages.profile.EditAccountPageValidator;
import com.instamall.pages.profile.HelpSupportPage;
import com.instamall.pages.profile.HelpSupportPageValidator;
import com.instamall.pages.profile.ManageAddressesPage;
import com.instamall.pages.profile.ManageAddressesPageValidator;
import com.instamall.pages.profile.ProfilePage;
import com.instamall.pages.profile.ProfilePageLocators;
import com.instamall.pages.profile.ProfilePageValidator;
import com.instamall.waits.WaitUtils;
import io.appium.java_client.AppiumBy;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;

/**
 * Profile module — positive and negative scenarios, driven from the
 * screens captured under test-output/exploration/profile-*.xml.
 *
 * Runs on its own throwaway account (profileEmail in config.properties):
 * the edit-account tests change data and the logout test ends the
 * session, neither of which the shared suite account could survive.
 *
 * Ordering is deliberate, via priority:
 *  - read-only checks first (landing, sub-screens),
 *  - then the tests that write and restore (edit account, consents),
 *  - logout dead last (priority 100) — after it the app is signed out.
 *
 * The delete-account row is asserted present but never tapped.
 */
public class ProfileTest extends BaseTest {

    private static final double MIAMI_LATITUDE = 25.7995;
    private static final double MIAMI_LONGITUDE = -80.1998;

    /** How long a rejected save gets to (wrongly) navigate away. */
    private static final Duration REJECTED_SAVE_SETTLE = Duration.ofSeconds(2);

    private final ProfileEntryFlow entryFlow = new ProfileEntryFlow();

    private final ProfilePage profilePage = new ProfilePage();
    private final ProfilePageValidator profileValidator = new ProfilePageValidator();
    private final EditAccountPage editAccountPage = new EditAccountPage();
    private final EditAccountPageValidator editAccountValidator = new EditAccountPageValidator();
    private final ManageAddressesPage addressesPage = new ManageAddressesPage();
    private final ManageAddressesPageValidator addressesValidator = new ManageAddressesPageValidator();
    private final AccountSettingsPage accountSettingsPage = new AccountSettingsPage();
    private final AccountSettingsPageValidator accountSettingsValidator = new AccountSettingsPageValidator();
    private final HelpSupportPage helpSupportPage = new HelpSupportPage();
    private final HelpSupportPageValidator helpSupportValidator = new HelpSupportPageValidator();
    private final AboutLegalPage aboutLegalPage = new AboutLegalPage();
    private final AboutLegalPageValidator aboutLegalValidator = new AboutLegalPageValidator();
    private final SelectLocationSheet locationSheet = new SelectLocationSheet();
    private final SelectLocationSheetValidator locationSheetValidator = new SelectLocationSheetValidator();

    @BeforeClass(alwaysRun = true)
    public void prepareAccount() {

        // Signed out on first launch, so the entry flow signs in as the
        // profile account instead of inheriting whatever ran last.
        AppSession.clearAppData();
        AppSession.grantLocationAndNotifications();
        AppSession.setSimulatedLocation(MIAMI_LATITUDE, MIAMI_LONGITUDE);
    }

    /**
     * Runs after BaseTest#resetApp has relaunched the app: every test
     * begins on the Profile landing screen. The first pass signs in;
     * later passes find the session alive and only navigate.
     */
    @BeforeMethod(alwaysRun = true)
    public void openProfileTab() {

        entryFlow.openProfileTab();
    }

    // ==================== Landing — positive ====================

    @Test(priority = 1,
          description = "Profile landing shows the signed-in account's identity and stat tiles")
    public void verifyLandingShowsAccountIdentity() {

        Assert.assertTrue(
                profileValidator.isUserEmailDisplayed(ConfigReader.get("profileEmail")),
                "Signed-in account email is not shown on the profile header");

        Assert.assertTrue(profileValidator.areStatTilesDisplayed(),
                "WISHLIST / ORDERS / WALLET stat tiles are not all displayed");
    }

    @Test(priority = 2,
          description = "Referral card shows a code and all three share actions")
    public void verifyReferralCardDisplayed() {

        Assert.assertTrue(profileValidator.isReferralCardDisplayed(),
                "Referral card title or code is missing");

        Assert.assertTrue(profileValidator.areReferralActionsDisplayed(),
                "Copy / Invite / QR referral actions are not all displayed");

        String code = profilePage.getReferralCode();

        Assert.assertTrue(code.matches("^[A-Z0-9]{8}$"),
                "Referral code \"" + code + "\" is not an 8-character code");
    }

    @Test(priority = 3,
          description = "Copying the referral code leaves the card intact")
    public void verifyCopyReferralCode() {

        String codeBefore = profilePage.getReferralCode();

        profilePage.copyReferralCode();

        Assert.assertEquals(profilePage.getReferralCode(), codeBefore,
                "Referral code changed after tapping copy");
    }

    @Test(priority = 4,
          description = "All six menu rows are reachable, the last two by scrolling")
    public void verifyAllMenuRowsReachable() {

        Assert.assertTrue(profileValidator.areTopMenuRowsDisplayed(),
                "My profile / Manage addresses / Help & support / About & legal are not all displayed");

        profilePage.scrollToRow("Account settings");

        Assert.assertTrue(
                WaitUtils.isVisible(ProfilePageLocators.visibleMenuRow("Log out")),
                "Log out row is not visible after scrolling to the bottom");
    }

    // ==================== MY PROFILE — positive ====================

    // The redesign dropped the YOUR PROFILE hub: "My profile" opens the
    // EDIT PROFILE form directly, and the preference sections (brand,
    // size, category, colour) are no longer anywhere in the app.

    @Test(priority = 10,
          description = "My profile opens the Edit profile form directly")
    public void verifyMyProfileScreenLayout() {

        profilePage.openMyProfile();
        editAccountPage.waitForScreen();

        Assert.assertTrue(editAccountValidator.isDisplayed(),
                "Edit profile form did not open from My profile");
    }

    // Kept, disabled, for the day the preference screens come back.
    @Test(priority = 11, enabled = false,
          description = "Brand preferences opens and lists brands (screen removed in the redesign)")
    public void verifyBrandPreferencesOpens() {

        profilePage.openMyProfile();

        WaitUtils.waitForVisibility(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND name == 'BRAND PREFERENCES' AND accessible == 1"));

        Assert.assertTrue(
                WaitUtils.isVisible(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeStaticText' AND name == 'Adidas' AND accessible == 1")),
                "Brand list did not render — 'Adidas' not found on the preferences screen");
    }

    // ==================== EDIT ACCOUNT — positive ====================

    @Test(priority = 20,
          description = "Edit Account comes prefilled with the account's data")
    public void verifyEditAccountPrefilled() {

        openEditAccount();

        Assert.assertEquals(editAccountPage.getEmail(), ConfigReader.get("profileEmail"),
                "Email field is not prefilled with the account email");

        Assert.assertFalse(editAccountPage.getFirstName().isEmpty(),
                "First name field came up empty");

        Assert.assertFalse(editAccountPage.getPhone().isEmpty(),
                "Phone field came up empty");
    }

    @Test(priority = 21,
          description = "Saving a changed last name persists it (and is restored after)")
    public void verifyEditAccountSavePersistsLastName() {

        openEditAccount();

        String original = editAccountPage.getLastName();
        String changed = "QaCheck";

        editAccountPage.enterLastName(changed).tapSave();

        // A successful save leaves the form for the Account screen.
        profilePage.waitForScreen();

        openEditAccount();

        try {

            Assert.assertEquals(editAccountPage.getLastName(), changed,
                    "Changed last name did not persist through a save");

        } finally {

            // Leave the account how it was found, whatever the assert said.
            editAccountPage.enterLastName(original).tapSave();
            profilePage.waitForScreen();
        }
    }

    // ==================== EDIT ACCOUNT — negative ====================

    @Test(priority = 30,
          description = "NEGATIVE: an empty first name is not saved")
    public void verifyEditAccountRejectsEmptyFirstName() {

        openEditAccount();

        editAccountPage.clearFirstName().tapSave();

        Assert.assertTrue(editAccountValidator.staysOnScreen(REJECTED_SAVE_SETTLE),
                "Save navigated away with an empty first name — expected the form to reject it");
    }

    @Test(priority = 31,
          description = "NEGATIVE: a malformed email is not saved")
    public void verifyEditAccountRejectsInvalidEmail() {

        openEditAccount();

        editAccountPage.enterEmail("invalid-email").tapSave();

        Assert.assertTrue(editAccountValidator.staysOnScreen(REJECTED_SAVE_SETTLE),
                "Save navigated away with a malformed email — expected the form to reject it");
    }

    @Test(priority = 32,
          description = "NEGATIVE: an incomplete phone number is not saved")
    public void verifyEditAccountRejectsShortPhone() {

        openEditAccount();

        editAccountPage.enterPhone("12345").tapSave();

        Assert.assertTrue(editAccountValidator.staysOnScreen(REJECTED_SAVE_SETTLE),
                "Save navigated away with a 5-digit phone number — expected the form to reject it");
    }

    // ==================== MANAGE ADDRESSES ====================

    @Test(priority = 40,
          description = "Manage Addresses lists the saved address and offers Add New Address")
    public void verifyManageAddressesShowsSavedAddress() {

        profilePage.openManageAddresses();
        addressesPage.waitForScreen();

        Assert.assertTrue(
                addressesValidator.isAddressDisplayed("Z9 Ventures"),
                "Saved address card \"Z9 Ventures\" is not displayed");

        Assert.assertTrue(addressesValidator.isAddNewAddressDisplayed(),
                "Add New Address button is not displayed");
    }

    @Test(priority = 41,
          description = "Add New Address opens the location search sheet")
    public void verifyAddNewAddressOpensLocationSearch() {

        profilePage.openManageAddresses();
        addressesPage.waitForScreen().tapAddNewAddress();

        locationSheet.waitUntilVisible();

        Assert.assertTrue(locationSheetValidator.isSearchBoxDisplayed(),
                "Address search box is not displayed on the sheet");

        Assert.assertTrue(locationSheetValidator.areQuickSlotsDisplayed(),
                "Set Home / Set Work quick slots are not displayed");
    }

    @Test(priority = 42,
          description = "NEGATIVE: gibberish address search suggests nothing")
    public void verifyGibberishAddressSearchSuggestsNothing() {

        String gibberish = "zzqqxvv gibberish 00000";

        profilePage.openManageAddresses();
        addressesPage.waitForScreen().tapAddNewAddress();

        locationSheet.waitUntilVisible();
        locationSheet.searchAddress(gibberish);

        // Give the lookup time to answer before reading the sheet.
        sleep(Duration.ofSeconds(3));

        Assert.assertTrue(locationSheetValidator.isDisplayed(),
                "Location sheet disappeared after a gibberish search");

        Assert.assertTrue(
                DriverManager.getDriver().findElements(AppiumBy.iOSNsPredicateString(
                        "name CONTAINS 'zzqqxvv' AND accessible == 1 AND visible == 1"
                                + " AND type != 'XCUIElementTypeTextField'")).isEmpty(),
                "A suggestion appeared for a gibberish address");
    }

    // ==================== ACCOUNT SETTINGS ====================

    @Test(priority = 50,
          description = "Account Settings shows the account, privacy and actions sections")
    public void verifyAccountSettingsScreen() {

        profilePage.openAccountSettings();
        accountSettingsPage.waitForScreen();

        Assert.assertTrue(accountSettingsValidator.areAllSectionsDisplayed(),
                "YOUR ACCOUNT / PRIVACY / ACCOUNT ACTIONS sections are not all displayed");

        Assert.assertTrue(
                accountSettingsValidator.isEmailShown(ConfigReader.get("profileEmail")),
                "Account email is not shown under YOUR ACCOUNT");

        Assert.assertTrue(accountSettingsValidator.isPhoneShown(),
                "Phone number is not shown under YOUR ACCOUNT");

        // Present and located — deliberately never tapped.
        Assert.assertTrue(accountSettingsValidator.isDeleteAccountRowDisplayed(),
                "Delete account row is not displayed under ACCOUNT ACTIONS");

        Assert.assertTrue(accountSettingsValidator.isAppVersionDisplayed(),
                "App version footer is not displayed");
    }

    // ==================== HELP & SUPPORT ====================

    @Test(priority = 60,
          description = "Help & Support shows the FAQ sections and questions")
    public void verifyHelpSupportShowsFaq() {

        profilePage.openHelpAndSupport();
        helpSupportPage.waitForScreen();

        Assert.assertTrue(helpSupportValidator.isSectionDisplayed("ORDERS & DELIVERY"),
                "ORDERS & DELIVERY section is not displayed");

        Assert.assertTrue(helpSupportValidator.isSectionDisplayed("RETURNS & REFUNDS"),
                "RETURNS & REFUNDS section is not displayed");

        Assert.assertTrue(
                helpSupportValidator.isQuestionDisplayed("How do I track my order?"),
                "First FAQ question is not displayed");
    }

    @Test(priority = 61,
          description = "Tapping an FAQ question expands its answer")
    public void verifyFaqQuestionExpands() {

        String question = "How do I track my order?";

        profilePage.openHelpAndSupport();
        helpSupportPage.waitForScreen();

        Assert.assertFalse(helpSupportValidator.isQuestionExpanded(question),
                "Question is already expanded before being tapped");

        helpSupportPage.tapQuestion(question);

        Assert.assertTrue(helpSupportValidator.isQuestionExpanded(question),
                "Tapping the question did not reveal its answer");
    }

    // ==================== ABOUT & LEGAL ====================

    @Test(priority = 70,
          description = "About & Legal shows the legal links and consents")
    public void verifyAboutLegalScreen() {

        profilePage.openAboutAndLegal();
        aboutLegalPage.waitForScreen();

        Assert.assertTrue(aboutLegalValidator.areLegalLinksDisplayed(),
                "Terms of Use / Privacy Policy links are not displayed");

        Assert.assertTrue(aboutLegalValidator.isConsentsSectionDisplayed(),
                "YOUR CONSENTS section is not displayed");

        Assert.assertTrue(aboutLegalValidator.isVirtualTryOnRowDisplayed(),
                "Virtual Try-On consent row is not displayed");
    }

    /**
     * Observed behavior (redesigned build): tapping the consent switch does
     * NOT flip it on the spot — it raises a "Turn off/on AI Try-On?"
     * confirmation, and only answering that changes anything. (Before the
     * redesign the tap merely revealed the explainer text.) This pins
     * down the part that matters: settings must not silently switch the
     * consent, whichever way it currently points.
     */
    @Test(priority = 71,
          description = "NEGATIVE: Try-On consent cannot be switched silently from settings")
    public void verifyVirtualTryOnNotSwitchableOnFromSettings() {

        profilePage.openAboutAndLegal();
        aboutLegalPage.waitForScreen();

        String before = aboutLegalPage.getVirtualTryOnState();

        aboutLegalPage.toggleVirtualTryOn();

        Assert.assertTrue(aboutLegalPage.isConsentConfirmationShowing(),
                "Tapping the consent switch did not ask for confirmation");

        aboutLegalPage.keepConsentAsIs();

        Assert.assertEquals(aboutLegalPage.getVirtualTryOnState(), before,
                "Consent state changed after declining the confirmation");
    }

    // ==================== LOGOUT — must run last ====================

    @Test(priority = 99,
          description = "Cancelling the logout confirmation keeps the session")
    public void verifyLogoutCancelKeepsSession() {

        profilePage.tapLogout();

        Assert.assertTrue(profileValidator.isLogoutConfirmationDisplayed(),
                "Logout confirmation dialog did not appear");

        profilePage.cancelLogout();

        Assert.assertTrue(
                WaitUtils.isVisible(ProfilePageLocators.visibleMenuRow("Log out"),
                        Duration.ofSeconds(5)),
                "Profile screen did not survive cancelling the logout");
    }

    @Test(priority = 100,
          description = "Confirming logout returns to the onboarding screen")
    public void verifyLogoutReturnsToOnboarding() {

        profilePage.tapLogout();

        Assert.assertTrue(profileValidator.isLogoutConfirmationDisplayed(),
                "Logout confirmation dialog did not appear");

        profilePage.confirmLogout();

        Assert.assertTrue(new OnboardingPageValidator().isLoginButtonDisplayed(),
                "Onboarding screen did not appear after confirming logout");
    }

    // ==================== helpers ====================

    private void openEditAccount() {

        // "My profile" is the Edit profile form now — there is no hub in
        // between.
        profilePage.openMyProfile();
        editAccountPage.waitForScreen();
    }

    private void sleep(Duration duration) {

        try {

            Thread.sleep(duration.toMillis());

        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();
        }
    }

}
