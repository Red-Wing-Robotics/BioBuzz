package org.firstinspires.ftc.teamcode.auto;

import com.bylazar.configurables.annotations.Configurable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The waypoints of the currently loaded route, exposed to Panels (dev builds,
 * http://192.168.43.1:8001 → Configurables) for live tuning.
 *
 * Workflow: INIT the auto (loads the route file into {@link #waypoints}), set
 * {@link #reloadFromFileOnInit} to false in Panels, edit waypoints, re-INIT to rebuild the paths,
 * repeat. Copy the final numbers back into the route JSON — Panels edits are not saved.
 */
@Configurable
public final class AutoWaypoints {
    /** Name of the route the waypoints below came from. */
    public static String route = "";
    /** true: every INIT reloads waypoints from the route file (discarding Panels edits). */
    public static boolean reloadFromFileOnInit = true;
    /** RED-frame waypoints of the loaded route, by name. */
    public static Map<String, Waypoint> waypoints = new LinkedHashMap<>();

    private AutoWaypoints() {}
}
