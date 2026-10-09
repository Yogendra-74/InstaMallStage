package com.instamall.pages.orders;

import com.instamall.base.BasePage;

import java.time.Duration;

public class OrderConfirmationPageValidator extends BasePage {

    private static final Duration ORDER_CREATION_TIMEOUT = Duration.ofSeconds(90);

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

}
