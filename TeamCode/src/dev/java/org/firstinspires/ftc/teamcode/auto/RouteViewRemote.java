package org.firstinspires.ftc.teamcode.auto;

import android.content.Context;
import android.os.SystemClock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.qualcomm.ftccommon.FtcEventLoop;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier;
import com.qualcomm.robotcore.util.RobotLog;
import com.qualcomm.robotcore.util.WebHandlerManager;

import org.firstinspires.ftc.ftccommon.external.OnCreateEventLoop;
import org.firstinspires.ftc.ftccommon.external.WebHandlerRegistrar;
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMeta;
import org.firstinspires.ftc.robotcore.internal.opmode.RegisteredOpModes;

import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Dev builds only (R704): lets tools/routeview pick, INIT, START and STOP autonomous OpModes
 * through the robot's own web server (port 8080; Jimothy reaches it with adb forward).
 *
 * <pre>
 * GET  /routeview/status        protocol, active OpMode, its phase, the autonomous OpModes; also the heartbeat
 * POST /routeview/init?name=X   INIT an OpMode (only when none is active)
 * POST /routeview/start         START the OpMode in INIT
 * POST /routeview/stop          stop the active OpMode
 * </pre>
 *
 * Safety ({@link RemoteRunGuard}): an OpMode started from here is stopped if Jimothy goes quiet
 * for 1.5 s (laptop asleep, Wi-Fi or adb dropped, tool closed) and after 30 s, like the Driver
 * Station's AUTO timer. The Driver Station can stop it as always. POSTs need the X-RouteView
 * header, which a page on another site can't send, so browsing the web from the robot's network
 * can't drive the robot.
 */
public final class RouteViewRemote {

    private static final String TAG = "RouteViewRemote";
    private static final String PATH = "/routeview/";
    private static final String HEADER = "x-routeview"; // NanoHTTPD lower-cases header names
    private static final long WATCH_MS = 100;
    /**
     * In every status, so Jimothy can tell an out-of-date dev build on the robot from a bug. Bump
     * it with RobotLink.PROTOCOL (RobotLinkTest checks they match) when this file's behaviour
     * changes. 1: builds before it was added.
     */
    static final int PROTOCOL = 2;

    private static volatile FtcEventLoop eventLoop;
    private static volatile OpModeManagerImpl manager;
    /**
     * Held here on purpose: the SDK keeps OpMode listeners in a WeakReferenceSet, so a listener
     * nothing else references is garbage collected and silently stops hearing INIT/START/stop.
     */
    private static OpModeManagerNotifier.Notifications listener;
    /** Guards itself and {@link #details}. */
    private static final RemoteRunGuard guard = new RemoteRunGuard();
    private static final Map<String, JsonObject> details = new HashMap<>();
    private static boolean watching;

    private RouteViewRemote() {}

    @OnCreateEventLoop
    public static void attachEventLoop(Context context, FtcEventLoop eventLoop) {
        OpModeManagerImpl m = eventLoop.getOpModeManager();
        RouteViewRemote.eventLoop = eventLoop;
        manager = m;
        listener = new OpModeManagerNotifier.Notifications() {
            @Override public void onOpModePreInit(OpMode opMode) { phase(m, "INIT"); }
            @Override public void onOpModePreStart(OpMode opMode) { phase(m, "RUNNING"); }
            @Override public void onOpModePostStop(OpMode opMode) { phase(m, "IDLE"); }
        };
        m.registerListener(listener);
    }

    @WebHandlerRegistrar
    public static void attachWebServer(Context context, WebHandlerManager web) {
        web.register(PATH + "status", session -> json(NanoHTTPD.Response.Status.OK, status()));
        web.register(PATH + "init", session -> control(session, () -> init(session.getParms().get("name"))));
        web.register(PATH + "start", session -> control(session, RouteViewRemote::start));
        web.register(PATH + "stop", session -> control(session, RouteViewRemote::stop));
        synchronized (guard) {
            if (!watching) {
                watching = true;
                Thread watchdog = new Thread(RouteViewRemote::watch, TAG);
                watchdog.setDaemon(true);
                watchdog.start();
            }
        }
    }

    /** The OpMode the SDK itself has active, or null for its idle placeholder. */
    private static String sdkActive() {
        OpModeManagerImpl m = manager;
        String name = m == null ? null : m.getActiveOpModeName();
        return name == null || name.equals(OpModeManagerImpl.DEFAULT_OP_MODE_NAME) ? null : name;
    }

    /** Event loop thread: an OpMode entered INIT or RUNNING, or stopped. */
    private static void phase(OpModeManagerImpl m, String next) {
        String name = m.getActiveOpModeName();
        boolean idle = name == null || name.equals(OpModeManagerImpl.DEFAULT_OP_MODE_NAME);
        synchronized (guard) {
            guard.phase(idle ? null : name, next, SystemClock.uptimeMillis());
            if (guard.lastEnded != null && next.equals("IDLE")) {
                RobotLog.ii(TAG, "%s", guard.lastEnded);
            }
        }
        if (!idle) {
            RobotLog.ii(TAG, "%s: %s", name, next);
        }
    }

    // ---- requests (web server threads) ---------------------------------------------------------

    private interface Action {
        /** Null when done, else why not. */
        String run();
    }

