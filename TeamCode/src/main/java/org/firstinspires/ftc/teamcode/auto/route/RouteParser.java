package org.firstinspires.ftc.teamcode.auto.route;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.firstinspires.ftc.teamcode.auto.Waypoint;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses and validates the top level of a route file:
 *
 * <pre>
 * {
 *   "name": "Leave + Park",                      // optional
 *   "waypoints": { "START": [x, y, headingDeg], ... },
 *   "start": "START",
 *   "routine": [ step, step, ... ]               // see RouteCompiler for step syntax
 * }
 * </pre>
 */
public final class RouteParser {

    private RouteParser() {}

    public static Route parse(String json) {
        JsonObject root;
        try {
            JsonElement element = new JsonParser().parse(json); // Gson 2.8.0 API (SDK-bundled)
            if (!element.isJsonObject()) {
                throw new RouteException("route: top level must be an object");
            }
            root = element.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new RouteException("route: invalid JSON — " + e.getMessage(), e);
        }

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("name") && !key.equals("waypoints") && !key.equals("start") && !key.equals("routine")) {
                throw new RouteException("route: unknown key \"" + key + "\" (allowed: name, waypoints, start, routine)");
            }
        }

        String name = root.has("name") ? root.get("name").getAsString() : "";

        JsonElement wps = root.get("waypoints");
        if (wps == null || !wps.isJsonObject()) {
            throw new RouteException("route: \"waypoints\" must be an object of NAME: [x, y, headingDeg]");
        }
        LinkedHashMap<String, Waypoint> waypoints = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : wps.getAsJsonObject().entrySet()) {
            waypoints.put(entry.getKey(), parseWaypoint(entry.getKey(), entry.getValue()));
        }
        if (waypoints.isEmpty()) {
            throw new RouteException("route: \"waypoints\" is empty");
        }

        JsonElement start = root.get("start");
        if (start == null || !start.isJsonPrimitive()) {
            throw new RouteException("route: \"start\" must name a waypoint");
        }
        if (!waypoints.containsKey(start.getAsString())) {
            throw new RouteException("route: start waypoint \"" + start.getAsString() + "\" is not defined (have " + waypoints.keySet() + ")");
        }

        JsonElement routine = root.get("routine");
        if (routine == null || !routine.isJsonArray()) {
            throw new RouteException("route: \"routine\" must be an array of steps");
        }

        return new Route(name, waypoints, start.getAsString(), routine.getAsJsonArray());
    }

    private static Waypoint parseWaypoint(String name, JsonElement value) {
        if (!value.isJsonArray() || value.getAsJsonArray().size() != 3) {
            throw new RouteException("waypoints." + name + ": must be [x, y, headingDeg]");
        }
        JsonArray a = value.getAsJsonArray();
        try {
            return new Waypoint(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
        } catch (RuntimeException e) {
            throw new RouteException("waypoints." + name + ": values must be numbers", e);
        }
    }
}
