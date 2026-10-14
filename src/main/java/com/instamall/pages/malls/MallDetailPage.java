package com.instamall.pages.malls;

import com.instamall.base.BasePage;
import com.instamall.components.permissions.SystemAlerts;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public class MallDetailPage extends BasePage {

    // The store list loads over the network, same as a store's own grid.
    private static final Duration CATALOG_TIMEOUT = Duration.ofSeconds(45);

    /**
     * Polls for a stray permission alert while waiting, the same
     * discipline as HomePage's own post-guest-entry wait: opening this
     * screen is itself a fresh navigation, and a delayed location/
     * notifications alert landing here instead of on Home would
     * otherwise sit unanswered for the whole timeout with nothing
     * retrying it — confirmed live as a real, if intermittent, failure
     * mode on a cold WDA session.
     */
    public MallDetailPage waitForScreen() {

        WaitUtils.waitUntil(driver -> {

            SystemAlerts.grantAnyShowingNow();

            return WaitUtils.isVisibleNow(MallDetailPageLocators.TITLE) ? Boolean.TRUE : null;

        }, CATALOG_TIMEOUT);

        return this;
    }

    /**
     * Opens a store from this mall's own directory by brand name, e.g.
     * "Burberry" — see MallDetailPageLocators.storeRow()'s own doc
     * comment for why this is matched by CONTAINS rather than BEGINSWITH.
     *
     * Scrolled into view first, same reason as HomePage.openMallCard():
     * a store further down this list (Burberry sits below Moncler and
     * Prada here) is off-screen on open, and click() alone times out
     * waiting for an element WDA does not yet report as visible.
     */
    public MallDetailPage openStore(String storeName) {

        WebElement store = WaitUtils.waitForPresence(MallDetailPageLocators.storeRow(storeName), CATALOG_TIMEOUT);

        scrollIntoView(store);

        store.click();

        return this;
    }

}