    private static NanoHTTPD.Response control(NanoHTTPD.IHTTPSession session, Action action) {
        if (session.getMethod() != NanoHTTPD.Method.POST) {
            return error(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, "use POST");
        }
        if (!session.getHeaders().containsKey(HEADER)) {
            return error(NanoHTTPD.Response.Status.FORBIDDEN, "missing X-RouteView header");
        }
        if (manager == null) {
            return error(NanoHTTPD.Response.Status.SERVICE_UNAVAILABLE, "the robot isn't running yet");
        }
        String refused = action.run();
        RobotLog.ii(TAG, "%s %s", session.getUri(), refused == null ? "accepted" : "refused: " + refused);
        return refused == null ? json(NanoHTTPD.Response.Status.OK, status())
                : error(NanoHTTPD.Response.Status.CONFLICT, refused);
    }

    private static String init(String name) {
        if (name == null || RegisteredOpModes.getInstance().getOpModeMetadata(name) == null) {
            return "no OpMode named \"" + name + "\" on the robot";
        }
        String running = sdkActive();
        if (running != null) {
            return running + " is active on the robot; stop it first";
        }
        String refused;
        synchronized (guard) {
            refused = guard.init();
        }
        if (refused == null) {
            manager.initOpMode(name);
        }
        return refused;
    }

    private static String start() {
        String refused;
        synchronized (guard) {
            refused = guard.start(SystemClock.uptimeMillis());
        }
        if (refused == null) {
            manager.startActiveOpMode();
        }
        return refused;
    }

    private static String stop() {
        // STOP mustn't depend on the listener: it stops whatever the SDK says is active.
        boolean sdk = sdkActive() != null;
        boolean any;
        synchronized (guard) {
            any = guard.stop();
            if (!any && sdk) {
                guard.lastStop = "stopped from Jimothy";
            }
        }
        if (sdk) {
            requestStop();
        }
        return null;
    }

    /**
     * Has the event loop stop the active OpMode on its own thread, as FTC Dashboard's STOP does.
     * Not {@code stopActiveOpMode()}: called from here, it would run the OpMode's stop() and
     * fail-safe the hubs on this thread while the event loop may be inside the same OpMode's loop().
     * The event loop ignores the request if that OpMode is no longer active.
     */
    private static void requestStop() {
        FtcEventLoop loop = eventLoop;
        OpModeManagerImpl m = manager;
        if (loop != null && m != null) {
            loop.requestOpModeStop(m.getActiveOpMode());
        }
    }

    /** Jimothy polls this every 0.5 s while connected; each poll is a heartbeat. */
    private static JsonObject status() {
        JsonObject out = new JsonObject();
        synchronized (guard) {
            long now = SystemClock.uptimeMillis();
            guard.heard(now);
            out.addProperty("protocol", PROTOCOL);
            out.addProperty("active", guard.active);
            out.addProperty("phase", guard.phase);
            out.addProperty("ms", guard.active == null ? 0 : now - guard.phaseSince);
            out.addProperty("remote", guard.startedHere);
            out.addProperty("limitMs", RemoteRunGuard.AUTO_LIMIT_MS);
            out.addProperty("lastStop", guard.lastStop);
            out.addProperty("lastEnded", guard.lastEnded);
        }
        // What the Driver Station shows in red, e.g. an exception thrown by an OpMode's init().
        out.addProperty("error", RobotLog.hasGlobalErrorMsg() ? RobotLog.getGlobalErrorMsg() : null);
        // The SDK's own view, in case it and the listener ever disagree; the page enables STOP on it.
        out.addProperty("sdkActive", sdkActive());
        JsonArray list = new JsonArray();
        for (OpModeMeta meta : RegisteredOpModes.getInstance().getOpModes()) {
            if (meta.flavor == OpModeMeta.Flavor.AUTONOMOUS) {
                list.add(describe(meta));
            }
        }
        out.add("opModes", list);
        return out;
    }

    /** Name and group, plus the route file and alliance for a JSON route OpMode. */
    private static JsonObject describe(OpModeMeta meta) {
        synchronized (guard) {
            JsonObject known = details.get(meta.name);
            if (known != null) {
                return known;
            }
        }
        JsonObject d = new JsonObject();
        d.addProperty("name", meta.name);
        d.addProperty("group", meta.group);
        try {
            // A fresh, never-INITed instance: routeName() and alliance() only return constants.
            OpMode instance = RegisteredOpModes.getInstance().getOpMode(meta.name);
            if (instance instanceof JsonAutoOpMode) {
                d.addProperty("route", ((JsonAutoOpMode) instance).routeName());
                d.addProperty("alliance", ((JsonAutoOpMode) instance).alliance().name());
            }
        } catch (RuntimeException e) {
            RobotLog.ww(TAG, "can't inspect OpMode %s: %s", meta.name, e);
        }
        synchronized (guard) {
            details.put(meta.name, d);
        }
        return d;
    }

    // ---- the watchdog ----------------------------------------------------------------------------

    private static void watch() {
        while (true) {
            String why;
            synchronized (guard) {
                why = guard.check(SystemClock.uptimeMillis());
            }
            if (why != null) {
                RobotLog.ww(TAG, "%s", why);
                requestStop();
            }
            SystemClock.sleep(WATCH_MS); // its own thread, never the OpMode loop
        }
    }

    // ---- responses -------------------------------------------------------------------------------

    private static NanoHTTPD.Response json(NanoHTTPD.Response.IStatus status, JsonObject body) {
        NanoHTTPD.Response r = NanoHTTPD.newFixedLengthResponse(status, "application/json", body.toString());
        r.addHeader("Cache-Control", "no-store");
        return r;
    }

    private static NanoHTTPD.Response error(NanoHTTPD.Response.IStatus status, String message) {
        JsonObject body = new JsonObject();
        body.addProperty("error", message);
        return json(status, body);
    }
}
