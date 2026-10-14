package com.instamall.support;

import org.testng.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Properties;

/**
 * The one piece of state a later phase of the order lifecycle needs from
 * an earlier one: the id of the order the Customer app placed.
 *
 * When every phase runs in one go, that id is simply handed along in
 * memory. When a phase is run on its own — a re-run of the Shopper
 * acceptance against an order placed an hour ago, a dispatch on the
 * Portal without placing anything new — it has to come from somewhere
 * else, and this is where. Resolved in this order:
 *
 *   1. what the current run already placed, if anything;
 *   2. the {@code orderId} system property — {@code -DorderId=IM-AE078}
 *      on the Maven command line, or a VM option in the IDE's run
 *      configuration;
 *   3. the id the most recent Phase 1 recorded on disk, in
 *      {@value #RECORD}, which Phase 1 writes the moment the order is
 *      placed.
 *
 * A phase that finds none of these fails up front, naming all three,
 * rather than opening an app and hunting for an order that was never
 * identified.
 */
public final class PlacedOrder {

    public static final String PROPERTY = "orderId";

    public static final String RECORD = "test-output/last-placed-order.properties";

    private PlacedOrder() {

    }

    /**
     * Records the order Phase 1 just placed, for any phase run later on
     * its own.
     */
    public static void record(String orderId) {

        Properties record = new Properties();

        record.setProperty(PROPERTY, orderId);
        record.setProperty("placedAt", ZonedDateTime.now().toString());

        try {

            Path path = Paths.get(RECORD);
            Files.createDirectories(path.getParent());

            try (var out = Files.newBufferedWriter(path)) {

                record.store(out, "The order the most recent Phase 1 placed — read by any"
                        + " later phase run on its own");
            }

        } catch (IOException notWritten) {

            // Not fatal for the run in progress, which still has the id
            // in memory; only a later standalone phase would miss it.
            System.out.println(">>> Could not record the placed order id to "
                    + RECORD + " (" + notWritten.getMessage() + ")");
        }
    }

    /**
     * @param fromThisRun the id an earlier phase of this same run placed,
     *                    or null when none did
     * @return the order id the phase should work on, or null when none
     *         of the three sources has one — the phase goes ahead
     *         regardless and fails at the point it actually needs the
     *         id, not before it has opened anything (see
     *         {@link #require(String, String)})
     */
    public static String resolve(String fromThisRun) {

        if (fromThisRun != null && !fromThisRun.isBlank()) {

            return fromThisRun;
        }

        String fromProperty = System.getProperty(PROPERTY);

        if (fromProperty != null && !fromProperty.isBlank()) {

            System.out.println(">>> Order id taken from -D" + PROPERTY + ": " + fromProperty);

            return fromProperty.trim();
        }

        Optional<String> fromRecord = recorded();

        if (fromRecord.isPresent()) {

            System.out.println(">>> Order id taken from " + RECORD + ": " + fromRecord.get());

            return fromRecord.get();
        }

        System.out.println(">>> No order id known yet: " + NONE_KNOWN
                + " — carrying on; this phase will fail where it needs one, or adopt the"
                + " order it finds where it can.");

        return null;
    }

    /**
     * The id, or a failure right here naming the three places it could
     * have come from — for the step of a phase that cannot go on without
     * one (finding the order on the Portal, say).
     */
    public static String require(String orderId, String neededFor) {

        Assert.assertNotNull(orderId,
                neededFor + " needs the order id, and none is known: " + NONE_KNOWN);

        return orderId;
    }

    /**
     * How to read an id that may still be unknown in a log line.
     */
    public static String describe(String orderId) {

        return orderId == null ? "(order id not yet known)" : orderId;
    }

    private static final String NONE_KNOWN =
            "nothing was placed in this run, no -D" + PROPERTY + " was given, and "
                    + RECORD + " holds no earlier order";

    /**
     * The id the most recent Phase 1 recorded, if the file is there.
     */
    public static Optional<String> recorded() {

        Path path = Paths.get(RECORD);

        if (!Files.isRegularFile(path)) {

            return Optional.empty();
        }

        Properties record = new Properties();

        try (var in = Files.newBufferedReader(path)) {

            record.load(in);

        } catch (IOException unreadable) {

            return Optional.empty();
        }

        String orderId = record.getProperty(PROPERTY);

        return orderId == null || orderId.isBlank()
                ? Optional.empty()
                : Optional.of(orderId.trim());
    }

}
