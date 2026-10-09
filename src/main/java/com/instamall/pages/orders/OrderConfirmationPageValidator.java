package com.instamall.pages.orders;

import com.instamall.base.BasePage;

import java.time.Duration;

public class OrderConfirmationPageValidator extends BasePage {

    private static final Duration ORDER_CREATION_TIMEOUT = Duration.ofSeconds(90);

    /**
     * Tracker stages of a delivery, in the order the app renders them.
     */
    private static final String[] PROGRESS_STAGES =
            {"PLACED", "SHOPPING", "DISPATCH", "ON THE WAY"};

    /**
     * A collection order tracks differently past the shopping stage —
     * nothing is dispatched and nothing is on its way, it is made ready
     * and then collected.
     */
    private static final String[] PICKUP_PROGRESS_STAGES =
            {"PLACED", "SHOPPING", "READY", "PICKED UP"};

    public boolean isDisplayed() {

        return isDisplayed(OrderConfirmationPageLocators.ORDER_ID, ORDER_CREATION_TIMEOUT);
    }

    public boolean isStatusInProgress() {

        return isDisplayed(OrderConfirmationPageLocators.STATUS_IN_PROGRESS);
    }

    public boolean isStatusMessageDisplayed() {

        return isDisplayed(OrderConfirmationPageLocators.STATUS_MESSAGE);
    }

    public boolean isStatusOrderPlaced() {

        return isDisplayed(OrderConfirmationPageLocators.STATUS_ORDER_PLACED);
    }

    public boolean areAllProgressStagesDisplayed() {

        return areAllStagesDisplayed(PROGRESS_STAGES);
    }

    public boolean areAllPickupStagesDisplayed() {

        return areAllStagesDisplayed(PICKUP_PROGRESS_STAGES);
    }

    private boolean areAllStagesDisplayed(String[] stages) {

        for (String stage : stages) {

            if (!isDisplayed(OrderConfirmationPageLocators.progressStage(stage))) {

                return false;
            }
        }

        return true;
    }

    public boolean isStoreDisplayed(String brand) {

        return isDisplayed(OrderConfirmationPageLocators.storeName(brand));
    }

    /**
     * Presence, not visibility.
     *
     * "Your Order" is the last section on the confirmation, and where it
     * lands depends on how much sits above it — the pickup block runs
     * longer than the delivery one, which pushes the item list past the
     * bottom of the screen. Asserting visibility would make this pass or
     * fail on how tall the rest of the page happened to be, which says
     * nothing about whether the order lists the item. Same reasoning as
     * the checkout summary rows.
     */
    public boolean isOrderedItemDisplayed(String productName) {

        return isPresent(OrderConfirmationPageLocators.orderedItem(productName));
    }

    /**
     * Checks the variant line under the item, e.g.
     * "Size: M | Color: Grain Beige | Qty: 1" — the one place the app
     * echoes back everything that was picked on the product page.
     *
     * Sits directly under the item, so below the fold just as often.
     */
    public boolean isOrderedVariantDisplayed(String size, String color, int quantity) {

        return isPresent(
                OrderConfirmationPageLocators.orderedItemVariant(size, color, quantity));
    }

    // Presence, not visibility: both blocks sit below the fold.
    public boolean isDeliveryDetailsDisplayed() {

        return isPresent(OrderConfirmationPageLocators.DELIVERY_DETAILS_SECTION);
    }

    public boolean isPickupDetailsDisplayed() {

        return isPresent(OrderConfirmationPageLocators.PICKUP_DETAILS_SECTION);
    }

    public boolean isContinueShoppingDisplayed() {

        return isDisplayed(OrderConfirmationPageLocators.CONTINUE_SHOPPING);
    }

    /**
     * True at the fresh "Order received" stage, before the boutique has
     * confirmed anything — see
     * {@link OrderConfirmationPageLocators#STATUS_EXPLAINER_STILL_CONFIRMING}.
     */
    public boolean isStillConfirmingWithBoutique() {

        return isDisplayed(OrderConfirmationPageLocators.STATUS_EXPLAINER_STILL_CONFIRMING);
    }

    public boolean isArrivalNotFinalDisplayed() {

        return isDisplayed(OrderConfirmationPageLocators.ARRIVAL_NOT_FINAL_LABEL)
                && isDisplayed(OrderConfirmationPageLocators.UPDATING_ARRIVAL_TIME);
    }

}
