package org.firstinspires.ftc.teamcode.routeview;

import org.firstinspires.ftc.teamcode.auto.route.RouteChecksum;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The route overrides on the Control Hub ({@code /sdcard/FIRST/autos}, which wins over the APK's
 * copy; see RouteFiles), reached over adb. Every call blocks on adb, so RouteView makes them all
 * from one thread.
 */
final class Hub {

    static final String DIR = "/sdcard/FIRST/autos/";

    /** An adb operation failed; the message is adb's explanation. */
    static final class Failure extends Exception {
        Failure(String message) {
            super(message);
        }
    }

    final String serial;
    private final Adb adb;

    Hub(Adb adb, String serial) {
        this.adb = adb;
        this.serial = serial;
    }

    /** A Wi-Fi serial like 192.168.43.1:5555, which adb has to be told to connect to. */
    boolean overNetwork() {
        return serial.contains(":");
    }

    boolean online() {
        return "device".equals(state());
    }

    /** adb's word for the hub: "device" (ready), "offline", "unauthorized", …, or null if adb has no such device. */
    String state() {
        return state(adb.run(3000, "-s", serial, "get-state"));
    }

    static String state(Adb.Result getState) {
        if (getState.ok()) {
            return getState.text();
        }
        String problem = getState.problem();
        for (String s : new String[] {"offline", "unauthorized", "authorizing", "connecting"}) {
            if (problem.contains("device " + s) || problem.contains("device is " + s)) {
                return s;
            }
        }
        return null;
    }

    /**
     * {@code adb connect} for a Wi-Fi serial; returns what happened, for the page. adb keeps a
     * connection that dropped or never finished as "offline" and then answers every connect with
     * "already connected" without trying, so an offline one is dropped and connected fresh.
     */
    String connect() {
        if ("offline".equals(state())) {
            adb.run(5000, "disconnect", serial);
        }
        Adb.Result r = adb.run(5000, "connect", serial);
        if (r.timedOut) {
            return "no answer from " + serial + ". Is this laptop on the robot's Wi-Fi?";
        }
        String reply = r.text().isEmpty() ? r.problem() : r.text();
        if (reply.contains("No route to host") || reply.contains("unreachable") || reply.contains("timed out")) {
            return reply + ". Is this laptop on the robot's Wi-Fi?";
        }
        if (!reply.contains("connected to")) {
            return reply;
        }
        // The TCP connection is up; adb's handshake with the hub finishes a moment later.
        String state = null;
        for (int i = 0; i < 8; i++) {
            state = state();
            if ("device".equals(state)) {
                return reply;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if ("unauthorized".equals(state)) {
            return "the hub refused adb (unauthorized). Check its adb settings, then Connect again.";
        }
        return "reached " + serial + ", but the hub isn't answering adb (adb says " + state
                + "). Is this laptop on the robot's Wi-Fi? Otherwise restart the robot.";
    }

    /** {@code adb disconnect} for a Wi-Fi serial; returns adb's reply. */
    String disconnect() {
        Adb.Result r = adb.run(5000, "disconnect", serial);
        return r.text().isEmpty() ? r.problem() : r.text();
    }

    /** Route name → {@link RouteChecksum} of every override on the hub. */
    Map<String, String> overrides() throws Failure {
        Map<String, String> found = new TreeMap<>();
        Adb.Result ls = adb.run(5000, "-s", serial, "shell", "ls", "-1", DIR);
        if (!ls.ok()) {
            if (ls.problem().contains("No such file")) {
                return found; // nothing pushed yet: the robot uses the APK copies
            }
            throw new Failure("listing " + DIR + ": " + ls.problem());
        }
        for (String name : routeNames(ls.text())) {
            Adb.Result cat = adb.run(5000, "-s", serial, "exec-out", "cat", DIR + name + ".json");
            if (!cat.ok()) {
                throw new Failure("reading " + DIR + name + ".json: " + cat.problem());
            }
            found.put(name, RouteChecksum.of(cat.out));
        }
        return found;
    }

    void push(File local, String name) throws Failure {
        // The full remote path, so adb creates /sdcard/FIRST/autos if it doesn't exist yet.
        Adb.Result r = adb.run(15000, "-s", serial, "push", local.getPath(), DIR + name + ".json");
        if (!r.ok()) {
            throw new Failure(r.problem());
        }
    }

    void remove(String name) throws Failure {
        Adb.Result r = adb.run(5000, "-s", serial, "shell", "rm", "-f", DIR + name + ".json");
        if (!r.ok()) {
            throw new Failure(r.problem());
        }
    }

    /** Route names from {@code ls -1} output: the {@code .json} files, without the extension. */
    static List<String> routeNames(String ls) {
        List<String> names = new ArrayList<>();
        for (String line : ls.split("\\r?\\n")) {
            String f = line.trim();
            if (f.endsWith(".json") && f.length() > ".json".length()) {
                names.add(f.substring(0, f.length() - ".json".length()));
            }
        }
        return names;
    }
}
