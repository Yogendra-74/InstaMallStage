package com.instamall.pages.profile;

import com.instamall.base.BasePage;

public class HelpSupportPage extends BasePage {

    public HelpSupportPage waitForScreen() {

        waitForVisibility(HelpSupportPageLocators.TITLE);

        return this;
    }

    /** Taps a question row to expand (or collapse) its answer. */
    public HelpSupportPage tapQuestion(String text) {

        click(HelpSupportPageLocators.question(text));

        return this;
    }

}
