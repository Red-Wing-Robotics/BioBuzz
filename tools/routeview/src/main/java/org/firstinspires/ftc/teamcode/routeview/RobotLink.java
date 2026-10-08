package org.firstinspires.ftc.teamcode.routeview;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Talks to RouteViewRemote (TeamCode/src/dev/java/.../auto) in the robot app, through the local
 * port adb forwards to the robot's web server. Every call is quick or fails within 2 s.
 */
final class RobotLink {

    /** One answer from the robot: HTTP status and JSON body, or status 0 and why it didn't answer. */
    static final class Answer {
        final int status;
        final JsonObject body;
        final String failure;

        Answer(int status, JsonObject body, String failure) {
            this.status = status;
            this.body = body;
            this.failure = failure;
        }

        boolean ok() {
            return status == 200;
        }

        /** The robot's reason for refusing, or why it couldn't be reached. */
        String problem() {
            if (failure != null) {
                return failure;
            }
            if (status == 404) {
                return "the robot app doesn't include Jimothy's remote control. Install the dev build (devDebug); competition builds leave it out";
            }
            JsonElement e = body == null ? null : body.get("error");
            return e != null && !e.isJsonNull() ? e.getAsString() : "the robot answered HTTP " + status;
        }
    }

    /** RouteViewRemote.PROTOCOL of the robot code this Jimothy works with. Bump both together. */
    static final int PROTOCOL = 2;

    /** Null if a status comes from robot code this Jimothy works with, else what to do about it. */
    static String protocolProblem(JsonObject status) {
        if (status == null) {
            return null;
        }
        JsonElement p = status.get("protocol");
        int robot = p != null && !p.isJsonNull() ? p.getAsInt() : 1; // builds from before the field
        if (robot == PROTOCOL) {
            return null;
        }
        return robot < PROTOCOL
                ? "the robot's dev build is older than this Jimothy (remote protocol " + robot + ", Jimothy needs "
                        + PROTOCOL + "). Install the current dev build (devDebug) on the robot"
                : "the robot's dev build is newer than this Jimothy (remote protocol " + robot + ", Jimothy has "
                        + PROTOCOL + "). Update and restart Jimothy";
    }

    final int port;
    private final String base;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    RobotLink(int port) {
        this.port = port;
        this.base = "http://127.0.0.1:" + port + "/routeview/";
    }

    Answer status() {
        return send(HttpRequest.newBuilder(URI.create(base + "status")).GET());
    }

    Answer init(String opMode) {
        // %20, not URLEncoder's +, so no server can read a space as a literal plus.
        return post("init?name=" + URLEncoder.encode(opMode, StandardCharsets.UTF_8).replace("+", "%20"));
    }

    Answer start() {
        return post("start");
    }

    Answer stop() {
        return post("stop");
    }

    private Answer post(String path) {
        return send(HttpRequest.newBuilder(URI.create(base + path))
                .header("X-RouteView", "1") // RouteViewRemote refuses POSTs without it
                .POST(HttpRequest.BodyPublishers.noBody()));
    }

    private Answer send(HttpRequest.Builder request) {
        try {
            HttpResponse<String> r = http.send(request.timeout(Duration.ofSeconds(2)).build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonObject body = null;
            try {
                JsonElement parsed = new JsonParser().parse(r.body()); // Gson 2.8.0 API, as on the robot
                body = parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
            } catch (RuntimeException notJson) {
                // e.g. the SDK's own 404 page
            }
            return new Answer(r.statusCode(), body, null);
        } catch (IOException e) {
            return new Answer(0, null, "the robot app isn't answering (" + e.getClass().getSimpleName()
                    + (e.getMessage() != null ? ": " + e.getMessage() : "") + "). Is it running?");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Answer(0, null, "interrupted");
        }
    }
}
