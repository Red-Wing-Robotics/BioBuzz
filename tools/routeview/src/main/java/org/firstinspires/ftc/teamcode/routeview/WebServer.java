package org.firstinspires.ftc.teamcode.routeview;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * The page and its JSON API, on 127.0.0.1 only. Anyone who can reach this server can push files to
 * the robot, so it never listens on the robot's Wi-Fi, and it refuses requests whose Host isn't
 * this machine (DNS rebinding) and POSTs from other sites' pages.
 *
 * <pre>
 * GET  /                       the page (resources/routeview/index.html)
 * GET  /jimothy.svg            the icon (resources/routeview/jimothy.svg)
 * GET  /api/state?since=N      every route's status; waits up to 25 s for a version newer than N
 * GET  /api/route?name=X       one route's check and both alliances' maps
 * GET  /api/source?name=X      the file's text and checksum, for the editor
 * POST /api/check              check and draw the body (unsaved editor text), as a save would
 * POST /api/save?name=X&base=C write the body to the route file if it's valid and the file is
 *                              still at checksum C (409 otherwise; &force=true overwrites)
 * POST /api/push?name=X        push now (only if valid)
 * POST /api/push-all           push every valid route that differs from the hub
 * POST /api/remove?name=X      delete the hub's copy, so the robot runs the APK's
 * POST /api/refresh            re-read the hub's copies
 * POST /api/connect            adb connect now (and keep reconnecting every 2 s)
 * POST /api/robot/init?name=X  INIT an OpMode on the robot (dev build: RouteViewRemote)
 * POST /api/robot/start        START it
 * POST /api/robot/stop         stop the robot's active OpMode
 * POST /api/disconnect         adb disconnect, and stop reconnecting until /api/connect
 * POST /api/autopush?on=B      push on save, or not
 * </pre>
 */
final class WebServer {

    private static final long LONG_POLL_MILLIS = 25_000;
    private static final int MAX_BODY_BYTES = 512 * 1024;
    private static final Gson GSON = new Gson();

    private final RouteView view;
    private final int port;

    private WebServer(RouteView view, int port) {
        this.view = view;
        this.port = port;
    }

    static void start(RouteView view, int port) throws IOException {
        WebServer web = new WebServer(view, port);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", web::handle);
        // Long polls hold a thread each, so leave room for a few open tabs.
        server.setExecutor(Executors.newFixedThreadPool(16));
        server.start();
    }

