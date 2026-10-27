package com.instamall.api;

import com.instamall.config.ConfigReader;
import org.testng.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * The delivery-state half of the order lifecycle, driven by posting the
 * webhooks Delivery Solutions itself posts — in place of the browser
 * automation that used to drive the DS portal UI to the same end.
 *
 * The portal was never under test. It was only ever a way to move an
 * order through ORDER_ASSIGNED -&gt; ... -&gt; ORDER_DELIVERED so the Customer
 * and Shopper apps would react, and it cost a hand-solved reCAPTCHA on
 * every run to do it (plus a browser kept alive between phases, and
 * across runs, to avoid solving a second one). These calls reach the
 * same receiver the real DS webhooks reach, with the same payloads, and
 * need no browser at all.
 *
 * Each payload under {@code src/test/resources/delivery/} is a verbatim
 * copy of the matching request in the "Delivery Solution APIs" Postman
 * collection, with one change: {@code orderExternalId} is always the
 * quoted placeholder {@code "{{ORDER_NUMBER}}"}. Several of the
 * collection's own payloads carry a hardcoded id from whenever they were
 * captured, and several more leave the placeholder unquoted, which is
 * not valid JSON; both are normalised when the templates are generated.
 *
 * Everything else in those payloads — tenant, store, driver, vehicle,
 * timestamps, coordinates — is left exactly as captured. They are
 * fixtures, not live data: nothing downstream of these tests reads them,
 * and keeping them byte-identical to the collection means a payload can
 * be diffed against it when the receiver changes.
 */
public final class DeliveryWebhooks {

    private DeliveryWebhooks() {

    }

    /**
     * Two receivers, not one: the collection posts the status events to
     * {@code DS_CALL_URL} and the tracking events to
     * {@code DS_TRACKING_CALL_URL}. Which status goes where is taken
     * from the collection and is not interchangeable.
     */
    private static final String STATUS_URL = "dsCallUrl";

    private static final String TRACKING_URL = "dsTrackingCallUrl";

    private static final String TOKEN = "dsInternalToken";

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    // ==================== Phase 3a ====================

    /** ORDER_ASSIGNED — a courier is assigned and en route to the store. */
    public static void driverAssigned(String externalOrderId) {

        post("driver-assigned.json", externalOrderId, STATUS_URL, "Driver Assigned");
    }

    /** PICKUP_STARTED — the courier has started the pickup leg. */
    public static void pickupStarted(String externalOrderId) {

        post("pickup-started.json", externalOrderId, TRACKING_URL, "Pickup Started");
    }

    /** PICKUP_AT_LOCATION — the courier has arrived at the store. */
    public static void driverAtPickup(String externalOrderId) {

        post("driver-at-pickup.json", externalOrderId, STATUS_URL, "Driver At Pickup");
    }

    // ==================== Phase 3b ====================

    /** PICKUP_COMPLETED — the courier has the items. */
    public static void pickupCompleted(String externalOrderId) {

        post("pickup-completed.json", externalOrderId, STATUS_URL, "Pickup Completed");
    }

    /** OUT_FOR_DELIVERY — on the way to the customer. */
    public static void outForDelivery(String externalOrderId) {

        post("out-for-delivery.json", externalOrderId, TRACKING_URL, "Out For Delivery");
    }

    /** ORDER_DELIVERED — dropped off. */
    public static void delivered(String externalOrderId) {

        post("delivered.json", externalOrderId, STATUS_URL, "Delivered");
    }

    // ==================== The call itself ====================

    private static void post(String template, String externalOrderId,
                             String urlKey, String describedAs) {

        String url = required(urlKey);
        String body = payload(template, externalOrderId);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("x-internal-secret", required(TOKEN))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        System.out.println(">>> " + describedAs + " -> POST " + url
                + " (orderExternalId " + externalOrderId + ")");

        HttpResponse<String> response;

        try {

            response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (IOException | InterruptedException failed) {

            if (failed instanceof InterruptedException) {

                Thread.currentThread().interrupt();
            }

            throw new AssertionError(describedAs + " could not be posted to " + url
                    + " — " + failed.getMessage(), failed);
        }

        System.out.println(">>> " + describedAs + " <- HTTP " + response.statusCode()
                + (response.body().isBlank() ? "" : " " + truncated(response.body())));

        // A 2xx is all the receiver gives us. It means the event was
        // accepted, NOT that it matched an order — an unknown
        // orderExternalId is accepted and dropped just as quietly, which
        // is why PlacedOrder.requireExternalId refuses to let a phase
        // get this far without a real one.
        Assert.assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                describedAs + " was rejected: HTTP " + response.statusCode()
                        + " from " + url + " — " + truncated(response.body()));

        // Space the events out by a second each, rather than firing the
        // whole 3a/3b sequence back to back in well under a second: the
        // real delivery flow sends them one after another over time, and
        // giving the receiver a moment to settle each state before the
        // next arrives keeps the transitions in order.
        pause(EVENT_SPACING);
    }

