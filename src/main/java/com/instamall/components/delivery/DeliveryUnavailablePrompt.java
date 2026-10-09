package com.instamall.components.delivery;

import com.instamall.waits.WaitUtils;

import java.time.Duration;

/**
 * The "Delivery not available at this time" message.
 *
 * It is a toast, not a dialog: it carries no button and clears itself
 * after a few seconds. The app raises it when the order cannot be
 * delivered to the chosen address in the chosen window — tapping the
 * place-order button simply does nothing else.
 *
 * Collection is the way through, so callers wait for it to clear and then
 * switch the order to pickup.
 */
public final class DeliveryUnavailablePrompt {

    private static final Duration APPEARS_WITHIN = Duration.ofSeconds(5);
    private static final Duration CLEARS_WITHIN = Duration.ofSeconds(20);

    private DeliveryUnavailablePrompt() {

    }

    public static boolean isShowing() {

        return WaitUtils.isVisible(
                DeliveryUnavailablePromptLocators.PROMPT, APPEARS_WITHIN);
    }

    /**
     * Up at this instant, without waiting.
     *
     * Lets a caller watch for this toast at the same time as it watches
     * for the payment sheet, instead of sitting out the sheet's deadline
     * first — by which time this toast has cleared itself and the reason
     * the order did not go through has gone unseen.
     */
    public static boolean isShowingNow() {

        return WaitUtils.isVisibleNow(DeliveryUnavailablePromptLocators.PROMPT);
    }

    /**
     * Waits for the toast to clear so the screen underneath can be tapped
     * again — a tap that lands while it is up goes nowhere.
     *
     * @return true if the toast was there, false if nothing appeared.
     */
    public static boolean waitUntilGone() {

        if (!isShowing()) {

            return false;
        }

        System.out.println(">>> \"Delivery not available\" — waiting for it to clear.");

        WaitUtils.waitForInvisibility(
                DeliveryUnavailablePromptLocators.PROMPT, CLEARS_WITHIN);

        return true;
    }

}
