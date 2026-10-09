package com.instamall.pages.malls;

import com.instamall.base.BasePage;

public class MallsListPage extends BasePage {

    public MallsListPage waitForScreen() {

        waitForVisibility(MallsListPageLocators.TITLE);

        return this;
    }

    /**
     * Opens a mall by name.
     *
     * KNOWN LIMITATION: the screen this leads to ("Explore the mall") is
     * not a working per-mall store directory — it was confirmed to show
     * the same static "Your Local Edit" card (Shops at Merrick Park ->
     * Alo Yoga) regardless of which mall was opened, with no way to
     * browse that mall's actual stores from there. Use SearchPage's
     * Stores tab to reach a specific store instead of this navigation.
     */
    public MallsListPage openMall(String mallName) {

        click(MallsListPageLocators.mallRow(mallName));

        return this;
    }

    public MallsListPage goBack() {

        click(MallsListPageLocators.BACK_BUTTON);

        return this;
    }

}
