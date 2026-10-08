package org.firstinspires.ftc.teamcode.auto;

import java.util.Locale;

/**
 * The rules for running an OpMode from Jimothy (see {@link RouteViewRemote}), without the SDK so
 * they're unit tested. Not thread-safe: RouteViewRemote synchronizes on it. Times are milliseconds
 * from any steady clock.
 *
 * An OpMode STARTed from here is stopped if Jimothy goes quiet for {@link #HEARTBEAT_TIMEOUT_MS}
 * or after {@link #AUTO_LIMIT_MS}. One started on the Driver Station is left to the Driver Station.
 */
final class RemoteRunGuard {

    static final long HEARTBEAT_TIMEOUT_MS = 1500;
    static final long AUTO_LIMIT_MS = 30_000;

    /** The OpMode in INIT or RUNNING, or null. */
    String active;
    /** IDLE, INIT or RUNNING. */
    String phase = "IDLE";
    long phaseSince;
    /** START came from Jimothy, so the heartbeat and time limit apply. */
    boolean startedHere;
    /** Why the last OpMode was stopped from here, for the page. */
    String lastStop;
    /** How the last OpMode ended, e.g. "Red: Leave + Park ended in INIT after 0.0 s". */
    String lastEnded;

    private boolean startRequested;
    private long lastHeard;

    /** The SDK reports an OpMode entering INIT, RUNNING or stopping (IDLE); {@code name} null = idle. */
    void phase(String name, String next, long now) {
        boolean idle = name == null || next.equals("IDLE");
        if (idle && active != null) {
            // An INIT that throws ends within milliseconds, between two polls; this keeps it visible.
            lastEnded = String.format(Locale.US, "%s ended in %s after %.1f s", active, phase, (now - phaseSince) / 1000.0);
        }
        active = idle ? null : name;
        phase = idle ? "IDLE" : next;
        phaseSince = now;
        startedHere = !idle && next.equals("RUNNING") && startRequested;
        startRequested = false;
    }

    /** Jimothy asked for the status; each ask is a heartbeat. */
    void heard(long now) {
        lastHeard = now;
    }

    /** Null if INIT may go ahead, else why not. */
    String init() {
        if (active != null) {
            return active + " is in " + phase + "; stop it first";
        }
        lastStop = null;
        lastEnded = null;
        return null;
    }

    /** Null if START may go ahead (the SDK then reports RUNNING), else why not. */
    String start(long now) {
        if (active == null || !phase.equals("INIT")) {
            return "nothing is in INIT";
        }
        startRequested = true;
        lastHeard = now;
        return null;
    }

    /** True if there's an OpMode to stop. */
    boolean stop() {
        if (active == null) {
            return false;
        }
        startedHere = false;
        lastStop = "stopped from Jimothy";
        return true;
    }

    /** Called often: non-null means stop the active OpMode now, for this reason. */
    String check(long now) {
        if (!startedHere || !phase.equals("RUNNING")) {
            return null;
        }
        String why = now - lastHeard > HEARTBEAT_TIMEOUT_MS ? "Jimothy stopped answering"
                : now - phaseSince > AUTO_LIMIT_MS ? "30 s AUTO limit" : null;
        if (why == null) {
            return null;
        }
        startedHere = false;
        lastStop = "stopped " + active + ": " + why;
        return lastStop;
    }
}
