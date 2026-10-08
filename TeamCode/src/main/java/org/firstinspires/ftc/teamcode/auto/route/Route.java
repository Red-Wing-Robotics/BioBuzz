package org.firstinspires.ftc.teamcode.auto.route;

import com.google.gson.JsonArray;

import org.firstinspires.ftc.teamcode.auto.Waypoint;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A parsed but not yet compiled route file. Waypoints are in the RED frame (docs/coordinates.md). */
public final class Route {
    public final String name;
    public final Map<String, Waypoint> waypoints;
    public final String start;
    public final JsonArray routine;

    Route(String name, LinkedHashMap<String, Waypoint> waypoints, String start, JsonArray routine) {
        this.name = name;
        this.waypoints = Collections.unmodifiableMap(waypoints);
        this.start = start;
        this.routine = routine;
    }
}
