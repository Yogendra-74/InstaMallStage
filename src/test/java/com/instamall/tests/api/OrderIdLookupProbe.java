package com.instamall.tests.api;

import com.instamall.api.OrderApi;
import com.instamall.config.ConfigReader;
import com.instamall.support.PlacedOrder;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Optional;

/**
 * Answers one question on its own, without a simulator, an app or an
 * Appium server: can the suite read an order's full Delivery Solutions
 * id back out of the order service at runtime?
 *
 * That id is what Phases 3a and 3b address their webhooks to, and the
 * Customer app never displays it — so until this passes, those phases
 * can only run with {@code -DorderExternalId=…} supplied by hand.
 *
 * Run it against the last order Phase 1 placed:
 *
 *   mvn test -Dtest=OrderIdLookupProbe
 *
 * or against any order:
 *
 *   mvn test -Dtest=OrderIdLookupProbe -DorderId=IM-0THZ8
 *
 * On a miss it does not just fail — {@link OrderApi} prints every field
 * the order service actually returned, which is what identifies the
 * right field without another round trip.
 */
public class OrderIdLookupProbe {

    @Test(description = "Can the full Delivery Solutions order id be read back at runtime?")
    public void canFetchTheFullOrderId() {

        String token = System.getenv("DS_INTERNAL_TOKEN") != null
                ? System.getenv("DS_INTERNAL_TOKEN")
                : ConfigReader.get("dsInternalToken");

        if (token == null || token.isBlank()) {

            throw new SkipException(
                    "No internal secret configured — set DS_INTERNAL_TOKEN in the environment"
                            + " (or dsInternalToken in config.properties). It is the same value"
                            + " the Delivery Solutions webhook calls already use.");
        }

        String orderId = PlacedOrder.resolve(null);

        if (orderId == null) {

            throw new SkipException(
                    "No order to look up — run Phase 1 first, or pass -DorderId=IM-XXXXX.");
        }

        System.out.println(">>> Looking up the Delivery Solutions id for order " + orderId);

        Optional<String> externalOrderId = OrderApi.findExternalOrderId(orderId);

        Assert.assertTrue(externalOrderId.isPresent(),
                "Could not read a Delivery Solutions order id for " + orderId + "."
                        + " The lookup's output above lists every field the order service"
                        + " returned — if the id is in there under a name this does not know,"
                        + " add it to OrderApi.LIKELY_KEYS; if it is not in there at all, the"
                        + " backend does not store it against the order and the webhook"
                        + " receiver needs to match on the app's own id instead.");

        System.out.println(">>> Phases 3a and 3b can run unattended: " + externalOrderId.get());
    }

}
