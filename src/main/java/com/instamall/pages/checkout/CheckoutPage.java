package com.instamall.pages.checkout;

import com.instamall.base.BasePage;
import com.instamall.driver.DriverManager;
import com.instamall.utils.CurrencyUtils;
import com.instamall.waits.WaitUtils;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CheckoutPage extends BasePage {

    // Delivery windows and fees are quoted on entry.
    private static final Duration QUOTE_TIMEOUT = Duration.ofSeconds(45);

    // How long to give the payment sheet before deciding a tap was lost.
    private static final Duration TAP_SETTLED_TIMEOUT = Duration.ofSeconds(12);

    /**
     * Gates on the delivery section rather than the bottom button, whose
     * copy depends on which speed happens to be selected.
     */
    /**
     * Waits for the checkout screen in either mode.
     *
     * The screen opens for delivery or for collection depending on how the
     * store group is set, and only the delivery form has a "Deliver to"
     * header or a Priority option. Waiting on those was making a pickup
     * checkout look like a screen that never loaded — so what is waited on
     * is the place-order button, which both forms carry.
     */
    public CheckoutPage waitForScreen() {

        WaitUtils.waitForVisibility(
                CheckoutPageLocators.PLACE_ORDER_BUTTON, QUOTE_TIMEOUT);

        return this;
    }

    /**
     * Waits for the delivery form specifically, for a caller that has
     * already established the screen is in that mode.
     */
    public CheckoutPage waitForDeliveryForm() {

        WaitUtils.waitForVisibility(
                CheckoutPageLocators.DELIVER_TO_HEADER, QUOTE_TIMEOUT);

        WaitUtils.waitForVisibility(
                CheckoutPageLocators.PRIORITY_OPTION, QUOTE_TIMEOUT);

        return this;
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

    /**
     * @return the copy on the bottom button, e.g. "Get it Today" or
     *         "Schedule Delivery".
     */
    public String getPlaceOrderLabel() {

        return getText(CheckoutPageLocators.PLACE_ORDER_BUTTON);
    }

    public CheckoutPage selectScheduledDelivery() {

        click(CheckoutPageLocators.SCHEDULE_OPTION);

        return this;
    }

    // NOTE: no tip method. The chips ($5 … $20) sit below the fold, so
    // they report visible == false and a tap can never become clickable
    // without scrolling the summary into view first — and the app already
    // applies a $10 tip by default, so no flow needs to touch them. The
    // locators are kept in CheckoutPageLocators for when scrolling
    // support lands.

    /**
     * Delivery fee as advertised on the Priority option, read out of its
     * label ("Priority $14.99 7:21am–7:38am").
     */
    public BigDecimal getPriorityFee() {

        return CurrencyUtils.parse(getText(CheckoutPageLocators.PRIORITY_OPTION));
    }

    /**
     * Every charge on the summary, in render order: subtotal, delivery,
     * tax, service fee, tip, total.
     *
     * Paired with the row labels by line rather than taken as every
     * amount on the screen: the store group prints "$14.99 delivery" and
     * each item its price, and those would otherwise be summed in.
     *
     * Read by attribute rather than getText(): these rows sit below the
     * fold, and an off-screen element's text is not reliably returned.
     */
    public List<BigDecimal> getCharges() {

        List<WebElement> labels = new ArrayList<>(DriverManager.getDriver()
                .findElements(CheckoutPageLocators.ALL_CHARGE_LABELS));
        List<WebElement> amounts = DriverManager.getDriver()
                .findElements(CheckoutPageLocators.ALL_CHARGE_AMOUNTS);

        labels.sort(Comparator.comparingInt(label -> label.getRect().getY()));

        List<BigDecimal> charges = new ArrayList<>();

        for (WebElement label : labels) {

            int line = label.getRect().getY();

            amounts.stream()
                    .filter(amount -> Math.abs(amount.getRect().getY() - line) < 5)
                    .findFirst()
                    .ifPresent(amount -> charges.add(
                            CurrencyUtils.parse(amount.getAttribute("name"))));
        }

        return charges;
    }

    /**
     * The last charge row, which is the order total.
     */
    public BigDecimal getTotal() {

        List<BigDecimal> charges = getCharges();

        if (charges.isEmpty()) {

            throw new IllegalStateException(
                    "No charge rows found on the checkout screen");
        }

        return charges.get(charges.size() - 1);
    }

    /**
     * Sum of every charge except the last — i.e. what the total should
     * add up to. Reading them positionally keeps this working when the app
     * adds or drops a fee line.
     */
    public BigDecimal sumOfCharges() {

        List<BigDecimal> charges = getCharges();

        if (charges.size() < 2) {

            throw new IllegalStateException(
                    "Expected charge rows plus a total, found " + charges.size());
        }

        BigDecimal sum = BigDecimal.ZERO;

        for (int index = 0; index < charges.size() - 1; index++) {

            sum = sum.add(charges.get(index));
        }

        return sum;
    }

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
        if (WaitUtils.isVisible(
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

}
