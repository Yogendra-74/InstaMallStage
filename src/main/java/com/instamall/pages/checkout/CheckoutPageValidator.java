package com.instamall.pages.checkout;

import com.instamall.base.BasePage;

import java.time.Duration;

public class CheckoutPageValidator extends BasePage {

    private static final Duration QUOTE_TIMEOUT = Duration.ofSeconds(45);

    /**
     * Charge rows the summary always carries, in render order.
     */
    private static final String[] CHARGE_ROWS =
            {"Subtotal", "Delivery", "Estimated tax", "Service fee", "Tip", "Total"};

    public boolean isDisplayed() {

        return isDisplayed(CheckoutPageLocators.PLACE_ORDER_BUTTON, QUOTE_TIMEOUT);
    }

    public boolean isDeliverToSectionDisplayed() {

        return isDisplayed(CheckoutPageLocators.DELIVER_TO_HEADER);
    }

    /**
     * On screen at this instant, without waiting — for racing the checkout
     * screen against the bag's refusal to leave.
     *
     * Either mode counts. A store group already set for collection opens
     * on "Pickup from" and carries no "Deliver to" at all, and looking only
     * for the delivery header made that checkout indistinguishable from one
     * that never opened — which the caller then read as the bag refusing.
     */
    public boolean isDisplayedNow() {

        return isDisplayedNow(CheckoutPageLocators.DELIVER_TO_HEADER)
                || isDisplayedNow(CheckoutPageLocators.PICKUP_FROM_HEADER);
    }

    /**
     * Whether the screen opened for collection rather than delivery, in
     * which case there is no delivery section to read and nothing to
     * switch — the slot is chosen and the order placed directly.
     */
    public boolean isInPickupMode() {

        return isDisplayed(CheckoutPageLocators.SWITCH_TO_DELIVERY)
                || isDisplayed(CheckoutPageLocators.PICKUP_FROM_HEADER);
    }

    public boolean isAddressDisplayed(String addressName) {

        return isDisplayed(CheckoutPageLocators.addressName(addressName));
    }

    public boolean areDeliveryOptionsDisplayed() {

        return isDisplayed(CheckoutPageLocators.PRIORITY_OPTION)
                && isDisplayed(CheckoutPageLocators.SCHEDULE_OPTION);
    }

    public boolean isFarAwayWarningDisplayed() {

        return isDisplayed(CheckoutPageLocators.FAR_AWAY_WARNING);
    }

    public boolean isContactDisplayed() {

        return isDisplayed(CheckoutPageLocators.CONTACT_ROW);
    }

    public boolean isSummaryStoreRowDisplayed(String storeName) {

        return isDisplayed(CheckoutPageLocators.summaryStoreRow(storeName));
    }

    /**
     * Presence check only — these rows sit below the fold, so visibility
     * would be false until the summary is scrolled into view.
     */
    public boolean areAllChargeRowsPresent() {

        for (String label : CHARGE_ROWS) {

            if (!isPresent(CheckoutPageLocators.chargeLabel(label))) {

                return false;
            }
        }

        return true;
    }

    public boolean isChargeRowPresent(String label) {

        return isPresent(CheckoutPageLocators.chargeLabel(label));
    }

    public boolean isReturnPolicyDisplayed() {

        return isDisplayed(CheckoutPageLocators.RETURN_POLICY_LABEL)
                && isDisplayed(CheckoutPageLocators.RETURN_POLICY_TEXT);
    }

}
