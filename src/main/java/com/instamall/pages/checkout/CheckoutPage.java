package com.instamall.pages.checkout;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.utils.ScreenSnapshot;
import com.instamall.utils.ScreenSnapshot.Item;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CheckoutPage extends BasePage {

    // Delivery windows and fees are quoted on entry.
    private static final Duration QUOTE_TIMEOUT = Duration.ofSeconds(45);

    // How long to give the payment sheet before deciding a tap was lost.
    private static final Duration TAP_SETTLED_TIMEOUT = Duration.ofSeconds(12);

    // How long the summary gets to finish painting: the quote refreshes
    // after a delivery speed is chosen, and a label can be on screen a
    // moment before its amount is.
    private static final Duration QUOTE_REPAINT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Everything a caller asks of the checkout screen, read in one go.
     *
     * One page-source snapshot answers which mode the screen opened in,
     * whether the delivery form is complete, which address it is
     * delivering to, what the bottom button says and what the summary
     * charges. Asked one locator at a time, as this used to be, those
     * same questions were some forty-five WebDriver round trips before
     * the Priority tap and another fifty before "Get it Today" — the
     * 10-15s this screen sat idle twice over, confirmed in the Appium
     * log at roughly a quarter of a second per call.
     */
    public static final class Screen {

        private final boolean open;
        private final boolean pickupMode;
        private final boolean deliveryFormComplete;
        private final String addressRow;
        private final String placeOrderLabel;

        private Screen(boolean open, boolean pickupMode, boolean deliveryFormComplete,
                       String addressRow, String placeOrderLabel) {

            this.open = open;
            this.pickupMode = pickupMode;
            this.deliveryFormComplete = deliveryFormComplete;
            this.addressRow = addressRow;
            this.placeOrderLabel = placeOrderLabel;
        }

        /**
         * Whether the checkout is on screen at all, in either mode — the
         * header each mode carries, since a pickup checkout has no
         * "Deliver to" and a delivery one no "Pickup from".
         */
        public boolean isOpen() {

            return open;
        }

        /**
         * Opened for collection rather than delivery: no delivery
         * section, no Priority option and no "Switch to pickup" — the
         * slot is chosen and the order placed directly.
         */
        public boolean isPickupMode() {

            return pickupMode;
        }

        /**
         * "Deliver to", Priority and Schedule all rendered.
         */
        public boolean isDeliveryFormComplete() {

            return deliveryFormComplete;
        }

        /**
         * The delivery address row's full name — "Deliver to. Z9
         * Ventures 38 NW 24th St…" — or null when no address populated
         * it yet.
         */
        public String addressRow() {

            return addressRow;
        }

        /**
         * Case-insensitive on purpose — the same address is uppercase on
         * the home header and in the saved-address list ("Z9 VENTURES")
         * but title case here.
         */
        public boolean isDeliveringTo(String addressName) {

            return addressRow != null
                    && addressRow.toLowerCase().contains(addressName.toLowerCase());
        }




        /**
         * The bottom button's copy, which follows the delivery speed —
         * "Schedule Delivery" while Schedule is chosen, "Get it Today"
         * once Priority is.
         */
        public String placeOrderLabel() {

            return placeOrderLabel;
        }
    }

    /**
     * Reads the screen as it is right now.
     */
    public Screen read() {

        ScreenSnapshot screen = ScreenSnapshot.capture();

        boolean deliverTo = screen.has(text(CheckoutPageLocators.DELIVER_TO_HEADER_TEXT));
        boolean pickupFrom = screen.has(text(CheckoutPageLocators.PICKUP_FROM_HEADER_TEXT));
        boolean switchToDelivery = screen.has(text(CheckoutPageLocators.SWITCH_TO_DELIVERY_TEXT));
        boolean priority = screen.has(item -> item.nameStartsWith(CheckoutPageLocators.PRIORITY_PREFIX));
        boolean schedule = screen.has(item -> item.nameStartsWith(CheckoutPageLocators.SCHEDULE_PREFIX));

        String addressRow = screen.first(item -> item.isButton()
                        && item.nameStartsWith(CheckoutPageLocators.ADDRESS_ROW_PREFIX))
                .map(item -> item.name)
                .orElse(null);

        // Present, not visible — the snapshot deliberately carries no
        // visibility (see ScreenSnapshot). The button is pinned to the
        // bottom of the sheet, so present is on screen; and placeOrder()
        // still goes through a visible == 1 locator for the tap itself.
        String placeOrderLabel = screen.first(item ->
                        item.nameStartsWith(CheckoutPageLocators.GET_IT_PREFIX)
                                || item.nameStartsWith(CheckoutPageLocators.SCHEDULE_DELIVERY_PREFIX))
                .map(item -> item.name)
                .orElse(null);

        return new Screen(
                deliverTo || pickupFrom,
                switchToDelivery || pickupFrom,
                deliverTo && priority && schedule,
                addressRow,
                placeOrderLabel);
    }

    /**
     * Waits for the checkout screen to be fully up in whichever mode it
     * opened: the place-order button, which both forms carry, plus the
     * delivery form's own three pieces when it is a delivery.
     *
     * Waiting on "Deliver to" or Priority alone made a pickup checkout
     * look like a screen that never loaded.
     */
    public Screen awaitScreen() {

        return WaitUtils.waitUntil(driver -> {

            Screen screen = read();

            boolean ready = screen.placeOrderLabel() != null
                    && (screen.isPickupMode() || screen.isDeliveryFormComplete());

            return ready ? screen : null;

        }, QUOTE_TIMEOUT);
    }

    /**
     * Re-reads the screen once the quote has had a moment to repaint —
     * what the caller needs after switching delivery speed, since the
     * place-order button's copy changes with it.
     */
    public Screen awaitSummary() {

        return WaitUtils.waitUntil(driver -> {

            Screen screen = read();

            return screen.placeOrderLabel() != null ? screen : null;

        }, QUOTE_REPAINT_TIMEOUT);
    }

    /**
     * Switches to Priority, which also relabels the bottom button to
     * "Get it Today".
     */
    public CheckoutPage selectPriorityDelivery() {

        click(CheckoutPageLocators.PRIORITY_OPTION);

        WaitUtils.waitForVisibility(CheckoutPageLocators.GET_IT_TODAY_BUTTON);

        return this;
    }

    // NOTE: no tip method. The chips ($5 … $20) sit below the fold, so
    // they report visible == false and a tap can never become clickable
    // without scrolling the summary into view first — and the app already
    // applies a $10 tip by default, so no flow needs to touch them.

    public CheckoutPage switchToPickup() {

        click(CheckoutPageLocators.SWITCH_TO_PICKUP);

        return this;
    }

    /**
     * @return the slots on offer, delivery windows or pickup windows
     *         depending on which mode the screen is in.
     */
    public List<String> listTimeSlots() {

        List<String> slots = new ArrayList<>();

        for (WebElement slot : DriverManager.getDriver()
                .findElements(CheckoutPageLocators.ANY_TIME_SLOT)) {

            String name = slot.getAttribute("name");

            if (name != null && !name.isBlank()) {

                slots.add(name);
            }
        }

        return slots;
    }

    /**
     * Takes the first slot on offer — the journey cares that an order can
     * be placed, not which window it lands in.
     *
     * @return the slot that was chosen.
     */
    public String selectFirstTimeSlot() {

        WaitUtils.waitForVisibility(CheckoutPageLocators.ANY_TIME_SLOT, QUOTE_TIMEOUT);

        List<String> slots = listTimeSlots();

        if (slots.isEmpty()) {

            throw new IllegalStateException("Checkout is offering no time slots");
        }

        String slot = slots.get(0);

        click(CheckoutPageLocators.timeSlot(slot));

        return slot;
    }

    /**
     * Opens the payment sheet. The order is only created once payment
     * goes through.
     */
    public CheckoutPage placeOrder() {

        // Pinned to the bottom of a scrolling sheet.
        settle(CheckoutPageLocators.PLACE_ORDER_BUTTON);

        click(CheckoutPageLocators.PLACE_ORDER_BUTTON);

        // The summary re-renders while the quote refreshes, and a tap that
        // lands mid-render is dropped without a trace. Once the payment
        // sheet is up the checkout behind it stops reporting visible, so
        // this button still being on screen means the tap went nowhere.
        //
        // isInvisible() rather than isVisible(): the tap succeeding is the
        // common case, and isVisible(locator, timeout) only returns
        // quickly when the element IS there — when it is already gone (a
        // tap that landed), it has no way to say so before the full
        // timeout elapses. Confirmed live: that cost a flat
        // TAP_SETTLED_TIMEOUT (12s) on every single successful tap.
        if (!WaitUtils.isInvisible(
                CheckoutPageLocators.PLACE_ORDER_BUTTON, TAP_SETTLED_TIMEOUT)) {

            System.out.println(">>> Place-order tap did not take — tapping again.");

            click(CheckoutPageLocators.PLACE_ORDER_BUTTON);
        }

        return this;
    }

    public CheckoutPage goBack() {

        click(CheckoutPageLocators.BACK_BUTTON);

        return this;
    }

    // ---------- reading the summary ----------

    private static Predicate<Item> text(String exact) {

        return item -> item.isStaticText() && item.named(exact);
    }

}
