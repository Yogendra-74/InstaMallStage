package com.instamall.pages.malls;

import com.instamall.base.BasePage;

public class MallsListPageValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(MallsListPageLocators.TITLE);
    }

    public boolean isMallDisplayed(String mallName) {

        return isDisplayed(MallsListPageLocators.mallRow(mallName));
    }

}
