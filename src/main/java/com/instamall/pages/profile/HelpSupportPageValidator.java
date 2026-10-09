package com.instamall.pages.profile;

import com.instamall.base.BasePage;
import io.appium.java_client.AppiumBy;

public class HelpSupportPageValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(HelpSupportPageLocators.TITLE);
    }

    public boolean isSectionDisplayed(String title) {

        return isDisplayed(HelpSupportPageLocators.section(title));
    }

    public boolean isQuestionDisplayed(String text) {

        return isDisplayed(HelpSupportPageLocators.question(text));
    }

    /**
     * Whether a question's answer is expanded. The question is a Button
     * whose value reads "expanded" while its answer is open, and carries
     * no value while collapsed. (Before the redesign the answer was
     * appended to the row's name instead.)
     */
    public boolean isQuestionExpanded(String question) {

        return isDisplayed(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name == '" + question + "'"
                        + " AND value == 'expanded' AND accessible == 1"));
    }

}