    private void handle(HttpExchange x) throws IOException {
        try {
            String method = x.getRequestMethod();
            if (!allowed(method, x.getRequestHeaders().getFirst("Host"), x.getRequestHeaders().getFirst("Origin"), port)) {
                send(x, 403, "text/plain", "Jimothy only answers http://127.0.0.1:" + port + "/");
                return;
            }
            String path = x.getRequestURI().getPath();
            Map<String, String> q = query(x.getRequestURI().getRawQuery());
            boolean post = method.equals("POST");

            if (path.equals("/") && !post) {
                page(x);
            } else if (path.equals("/jimothy.svg") && !post) {
                icon(x);
            } else if (path.equals("/api/state") && !post) {
                long since = q.containsKey("since") ? Long.parseLong(q.get("since")) : 0;
                json(x, 200, view.awaitState(since, LONG_POLL_MILLIS));
            } else if (path.equals("/api/route") && !post) {
                JsonObject detail = view.detail(q.getOrDefault("name", ""));
                if (detail == null) {
                    send(x, 404, "text/plain", "no such route");
                } else {
                    json(x, 200, detail);
                }
            } else if (path.equals("/api/source") && !post) {
                reply(x, view.source(q.get("name")));
            } else if (post && path.equals("/api/check")) {
                String text = body(x);
                if (text != null) {
                    json(x, 200, view.checkDraft(text));
                }
            } else if (post && path.equals("/api/save") && q.containsKey("name")) {
                String text = body(x);
                if (text != null) {
                    reply(x, view.save(q.get("name"), text, q.getOrDefault("base", ""), "true".equals(q.get("force"))));
                }
            } else if (post && path.equals("/api/push") && q.containsKey("name")) {
                view.requestPush(q.get("name"));
                accepted(x);
            } else if (post && path.equals("/api/push-all")) {
                view.requestPushAll();
                accepted(x);
            } else if (post && path.equals("/api/remove") && q.containsKey("name")) {
                view.requestRemove(q.get("name"));
                accepted(x);
            } else if (post && path.equals("/api/robot/init") && q.containsKey("name")) {
                robot(x, view.robotCommand("init", q.get("name")));
            } else if (post && path.equals("/api/robot/start")) {
                robot(x, view.robotCommand("start", null));
            } else if (post && path.equals("/api/robot/stop")) {
                robot(x, view.robotCommand("stop", null));
            } else if (post && path.equals("/api/connect")) {
                view.requestConnect();
                accepted(x);
            } else if (post && path.equals("/api/disconnect")) {
                view.requestDisconnect();
                accepted(x);
            } else if (post && path.equals("/api/refresh")) {
                view.requestRefresh();
                accepted(x);
            } else if (post && path.equals("/api/autopush") && q.containsKey("on")) {
                view.setAutoPush(Boolean.parseBoolean(q.get("on")));
                accepted(x);
            } else {
                send(x, 404, "text/plain", "not found");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException e) {
            send(x, 500, "text/plain", e.toString());
        } finally {
            x.close();
        }
    }

    /**
     * Only requests addressed to this machine, and no state changes from another site's page
     * (a browser sends Origin on a cross-site POST).
     */
    static boolean allowed(String method, String host, String origin, int port) {
        String h = host == null ? "" : host.toLowerCase(Locale.ROOT);
        if (!h.equals("127.0.0.1:" + port) && !h.equals("localhost:" + port)) {
            return false;
        }
        if (method.equals("GET") || origin == null) {
            return true;
        }
        return origin.equals("http://127.0.0.1:" + port) || origin.equals("http://localhost:" + port);
    }

    static Map<String, String> query(String raw) {
        Map<String, String> q = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return q;
        }
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            q.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return q;
    }

    private void page(HttpExchange x) throws IOException {
        try (InputStream in = WebServer.class.getResourceAsStream("/routeview/index.html")) {
            if (in == null) {
                send(x, 500, "text/plain", "index.html missing from the build");
                return;
            }
            send(x, 200, "text/html; charset=utf-8", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /** The request body as text, or null after answering 413 if it's too big for a route file. */
    private static String body(HttpExchange x) throws IOException {
        byte[] bytes = x.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) {
            send(x, 413, "text/plain", "too large for a route file");
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** The robot's answer: 200, 409 if it refused, 502 if it couldn't be reached. */
    private static void robot(HttpExchange x, RobotLink.Answer answer) throws IOException {
        JsonObject body = new JsonObject();
        if (!answer.ok()) {
            body.addProperty("error", answer.problem());
        }
        json(x, answer.ok() ? 200 : answer.status == 0 ? 502 : 409, body);
    }

    private static void reply(HttpExchange x, RouteView.Reply reply) throws IOException {
        json(x, reply.status, reply.body);
    }

    private void icon(HttpExchange x) throws IOException {
        try (InputStream in = WebServer.class.getResourceAsStream("/routeview/jimothy.svg")) {
            if (in == null) {
                send(x, 404, "text/plain", "icon missing from the build");
                return;
            }
            send(x, 200, "image/svg+xml", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static void json(HttpExchange x, int status, JsonObject body) throws IOException {
        send(x, status, "application/json; charset=utf-8", GSON.toJson(body));
    }

    private static void accepted(HttpExchange x) throws IOException {
        send(x, 202, "application/json; charset=utf-8", "{}");
    }

    private static void send(HttpExchange x, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", type);
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = x.getResponseBody()) {
            out.write(bytes);
        }
    }
}
