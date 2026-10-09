package com.instamall.pages.profile;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * ABOUT & LEGAL screen. Verified against
 * test-output/exploration/profile-about-legal.xml.
 */
public final class AboutLegalPageLocators {

    private AboutLegalPageLocators() {

    }

    public static final By TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name == 'About & legal' AND accessible == 1");

    public static final By TERMS_OF_USE = staticText("Terms of Use");

    public static final By PRIVACY_POLICY = staticText("Privacy Policy");

    public static final By SECTION_YOUR_CONSENTS = staticText("Your consents");

    public static final By CONSENT_TERMS_ROW =
            staticText("Terms of Use & Privacy Policy");

    public static final By VIRTUAL_TRY_ON_LABEL = staticText("AI & Biometric Consent");

    /**
     * The Virtual Try-On consent switch — the only Switch on this screen,
     * and it carries no name. Its "value" attribute reads "1"/"0".
     */
    // Two switches now — AI & Biometric first, AI Stylist under it.
    public static final By VIRTUAL_TRY_ON_SWITCH =
            AppiumBy.iOSClassChain("**/XCUIElementTypeSwitch[1]");

    /**
     * The consent's explainer text. Hidden until the row is tapped —
     * tapping reveals it without flipping the switch (consent is granted
     * inside the try-on flow itself, not from this screen).
     */
    // ---------- Consent confirmation sheet ----------

    /**
     * Tapping a consent switch no longer flips it; it raises a sheet —
     * "Turn off AI Try-On?" (or the turn-on equivalent) — with a button to
     * go ahead and one to keep things as they are.
     */
    public static final By CONSENT_CONFIRMATION_TITLE =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH[c] 'Turn o' AND visible == 1"
            );

    public static final By CONSENT_CONFIRMATION_KEEP =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH 'Keep' AND accessible == 1 AND visible == 1"
            );

    public static final By CONSENT_CONFIRMATION_PROCEED =
            AppiumBy.iOSNsPredicateString(
                    "name BEGINSWITH[c] 'Turn o' AND accessible == 1 AND visible == 1"
                            + " AND type != 'XCUIElementTypeStaticText'"
            );

    public static final By VIRTUAL_TRY_ON_DESCRIPTION =
            AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeStaticText' AND name BEGINSWITH 'Allow AI Try-On' AND accessible == 1");

    // The document links became Buttons, the headings stayed text.
    private static By staticText(String name) {

        return AppiumBy.iOSNsPredicateString(
                "type IN {'XCUIElementTypeStaticText', 'XCUIElementTypeButton'} AND name == '" + name + "'"
                        + " AND accessible == 1");
    }

}
