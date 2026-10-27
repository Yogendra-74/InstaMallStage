package com.instamall.api;

import com.instamall.config.ConfigReader;
import org.openqa.selenium.json.Json;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reads an order back out of the InstaMall order service, for the one
 * thing the Customer app never shows: the full id Delivery Solutions
 * knows the order by.
 *
 * The app displays the short form only ("ORDER IM-0THZ8"). The DS
 * webhooks in {@link DeliveryWebhooks} are addressed by a complete
 * {@code orderExternalId}, and a webhook naming an order the receiver
 * cannot find is accepted with a 200 and dropped, so guessing is worse
 * than failing.
 *
 * Authenticated with the same {@code x-internal-secret} the DS webhook
 * calls use, against the order service's {@code internal/} routes. The
 * customer-facing routes want a user JWT instead, and the user service
 * has no password login — it is Firebase-backed — so the internal route
 * keeps this to one credential the suite already needs.
 *
 * WHICH FIELD HOLDS THE ID IS NOT YET CONFIRMED. The order service
 * documents these routes with no response model at all ({@code 200: …
 * {}}), and no schema in any of the nine InstaMall services declares an
 * {@code orderExternalId}. So rather than hardcode a guess, this finds
 * the id by shape — see {@link #externalIdIn} — and
 * {@link #describe} prints the whole document when it cannot, which is
 * what turns one real run into the answer.
 */
public final class OrderApi {

    private OrderApi() {

    }

    private static final String BASE_URL = "orderApiBaseUrl";

    private static final String TOKEN = "dsInternalToken";

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Json JSON = new Json();

    /**
     * A Delivery Solutions order id as the webhooks carry it: a ULID —
     * 26 characters of Crockford base32 — optionally prefixed, as the
     * collection's own return payloads are ("RO-01KWGNMT…").
     */
    private static final Pattern DS_ORDER_ID =
            Pattern.compile("^(?:[A-Z]{2}-)?[0-9ABCDEFGHJKMNPQRSTVWXYZ]{26}$");

    /**
     * Field names worth trusting ahead of shape, most specific first.
     */
    private static final List<String> LIKELY_KEYS = List.of(
            "orderExternalId", "externalOrderId", "externalId",
            "dsOrderId", "deliveryOrderId", "orderNumber");

    /**
     * The full Delivery Solutions id for an order, or empty when the
     * lookup could not produce one — the caller decides whether that is
     * fatal.
     *
     * @param appOrderId the id the Customer app showed ("IM-0THZ8"), or
     *                   the order service's own Mongo id
     */
    public static Optional<String> findExternalOrderId(String appOrderId) {

        Optional<String> body = fetch(appOrderId);

        if (body.isEmpty()) {

            return Optional.empty();
        }

        Map<String, Object> document = parse(body.get());

        if (document == null) {

            return Optional.empty();
        }

        Optional<String> found = externalIdIn(document);

        if (found.isPresent()) {

            System.out.println(">>> Delivery Solutions order id for " + appOrderId
                    + ": " + found.get());

        } else {

            System.out.println(">>> No Delivery Solutions order id found in the response for "
                    + appOrderId + ". Fields that came back:");
            describe(document);
        }

        return found;
    }

    /**
     * GET the order, returning the raw body on a 2xx.
     */
    private static Optional<String> fetch(String appOrderId) {

        String base = ConfigReader.get(BASE_URL);
        String secret = secret();

        if (base == null || base.isBlank() || secret == null || secret.isBlank()) {

            System.out.println(">>> Order lookup skipped: set " + BASE_URL + " and "
                    + TOKEN + " (or DS_INTERNAL_TOKEN in the environment) to enable it.");

            return Optional.empty();
        }

        String url = base.replaceAll("/+$", "")
                + "/orders/internal/details/"
                + URLEncoder.encode(appOrderId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("x-internal-secret", secret)
                .GET()
                .build();

        System.out.println(">>> Order lookup -> GET " + url);

        try {

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 == 2) {

                return Optional.of(response.body());
            }

            System.out.println(">>> Order lookup <- HTTP " + response.statusCode()
                    + " " + shorten(response.body()));

            if (response.statusCode() == 401) {

                System.out.println(">>> The order service rejected the internal secret."
                        + " It should be the same value the DS webhook calls send.");
            }

            if (response.statusCode() == 404 || response.statusCode() == 400) {

                System.out.println(">>> This route may not accept the app's \"" + appOrderId
                        + "\" form — it may want the order service's own Mongo id instead.");
            }

            return Optional.empty();

        } catch (IOException | InterruptedException failed) {

            if (failed instanceof InterruptedException) {

                Thread.currentThread().interrupt();
            }

            System.out.println(">>> Order lookup failed against " + url
                    + " — " + failed.getMessage());

            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String body) {

        try {

            Object parsed = JSON.toType(body, Map.class);

            return parsed instanceof Map ? (Map<String, Object>) parsed : null;

        } catch (RuntimeException notJson) {

            System.out.println(">>> Order lookup returned something that is not a JSON object: "
                    + shorten(body));

            return null;
        }
    }

    /**
     * The Delivery Solutions id somewhere in a parsed order document.
     *
     * Looked for by name first, across the whole tree — the service
     * wraps its payloads in {@code data}, and nothing says the id sits
     * at the top level. Failing that, by shape: a lone ULID-looking
     * string is what a DS order id is, whatever the field is called.
     */
    static Optional<String> externalIdIn(Map<String, Object> document) {

        Map<String, String> flat = new LinkedHashMap<>();
        flatten(document, "", flat);

        for (String key : LIKELY_KEYS) {

            for (Map.Entry<String, String> entry : flat.entrySet()) {

                if (leaf(entry.getKey()).equalsIgnoreCase(key)
                        && !entry.getValue().isBlank()) {

                    return Optional.of(entry.getValue());
                }
            }
        }

        List<String> byShape = new ArrayList<>();

        for (Map.Entry<String, String> entry : flat.entrySet()) {

            if (DS_ORDER_ID.matcher(entry.getValue()).matches()) {

                byShape.add(entry.getKey() + " = " + entry.getValue());
            }
        }

        if (byShape.size() == 1) {

            String only = byShape.get(0);
            System.out.println(">>> Matched a Delivery Solutions id by shape: " + only);

            return Optional.of(only.substring(only.indexOf(" = ") + 3));
        }

        if (byShape.size() > 1) {

            System.out.println(">>> Several fields look like a Delivery Solutions id;"
                    + " not guessing between them:");
            byShape.forEach(candidate -> System.out.println("      " + candidate));
        }

        return Optional.empty();
    }

    /**
     * Every field in the document, so a run that cannot find the id
     * still says what it did get back — which is how the field gets
     * identified without another round trip.
     */
    private static void describe(Map<String, Object> document) {

        Map<String, String> flat = new LinkedHashMap<>();
        flatten(document, "", flat);

        flat.forEach((path, value) ->
                System.out.println("      " + path + " = " + shorten(value)));
    }

    @SuppressWarnings("unchecked")
    private static void flatten(Object node, String path, Map<String, String> into) {

        if (node instanceof Map<?, ?> map) {

            map.forEach((key, value) ->
                    flatten(value, path.isEmpty() ? String.valueOf(key) : path + "." + key, into));

        } else if (node instanceof List<?> list) {

            for (int index = 0; index < list.size(); index++) {

                flatten(list.get(index), path + "[" + index + "]", into);
            }

        } else if (node != null) {

            into.put(path, String.valueOf(node));
        }
    }

    private static String leaf(String path) {

        int dot = path.lastIndexOf('.');

        return dot < 0 ? path : path.substring(dot + 1);
    }

    private static String secret() {

        String fromEnvironment = System.getenv("DS_INTERNAL_TOKEN");

        return fromEnvironment != null && !fromEnvironment.isBlank()
                ? fromEnvironment.trim()
                : ConfigReader.get(TOKEN);
    }

    private static String shorten(String value) {

        String collapsed = value == null ? "" : value.strip().replaceAll("\\s+", " ");

        return collapsed.length() <= 200 ? collapsed : collapsed.substring(0, 200) + "…";
    }

}
