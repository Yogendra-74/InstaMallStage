package com.instamall.pages.checkout;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.utils.CurrencyUtils;
import com.instamall.utils.ScreenSnapshot;
import com.instamall.utils.ScreenSnapshot.Item;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public class CheckoutPage extends BasePage {

    // Delivery windows and fees are quoted on entry.
    private static final Duration QUOTE_TIMEOUT = Duration.ofSeconds(45);

    // How long to give the payment sheet before deciding a tap was lost.
    private static final Duration TAP_SETTLED_TIMEOUT = Duration.ofSeconds(12);

    // How long the summary gets to finish painting: the quote refreshes
    // after a delivery speed is chosen, and a label can be on screen a
    // moment before its amount is.
    private static final Duration CHARGES_SETTLE_TIMEOUT = Duration.ofSeconds(10);

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
            this.charges = Collections.unmodifiableMap(charges);
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
         * Whether every charge label on screen had an amount on its
         * line. False while the quote is still painting.
         */
        boolean isChargesComplete() {

            return chargesComplete;
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
     * Waits for the summary to finish painting, then returns the screen
     * with its charges.
     *
     * Past CHARGES_SETTLE_TIMEOUT whatever was last read is returned,
     * and the caller's own checks say what is missing.
     */
    public Screen awaitSummary() {

        AtomicReference<Screen> lastRead = new AtomicReference<>();

        try {

            return WaitUtils.waitUntil(driver -> {

                Screen screen = read();

                lastRead.set(screen);

                return screen.isChargesComplete() && !screen.charges().isEmpty() ? screen : null;

            }, CHARGES_SETTLE_TIMEOUT);

        } catch (TimeoutException stillIncomplete) {

            return lastRead.get() != null ? lastRead.get() : read();
        }
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

    /**
     * Pairs each charge label with the amount on its line, top to bottom.
     *
     * Paired by line rather than taken as every amount on the screen:
     * the store group prints "$14.99 delivery" and each item its price,
     * and those would otherwise be summed in. The rows sit below the
     * fold, so visibility is not required: the snapshot carries their
     * names and frames either way.
     *
     * @return false if a label on screen had no amount on its line — the
     *         summary is mid-render, and the read should be taken again
     */
    private static boolean readCharges(ScreenSnapshot screen, Map<String, BigDecimal> into) {

        List<Item> labels = screen.where(item -> item.isStaticText()
                && CheckoutPageLocators.CHARGE_ROW_LABELS.contains(item.name));

        // "-$42.11" for a Rewards discount, "$16.00" for everything else.
        List<Item> amounts = screen.where(item -> item.isStaticText()
                && (item.nameStartsWith("$") || item.nameStartsWith("-$")));

        labels.sort((first, second) -> Integer.compare(first.y, second.y));

        for (Item label : labels) {

            Optional<Item> amount = amounts.stream()
                    .filter(candidate -> candidate.onSameLineAs(label))
                    .findFirst();

            if (amount.isEmpty()) {

                return false;
            }

            into.put(label.name, CurrencyUtils.parse(amount.get().name));
        }

        return true;
    }

    private static Predicate<Item> text(String exact) {

        return item -> item.isStaticText() && item.named(exact);
    }

}
