package com.instamall.pages.profile;

import com.instamall.base.BasePage;
import com.instamall.waits.WaitUtils;

import java.time.Duration;

public class EditAccountPageValidator extends BasePage {

    public boolean isDisplayed() {

        return isDisplayed(EditAccountPageLocators.TITLE);
    }

    /**
     * Still on the edit form after this long — how a rejected save shows
     * up, since a successful one navigates back to YOUR PROFILE.
     */
    public boolean staysOnScreen(Duration within) {

        try {

            Thread.sleep(within.toMillis());

        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();
        }

        return WaitUtils.isVisibleNow(EditAccountPageLocators.TITLE);
    }

}
