package org.firstinspires.ftc.teamcode.routeview;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.firstinspires.ftc.teamcode.auto.route.RouteChecksum;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Jimothy (the tool in tools/routeview; code names say RouteView): watches the route files, checks and draws each one as the robot and docs/autos.pdf
 * would, pushes every saved route to the Control Hub over adb, and serves it all on
 * http://127.0.0.1:8765. See readme.md.
 *
 * Threads: the watcher polls the files every 200 ms; one hub thread makes every adb call (pushes
 * queue there in order) except {@code adb connect}, which can take seconds and runs on its own; the web server's threads read state, and the page's editor writes route
 * files, which the watcher then picks up like any other save. State is guarded by {@code this};
 * each change bumps {@link #version}, which wakes the page's long poll.
 */
public final class RouteView {

    static final String AUTOS = "TeamCode/src/main/assets/autos";
    static final String SOURCES = "TeamCode/src/main/java";

    private static final long POLL_MS = 200;
    private static final long HUB_POLL_MS = 2000;
    /** Also the heartbeat that keeps an OpMode started from here running (RouteViewRemote). */
    private static final long ROBOT_POLL_MS = 500;
    private static final int POLLS_PER_SOURCE_SCAN = 5;
    private static final String PUSHING = "saved · pushing…";
    /** Route names the editor may write: a plain file name, no paths. */
    private static final Pattern ROUTE_NAME = Pattern.compile("[A-Za-z0-9_\\-][A-Za-z0-9_.\\-]*");

    /** One route file as last read from disk, plus what happened to it. */
    static final class Entry {
        final String name;
        boolean present;
        long modified = -1;
        long length = -1;
        byte[] bytes;
        String checksum;
        RouteCheck.Result check;
        /** Saved while the hub was unreachable (or a push failed): push when it's back. */
        boolean pending;
        String event;
        boolean eventOk;
        long eventAt;

        Entry(String name) {
            this.name = name;
        }
    }

    private final File autosDir;
    private final File sourceRoot;
    private final String serial;
    private final Hub hub;
    private final File pushDir;
    private final ScheduledExecutorService watcher = Executors.newSingleThreadScheduledExecutor(daemon("watcher"));
    private final ScheduledExecutorService hubThread = Executors.newSingleThreadScheduledExecutor(daemon("hub"));
    private final ExecutorService connector = Executors.newCachedThreadPool(daemon("connect"));
    private final ScheduledExecutorService robotThread = Executors.newSingleThreadScheduledExecutor(daemon("robot"));

    // Written by the watcher; the editor's checks read it.
    private volatile RouteCheck.Registrations registrations;
    // Watcher-thread only.
    private int polls;
    // Serializes the editor's saves.
    private final Object saveLock = new Object();

    // Guarded by this.
    private final Map<String, Entry> routes = new TreeMap<>();
    private Map<String, String> hubFiles;
    private boolean hubOnline;
    private String hubMessage;
    private boolean autoPush;
    /** False after Disconnect: no automatic reconnects until Connect. */
    private boolean connectWanted = true;
    /** A Connect click is being tried. */
    private boolean connecting;
    /** A background (every 2 s) adb connect is under way. */
    private boolean autoConnecting;
    /** RouteViewRemote in the robot app, while the hub is connected and adb forwards to it. */
    private RobotLink robot;
    private JsonObject robotStatus;
    private String robotProblem;
    private long version = 1;

    RouteView(File repo, String serial, boolean autoPush) throws IOException {
        this.autosDir = new File(repo, AUTOS);
        this.sourceRoot = new File(repo, SOURCES);
        this.registrations = RouteCheck.Registrations.scan(sourceRoot); // before the server answers checks
        this.serial = serial;
        this.autoPush = autoPush;
        Adb adb = Adb.find(repo);
        this.hub = adb == null ? null : new Hub(adb, serial);
        this.hubMessage = adb == null
                ? "adb not found: set sdk.dir in local.properties (Android Studio does) or ANDROID_HOME"
                : "looking for the hub…";
        this.pushDir = Files.createTempDirectory("routeview").toFile();
        this.pushDir.deleteOnExit();
    }

    // ---- startup ------------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        int port = 8765;
        String serial = "192.168.43.1:5555";
        boolean autoPush = true;
        boolean open = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port": port = Integer.parseInt(args[++i]); break;
                case "--hub": serial = args[++i]; break;
                case "--no-push": autoPush = false; break;
                case "--open": open = true; break;
                default:
                    System.err.println("usage: routeview [--port 8765] [--hub 192.168.43.1:5555 | USB serial] [--no-push] [--open]");
                    System.exit(2);
            }
        }

        File repo = new File("").getAbsoluteFile(); // the Gradle run task starts us in the repo root
        if (!new File(repo, AUTOS).isDirectory()) {
            System.err.println("Jimothy: no " + AUTOS + " under " + repo + ". Run it with ./gradlew -p tools/routeview run");
            System.exit(2);
        }

        RouteView view = new RouteView(repo, serial, autoPush);
        String url = "http://127.0.0.1:" + port + "/";
        try {
            WebServer.start(view, port);
        } catch (BindException e) {
            System.err.println("Jimothy: port " + port + " is in use (already running?). Use --args=\"--port 8766\".");
            System.exit(1);
        }
        view.start(url);
        if (open) {
            openBrowser(url);
        }
    }

    private void start(String url) {
        pollFiles(true);
        System.out.println();
        System.out.println("Jimothy  " + url);
        System.out.println("  routes  " + AUTOS);
        System.out.println("  hub     " + serial + (hub == null ? "  (" + hubMessage + ")" : "") + "   auto-push on save: " + (autoPush ? "on" : "off"));
        System.out.println();
        synchronized (this) {
            for (Entry e : routes.values()) {
                log(e.name + "  " + describe(e.check) + "  #" + e.checksum);
            }
        }
        watcher.scheduleWithFixedDelay(() -> pollFiles(false), POLL_MS, POLL_MS, TimeUnit.MILLISECONDS);
        if (hub != null) {
            hubThread.scheduleWithFixedDelay(this::checkHub, 0, HUB_POLL_MS, TimeUnit.MILLISECONDS);
            robotThread.scheduleWithFixedDelay(this::pollRobot, ROBOT_POLL_MS, ROBOT_POLL_MS, TimeUnit.MILLISECONDS);
            // Ctrl+C or the Gradle task stopped: otherwise each run leaves a forward on the hub.
            Runtime.getRuntime().addShutdownHook(new Thread(this::removeForward, "unforward"));
        }
    }

    // ---- the route files (watcher thread) -------------------------------------------------------

    private void pollFiles(boolean startup) {
        try {
            if (polls++ % POLLS_PER_SOURCE_SCAN == 0) {
                RouteCheck.Registrations now = RouteCheck.Registrations.scan(sourceRoot);
                if (!now.equals(registrations)) {
                    registrations = now;
                    recheckAll();
                }
            }
            File[] files = autosDir.listFiles((dir, n) -> n.endsWith(".json"));
            Set<String> seen = new HashSet<>();
            for (File f : files == null ? new File[0] : files) {
                String name = f.getName().substring(0, f.getName().length() - ".json".length());
                seen.add(name);
                readIfChanged(name, f, startup);
            }
            synchronized (this) {
                for (Entry e : routes.values()) {
                    if (e.present && !seen.contains(e.name)) {
                        e.present = false;
                        e.bytes = null;
                        e.checksum = null;
                        e.check = null;
                        e.pending = false;
                        event(e, true, "deleted on the laptop; any copy on the hub stays until you remove it");
                        log(e.name + "  deleted on the laptop");
                        changed();
                    }
                }
            }
        } catch (RuntimeException e) {
            log("watcher: " + e); // keep the scheduled poll alive
        }
    }

    private void readIfChanged(String name, File f, boolean startup) {
        long modified = f.lastModified();
        long length = f.length();
        Entry known;
        synchronized (this) {
            known = routes.get(name);
            if (known != null && known.present && known.modified == modified && known.length == length) {
                return;
            }
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(f.toPath());
        } catch (IOException e) {
            return; // mid-save; the next poll gets it
        }
        String checksum = RouteChecksum.of(bytes);
        RouteCheck.Result check = null;
        if (known == null || !known.present || !checksum.equals(known.checksum)) {
            check = RouteCheck.check(new String(bytes, StandardCharsets.UTF_8), registrations);
        }
        synchronized (this) {
            Entry e = routes.computeIfAbsent(name, Entry::new);
            e.modified = modified;
            e.length = length;
            if (check == null) {
                return; // touched, not changed
            }
            e.present = true;
            e.bytes = bytes;
            e.checksum = checksum;
            e.check = check;
            if (!startup) {
                saved(e);
            }
            changed();
        }
    }

    /** Re-checks every route after TeamCode's registered action names changed (warnings only). */
    private void recheckAll() {
        Map<String, byte[]> current = new TreeMap<>();
        synchronized (this) {
            for (Entry e : routes.values()) {
                if (e.present) {
                    current.put(e.name, e.bytes);
                }
            }
        }
        for (Map.Entry<String, byte[]> c : current.entrySet()) {
            RouteCheck.Result check = RouteCheck.check(new String(c.getValue(), StandardCharsets.UTF_8), registrations);
            synchronized (this) {
                Entry e = routes.get(c.getKey());
                if (e != null && e.bytes == c.getValue()) {
                    e.check = check;
                    changed();
                }
            }
        }
    }

    /** A route changed on disk after startup. Holding the lock. */
    private void saved(Entry e) {
        if (!e.check.valid()) {
            event(e, false, "not pushed: " + e.check.error);
            alarm(e.name + "  saved · INVALID, not pushed\n          " + e.check.error);
            return;
        }
        for (String w : e.check.warnings) {
            log(e.name + "  warning: " + w);
        }
        if (!autoPush) {
            event(e, true, "saved · auto-push is off");
            log(e.name + "  saved · valid  #" + e.checksum + "  (auto-push off)");
        } else if (hub == null) {
            event(e, true, "saved · not pushed: " + hubMessage);
            log(e.name + "  saved · valid  #" + e.checksum + "  (no adb)");
        } else if (!hubOnline) {
            e.pending = true;
            event(e, true, "saved · will push when the hub connects");
            log(e.name + "  saved · valid  #" + e.checksum + "  (hub not connected: will push when it is)");
        } else {
            event(e, true, PUSHING);
            String name = e.name;
            hubThread.execute(() -> push(name, true));
        }
    }

    // ---- the hub (hub thread) -----------------------------------------------------------------

    /**
     * Every 2 s: notices the hub coming and going. While it's away (and Disconnect wasn't clicked),
     * starts an {@code adb connect} on the connector thread, since one can take seconds and pushes
     * shouldn't wait behind it.
     */
    private void checkHub() {
        try {
            synchronized (this) {
                if (!connectWanted) {
                    return;
                }
            }
            boolean online = hub.online();
            boolean was;
            synchronized (this) {
                was = hubOnline;
            }
            if (online && !was) {
                Map<String, String> files = null;
                String problem = null;
                try {
                    files = hub.overrides();
                } catch (Hub.Failure f) {
                    problem = f.getMessage();
                }
                RobotLink link = null;
                String linkProblem = null;
                try {
                    link = new RobotLink(hub.forwardWebServer());
                } catch (Hub.Failure f) {
                    linkProblem = f.getMessage();
                }
                List<String> pending = new ArrayList<>();
                synchronized (this) {
                    robot = link;
                    robotStatus = null;
                    robotProblem = linkProblem;
                    hubOnline = true;
                    hubFiles = files;
                    hubMessage = problem == null ? "connected" : "connected, but " + problem;
                    for (Entry e : routes.values()) {
                        if (e.present && e.pending) {
                            pending.add(e.name);
                        }
                    }
                    changed();
                }
                log("hub " + hub.serial + " connected · " + (files == null ? problem : files.size() + " route override(s) on it"));
                for (String name : pending) {
                    push(name, true);
                }
            } else if (!online && was) {
                synchronized (this) {
                    dropRobot();
                    hubOnline = false;
                    hubFiles = null;
                    hubMessage = "not connected";
                    changed();
                }
                alarm("hub " + hub.serial + " disconnected");
            }
            if (!online && hub.overNetwork()) {
                synchronized (this) {
                    if (connecting || autoConnecting) {
                        return; // one attempt at a time is enough
                    }
                    autoConnecting = true;
                }
                connector.execute(() -> connectNow(false));
            }
        } catch (RuntimeException e) {
            log("hub: " + e); // keep the scheduled check alive
        }
    }

    /** Connector thread: {@code adb connect}, then the hub thread looks again at once. */
    private void connectNow(boolean clicked) {
        String reply = hub.overNetwork() ? hub.connect() : null;
        hubThread.execute(() -> {
            boolean wanted;
            synchronized (this) {
                wanted = connectWanted;
            }
            if (!wanted && reply != null && reply.contains("connected to")) {
                hub.disconnect(); // Disconnect was clicked while this attempt was under way
            }
            checkHub();
            boolean online;
            synchronized (this) {
                online = hubOnline;
                if (clicked) {
                    connecting = false;
                } else {
                    autoConnecting = false;
                }
                if (!online && connectWanted) {
                    hubMessage = reply != null ? reply : "adb doesn't see " + hub.serial;
                }
                changed();
            }
            if (clicked && !online && wanted) {
                alarm("couldn't connect to the hub at " + hub.serial + ": " + (reply != null ? reply : "adb doesn't see it"));
            }
        });
    }

    /** Drops adb's Wi-Fi connection to the hub and stops reconnecting until Connect. */
    private void disconnect() {
        removeForward();
        String reply = hub.overNetwork() ? hub.disconnect() : null;
        synchronized (this) {
            dropRobot();
            hubOnline = false;
            hubFiles = null;
            hubMessage = "disconnected; click Connect to reconnect";
            changed();
        }
        log("hub " + hub.serial + " disconnected (Disconnect clicked)" + (reply != null ? ": " + reply : ""));
    }

    /** Takes down the adb forward to the robot's web server, if there is one. */
    private void removeForward() {
        RobotLink link;
        synchronized (this) {
            link = robot;
        }
        if (link != null) {
            hub.removeForward(link.port);
        }
    }

    /** Pushes the route's current contents if they're valid and not already on the hub. */
    private void push(String name, boolean automatic) {
        byte[] bytes;
        String checksum;
        synchronized (this) {
            Entry e = routes.get(name);
            if (e == null || !e.present) {
                return;
            }
            if (!e.check.valid()) {
                if (!automatic) {
                    event(e, false, "not pushed: fix the errors first");
                    changed();
                }
                return;
            }
            if (hub == null || !hubOnline) {
                e.pending = hub != null;
                event(e, hub != null, hub == null ? "not pushed: " + hubMessage : "will push when the hub connects");
                changed();
                return;
            }
            if (hubFiles != null && e.checksum.equals(hubFiles.get(name))) {
                e.pending = false;
                // A queued push whose save an earlier push already delivered keeps that push's message.
                if (!automatic || PUSHING.equals(e.event)) {
                    event(e, true, "already on the hub");
                    changed();
                }
                return;
            }
            bytes = e.bytes;
            checksum = e.checksum;
        }

        long start = System.nanoTime();
        String failure;
        try {
            File copy = new File(pushDir, name + ".json"); // exactly the bytes that were checked
            Files.write(copy.toPath(), bytes);
            copy.deleteOnExit();
            hub.push(copy, name);
            failure = null;
        } catch (IOException | Hub.Failure f) {
            failure = f.getMessage();
        }
        long ms = (System.nanoTime() - start) / 1_000_000;

        synchronized (this) {
            Entry e = routes.get(name);
            if (failure == null) {
                if (hubFiles != null) {
                    hubFiles.put(name, checksum);
                }
                if (e != null) {
                    e.pending = e.pending && !checksum.equals(e.checksum);
                    event(e, true, "pushed in " + ms + " ms");
                }
            } else if (e != null) {
                e.pending = true;
                event(e, false, "push failed: " + failure);
            }
            changed();
        }
        if (failure == null) {
            log(name + "  pushed in " + ms + " ms  #" + checksum + "  (the Driver Station shows this at INIT)");
        } else {
            alarm(name + "  push FAILED: " + failure);
            hubThread.execute(this::checkHub); // it may have just dropped
        }
    }

    private void remove(String name) {
        synchronized (this) {
            if (hub == null || !hubOnline) {
                return;
            }
        }
        String failure = null;
        try {
            hub.remove(name);
        } catch (Hub.Failure f) {
            failure = f.getMessage();
        }
        synchronized (this) {
            Entry e = routes.computeIfAbsent(name, Entry::new);
            if (failure == null) {
                if (hubFiles != null) {
                    hubFiles.remove(name);
                }
                e.pending = false;
                event(e, true, "removed from the hub: the robot runs the APK copy");
            } else {
                event(e, false, "remove failed: " + failure);
            }
            changed();
        }
        if (failure == null) {
            log(name + "  removed from the hub (the robot runs the APK copy)");
        } else {
            alarm(name + "  remove FAILED: " + failure);
        }
    }

    private void refreshHub() {
        synchronized (this) {
            if (hub == null || !hubOnline) {
                return;
            }
        }
        try {
            Map<String, String> files = hub.overrides();
            synchronized (this) {
                hubFiles = files;
                hubMessage = "connected";
                changed();
            }
            log("hub re-read · " + files.size() + " route override(s) on it");
        } catch (Hub.Failure f) {
            synchronized (this) {
                hubMessage = "connected, but " + f.getMessage();
                changed();
            }
        }
    }

    // ---- the robot app (robot thread) --------------------------------------------------------------

    /** Every 0.5 s while connected: RouteViewRemote's status, which is also its heartbeat. */
    private void pollRobot() {
        try {
            RobotLink link;
            synchronized (this) {
                link = robot;
            }
            if (link == null) {
                return;
            }
            RobotLink.Answer answer = link.status();
            synchronized (this) {
                if (robot != link) {
                    return; // the hub went away meanwhile
                }
                JsonObject body = answer.ok() ? answer.body : null;
                String problem = answer.ok() ? RobotLink.protocolProblem(body) : answer.problem();
                JsonObject status = problem == null ? body : null;
                String before = robotSummary(robotStatus, robotProblem);
                String after = robotSummary(status, problem);
                robotStatus = status;
                robotProblem = problem;
                if (!after.equals(before)) {
                    // Elapsed time alone doesn't wake the page; it counts locally between changes.
                    logRobot(status, problem);
                    changed();
                }
            }
        } catch (RuntimeException e) {
            log("robot: " + e); // keep the scheduled poll alive
        }
    }

    /** INIT, START or STOP on the robot, from a web server thread so STOP never queues behind adb. */
    RobotLink.Answer robotCommand(String command, String opMode) {
        RobotLink link;
        String problem;
        synchronized (this) {
            link = robot;
            problem = robotProblem != null ? robotProblem : "the hub isn't connected";
        }
        if (link == null) {
            return new RobotLink.Answer(0, null, problem);
        }
        RobotLink.Answer answer = command.equals("init") ? link.init(opMode)
                : command.equals("start") ? link.start() : link.stop();
        String what = "robot " + command.toUpperCase() + (opMode != null ? " " + opMode : "");
        if (answer.ok()) {
            log(what + ": accepted");
        } else {
            alarm(what + " refused: " + answer.problem());
        }
        robotThread.execute(this::pollRobot); // show the new state now, not at the next poll
        return answer;
    }

    /** What the page shows about the robot, minus the ticking time. Holding the lock. */
    private static String robotSummary(JsonObject status, String problem) {
        if (status == null) {
            return "problem: " + problem;
        }
        return text(status, "active") + "|" + text(status, "phase") + "|" + text(status, "sdkActive") + "|"
                + text(status, "remote") + "|"
                + text(status, "lastStop") + "|" + text(status, "lastEnded") + "|" + text(status, "error") + "|"
                + status.get("opModes");
    }

    private static void logRobot(JsonObject status, String problem) {
        if (status == null) {
            log("robot: " + problem);
            return;
        }
        String phase = text(status, "phase");
        String stop = text(status, "lastStop");
        String ended = text(status, "lastEnded");
        String sdk = text(status, "sdkActive");
        if (phase.equals("IDLE") && !sdk.isEmpty()) {
            log("robot: " + sdk + " is active, but the robot's phase tracking didn't see it start");
            return;
        }
        log("robot: " + (phase.equals("IDLE") ? "no OpMode active" + (stop.isEmpty() ? "" : " (" + stop + ")")
                + (ended.isEmpty() ? "" : " · " + ended)
                : phase + " " + text(status, "active")));
        if (!text(status, "error").isEmpty()) {
            alarm("robot error: " + text(status, "error"));
        }
    }

    private static String text(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    /** The hub went away. Holding the lock. */
    private void dropRobot() {
        robot = null;
        robotStatus = null;
        robotProblem = null;
    }

    // ---- requests from the page (web server threads) -------------------------------------------

    void requestPush(String name) {
        hubThread.execute(() -> push(name, false));
    }

    synchronized void requestPushAll() {
        for (Entry e : routes.values()) {
            if (e.present && e.check.valid()) {
                String name = e.name;
                hubThread.execute(() -> push(name, false));
            }
        }
    }

    void requestRemove(String name) {
        hubThread.execute(() -> remove(name));
    }

    synchronized void requestConnect() {
        if (hub == null || connecting) {
            return;
        }
        connectWanted = true;
        connecting = true;
        hubMessage = "connecting…";
        changed();
        connector.execute(() -> connectNow(true)); // now, even if a background attempt is under way
    }

    synchronized void requestDisconnect() {
        if (hub == null) {
            return;
        }
        connectWanted = false; // stops the 2 s reconnects right away
        hubThread.execute(this::disconnect);
    }

    void requestRefresh() {
        hubThread.execute(this::refreshHub);
    }

    synchronized void setAutoPush(boolean on) {
        if (autoPush != on) {
            autoPush = on;
            log("auto-push on save: " + (on ? "on" : "off"));
            changed();
        }
    }

    /** Blocks until the state is newer than {@code since} or the timeout passes, then returns it. */
    synchronized JsonObject awaitState(long since, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (version == since) {
            long left = deadline - System.currentTimeMillis();
            if (left <= 0) {
                break;
            }
            wait(left);
        }
        return state();
    }

    // ---- state as JSON (holding the lock) -------------------------------------------------------

    private JsonObject state() {
        JsonObject out = new JsonObject();
        out.addProperty("version", version);
        out.addProperty("autoPush", autoPush);

        JsonObject h = new JsonObject();
        h.addProperty("serial", serial);
        h.addProperty("adb", hub != null);
        h.addProperty("online", hubOnline);
        h.addProperty("message", hubMessage);
        h.addProperty("wanted", connectWanted);
        h.addProperty("connecting", connecting);
        out.add("hub", h);

        Set<String> names = new TreeSet<>();
        for (Entry e : routes.values()) {
            if (e.present) {
                names.add(e.name);
            }
        }
        if (hubFiles != null) {
            names.addAll(hubFiles.keySet());
        }
        JsonArray list = new JsonArray();
        for (String name : names) {
            Entry e = routes.get(name);
            boolean present = e != null && e.present;
            String onHub = hubFiles == null ? null : hubFiles.get(name);
            JsonObject r = new JsonObject();
            r.addProperty("name", name);
            r.addProperty("present", present);
            if (present) {
                r.addProperty("checksum", e.checksum);
                r.addProperty("valid", e.check.valid());
                r.addProperty("error", e.check.error);
                r.addProperty("warnings", e.check.warnings.size());
                r.addProperty("title", e.check.routeName);
            }
            r.addProperty("hubChecksum", onHub);
            r.addProperty("sync", hubFiles == null ? "unknown"
                    : !present ? "hubOnly"
                    : onHub == null ? "missing"
                    : onHub.equals(e.checksum) ? "same" : "differs");
            if (e != null) {
                r.addProperty("pending", e.pending);
                r.addProperty("event", e.event);
                r.addProperty("eventOk", e.eventOk);
                r.addProperty("eventAt", e.eventAt);
            }
            list.add(r);
        }
        out.add("routes", list);

        JsonObject r = new JsonObject();
        r.addProperty("available", robot != null && robotStatus != null);
        r.addProperty("problem", robot == null && robotProblem == null && hubOnline ? "setting up…" : robotProblem);
        if (robotStatus != null) {
            r.add("status", robotStatus);
        }
        out.add("robot", r);
        return out;
    }

    /** The full check and both alliances' maps for one route, or null if there's no such file. */
    synchronized JsonObject detail(String name) {
        Entry e = routes.get(name);
        if (e == null || !e.present) {
            return null;
        }
        JsonObject out = checkJson(e.check);
        out.addProperty("name", name);
        out.addProperty("file", AUTOS + "/" + name + ".json");
        out.addProperty("checksum", e.checksum);
        return out;
    }

    private static JsonObject checkJson(RouteCheck.Result check) {
        JsonObject out = new JsonObject();
        out.addProperty("valid", check.valid());
        out.addProperty("error", check.error);
        out.addProperty("title", check.routeName);
        JsonArray warnings = new JsonArray();
        for (String w : check.warnings) {
            warnings.add(w);
        }
        out.add("warnings", warnings);
        if (check.valid()) {
            out.add("red", check.red);
            out.add("blue", check.blue);
        }
        return out;
    }

    // ---- the page's editor (web server threads) -------------------------------------------------

    /** An HTTP status and JSON body for the web server to send. */
    static final class Reply {
        final int status;
        final JsonObject body;

        Reply(int status, JsonObject body) {
            this.status = status;
            this.body = body;
        }

        static Reply error(int status, String message) {
            JsonObject body = new JsonObject();
            body.addProperty("error", message);
            return new Reply(status, body);
        }
    }

    /** A route file's text and checksum, for the editor. */
    Reply source(String name) throws IOException {
        File f = routeFile(name);
        if (f == null || !f.isFile()) {
            return Reply.error(404, "no route file " + name + ".json");
        }
        byte[] bytes = Files.readAllBytes(f.toPath());
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("text", new String(bytes, StandardCharsets.UTF_8));
        body.addProperty("checksum", RouteChecksum.of(bytes));
        return new Reply(200, body);
    }

    /** Checks and draws unsaved editor text exactly as a save would be checked. */
    JsonObject checkDraft(String text) {
        return checkJson(RouteCheck.check(text, registrations));
    }

    /**
     * Writes editor text to the route file. Refuses text the robot would reject (422), and text
     * edited from a version that is no longer on disk (409: someone saved in another editor),
     * unless {@code force}. The watcher picks the file up at once, so a save pushes like any other.
     */
    Reply save(String name, String text, String base, boolean force) throws IOException {
        File f = routeFile(name);
        if (f == null || !f.isFile()) {
            return Reply.error(404, "no route file " + name + ".json");
        }
        RouteCheck.Result check = RouteCheck.check(text, registrations);
        if (!check.valid()) {
            return Reply.error(422, check.error);
        }
        byte[] bytes;
        synchronized (saveLock) {
            byte[] current = Files.readAllBytes(f.toPath());
            String onDisk = RouteChecksum.of(current);
            if (!force && !onDisk.equals(base)) {
                Reply conflict = Reply.error(409, "the file changed on disk since the editor loaded it");
                conflict.body.addProperty("checksum", onDisk);
                return conflict;
            }
            // Browsers hand back \n; keep the file's own line endings so git sees only real changes.
            String body = text.replace("\r\n", "\n");
            if (new String(current, StandardCharsets.UTF_8).contains("\r\n")) {
                body = body.replace("\n", "\r\n");
            }
            bytes = body.getBytes(StandardCharsets.UTF_8);
            // Write a hidden temp file (the watcher only reads *.json), then swap it in, so no
            // reader ever sees half a file.
            File temp = new File(autosDir, "." + name + ".json.tmp");
            Files.write(temp.toPath(), bytes);
            try {
                Files.move(temp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        watcher.execute(() -> pollFiles(false)); // now, not at the next 200 ms poll
        JsonObject body = new JsonObject();
        body.addProperty("checksum", RouteChecksum.of(bytes));
        return new Reply(200, body);
    }

    private File routeFile(String name) {
        return name != null && ROUTE_NAME.matcher(name).matches() ? new File(autosDir, name + ".json") : null;
    }

    // ---- helpers ------------------------------------------------------------------------------

    private void changed() {
        version++;
        notifyAll();
    }

    private static void event(Entry e, boolean ok, String text) {
        e.event = text;
        e.eventOk = ok;
        e.eventAt = System.currentTimeMillis();
    }

    private static String describe(RouteCheck.Result check) {
        if (!check.valid()) {
            return "INVALID: " + check.error;
        }
        return check.warnings.isEmpty() ? "valid" : "valid, " + check.warnings.size() + " warning(s)";
    }

    private static void log(String message) {
        System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + "  " + message);
    }

    /** A log line with the terminal bell, for things that need attention. */
    private static void alarm(String message) {
        log(message + "\u0007");
    }

    private static ThreadFactory daemon(String name) {
        return r -> {
            Thread t = new Thread(r, "routeview-" + name);
            t.setDaemon(true);
            return t;
        };
    }

    private static void openBrowser(String url) {
        String os = System.getProperty("os.name", "").toLowerCase();
        String[] command = os.startsWith("mac") ? new String[] {"open", url}
                : os.startsWith("windows") ? new String[] {"rundll32", "url.dll,FileProtocolHandler", url}
                : new String[] {"xdg-open", url};
        try {
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            log("couldn't open a browser: " + e.getMessage());
        }
    }
}
