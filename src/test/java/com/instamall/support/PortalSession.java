package com.instamall.support;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.codec.w3c.W3CHttpCommandCodec;
import org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec;
import org.openqa.selenium.remote.http.HttpClient;
import org.openqa.selenium.remote.http.HttpMethod;
import org.openqa.selenium.remote.http.HttpRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * The Delivery Portal browser Phase 3a logs into, kept open until Phase
 * 3b marks the order Delivered — across separate runs, not just one.
 *
 * Chromedriver is its own process and outlives the JVM, so 3a records
 * its address and session id in {@value #RECORD} and exits without
 * quitting. 3b re-attaches to that same session (same window, same
 * order page), and closes it only once Delivered is confirmed.
 */
public final class PortalSession {

    public static final String RECORD = "test-output/portal-session.properties";

    private PortalSession() {

    }

    /** Remembers the browser Phase 3a opened so a later run can re-attach. */
    public static void record(WebDriver driver) {

        if (!(driver instanceof RemoteWebDriver remote)
                || !(remote.getCommandExecutor() instanceof HttpCommandExecutor http)) {

            return;
        }

        Properties record = new Properties();
        record.setProperty("serverUrl", http.getAddressOfRemoteServer().toString());
        record.setProperty("sessionId", remote.getSessionId().toString());

        try {

            Path path = Paths.get(RECORD);
            Files.createDirectories(path.getParent());

            try (var out = Files.newBufferedWriter(path)) {

                record.store(out, "Portal browser Phase 3a left open; Phase 3b closes it once Delivered");
            }

            System.out.println(">>> Portal session recorded in " + RECORD);

        } catch (IOException notWritten) {

            System.out.println(">>> WARNING: could not write " + RECORD + " (" + notWritten.getMessage() + ")");
        }
    }

    /** The recorded browser, re-attached and answering — or empty. */
    public static Optional<WebDriver> reattach() {

        Properties record = new Properties();

        try (var in = Files.newBufferedReader(Paths.get(RECORD))) {

            record.load(in);

        } catch (IOException none) {

            return Optional.empty();
        }

        String serverUrl = record.getProperty("serverUrl");
        String sessionId = record.getProperty("sessionId");

        try {

            AttachedDriver driver = new AttachedDriver(URI.create(serverUrl).toURL(), new SessionId(sessionId));

            System.out.println(">>> Re-attached to Portal session " + sessionId + " on " + driver.getCurrentUrl());

            return Optional.of(driver);

        } catch (Exception gone) {

            System.out.println(">>> Recorded Portal session " + sessionId + " at " + serverUrl
                    + " no longer answers — " + gone.getClass().getSimpleName());

            clear();

            return Optional.empty();
        }
    }

    /** Quits the browser, stops a re-attached chromedriver, forgets the record. */
    public static void close(WebDriver driver) {

        if (driver != null) {

            driver.quit();
        }

        if (driver instanceof AttachedDriver attached) {

            attached.shutdownChromedriver();
        }

        clear();
    }

    private static void clear() {

        try {

            Files.deleteIfExists(Paths.get(RECORD));

        } catch (IOException ignored) {

            // A stale record is re-checked on the next run anyway.
        }
    }

    /**
     * A RemoteWebDriver on a session that already exists. Selenium has no
     * public "attach": its constructor always sends New Session, so the
     * executor answers that one command locally with the known session
     * id and sends everything else over HTTP with the W3C codecs.
     */
    private static final class AttachedDriver extends RemoteWebDriver {

        private final HttpClient client;

        private AttachedDriver(URL serverUrl, SessionId sessionId) {

            this(HttpClient.Factory.createDefault().createClient(serverUrl), sessionId);
        }

        private AttachedDriver(HttpClient client, SessionId sessionId) {

            super(new AttachedExecutor(client, sessionId), new ChromeOptions());
            this.client = client;
        }

        /** The JVM that started this chromedriver is gone; stop it directly. */
        private void shutdownChromedriver() {

            try {

                client.execute(new HttpRequest(HttpMethod.GET, "/shutdown"));

            } catch (Exception ignored) {

                // Already down.
            }
        }
    }

    private static final class AttachedExecutor implements CommandExecutor {

        private final HttpClient client;
        private final SessionId sessionId;
        private final W3CHttpCommandCodec commandCodec = new W3CHttpCommandCodec();
        private final W3CHttpResponseCodec responseCodec = new W3CHttpResponseCodec();

        private AttachedExecutor(HttpClient client, SessionId sessionId) {

            this.client = client;
            this.sessionId = sessionId;
        }

        @Override
        public Response execute(Command command) throws IOException {

            if (DriverCommand.NEW_SESSION.equals(command.getName())) {

                Response attached = new Response(sessionId);
                attached.setState("success");
                attached.setValue(Map.of("browserName", "chrome"));

                return attached;
            }

            Command bound = new Command(sessionId, command.getName(), command.getParameters());
            Response response = responseCodec.decode(client.execute(commandCodec.encode(bound)));

            if (response.getSessionId() == null) {

                response.setSessionId(sessionId.toString());
            }

            return response;
        }
    }

}