    /**
     * How long to wait after each webhook before the next one is sent.
     */
    private static final Duration EVENT_SPACING = Duration.ofSeconds(1);

    private static void pause(Duration duration) {

        try {

            Thread.sleep(duration.toMillis());

        } catch (InterruptedException interrupted) {

            Thread.currentThread().interrupt();
        }
    }

    /**
     * The capture-time identifiers the templates still carry from the
     * November recording, blanked out before each call — see
     * {@link #payload}.
     */
    private static final String RECORDED_ORDER_ID = "69270bb7dfbdb47f3daca918";

    private static final String RECORDED_TRACKING_NUMBER = "del_yD09pJ4ARaW-mAnXJQbPKw";

    /**
     * The template with this run's order in it.
     *
     * The receiver matches an incoming event to an order by
     * {@code orderExternalId} alone — confirmed live: posting
     * ORDER_ASSIGNED → PICKUP_STARTED → PICKUP_AT_LOCATION with the live
     * {@code orderExternalId} moved the Shopper app's card to "READY FOR
     * HANDOFF" / "START HANDOFF", exactly as the delivery flow expects.
     *
     * But only once the stale {@code orderId} and {@code trackingNumber}
     * the templates still carry from the November recording are blanked.
     * Left in, the receiver keyed off one of them, matched nothing, and
     * still answered 201 "Request successful" — so every event was
     * accepted and silently dropped and the order never progressed. The
     * live {@code trackingNumber}/{@code orderId} Delivery Solutions
     * issues for a fresh order are not available to the suite (the order
     * service rejects this internal secret with 401), so they are cleared
     * rather than filled — the order is identified by
     * {@code orderExternalId}, which is all the receiver needs.
     */
    private static String payload(String template, String externalOrderId) {

        Assert.assertNotNull(externalOrderId,
                "No orderExternalId to put in " + template
                        + " — see PlacedOrder.requireExternalId");

        String resource = "delivery/" + template;

        try (InputStream in = DeliveryWebhooks.class.getClassLoader()
                .getResourceAsStream(resource)) {

            if (in == null) {

                throw new AssertionError("Missing payload template on the classpath: " + resource);
            }

            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{ORDER_NUMBER}}", externalOrderId)
                    .replace(RECORDED_ORDER_ID, "")
                    .replace(RECORDED_TRACKING_NUMBER, "");

        } catch (IOException unreadable) {

            throw new AssertionError("Could not read " + resource
                    + " — " + unreadable.getMessage(), unreadable);
        }
    }

    /**
     * A config value that the call cannot be made without, from the
     * environment first so a CI run can supply the token without it
     * being committed in config.properties.
     *
     * The environment variable is the key in SCREAMING_SNAKE_CASE:
     * {@code dsCallUrl} -&gt; {@code DS_CALL_URL}.
     */
    private static String required(String key) {

        String fromEnvironment = System.getenv(environmentVariable(key));

        if (fromEnvironment != null && !fromEnvironment.isBlank()) {

            return fromEnvironment.trim();
        }

        String fromConfig = ConfigReader.get(key);

        Assert.assertTrue(fromConfig != null && !fromConfig.isBlank(),
                "No " + key + " configured: set " + environmentVariable(key)
                        + " in the environment, or " + key + " in config.properties."
                        + " The Delivery Solutions webhook calls cannot run without it.");

        return fromConfig.trim();
    }

    private static String environmentVariable(String key) {

        return key.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase();
    }

    private static String truncated(String body) {

        String collapsed = body.strip().replaceAll("\\s+", " ");

        return collapsed.length() <= 300 ? collapsed : collapsed.substring(0, 300) + "…";
    }

}
