package org.firstinspires.ftc.teamcode.auto.route;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import org.firstinspires.ftc.teamcode.auto.Waypoint;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
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
 *
 * The JSON must be strict: no comments or unquoted text, and no key twice in one object, so a
 * waypoint defined twice is an error rather than a silent override.
 */
public final class RouteParser {

    private RouteParser() {}

    public static Route parse(String json) {
        JsonElement element = parseJson(json);
        if (!element.isJsonObject()) {
            throw new RouteException("route: top level must be an object");
        }
        JsonObject root = element.getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("name") && !key.equals("waypoints") && !key.equals("start") && !key.equals("routine")) {
                throw new RouteException("route: unknown key \"" + key + "\" (allowed: name, waypoints, start, routine)");
            }
        }

        JsonElement nameElement = root.get("name");
        if (nameElement != null && !isString(nameElement)) {
            throw new RouteException("route: \"name\" must be a string");
        }
        String name = nameElement != null ? nameElement.getAsString() : "";

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
        if (start == null || !isString(start)) {
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
        for (JsonElement v : a) {
            if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) {
                throw new RouteException("waypoints." + name + ": values must be numbers");
            }
        }
        return new Waypoint(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }

    private static boolean isString(JsonElement e) {
        return e.isJsonPrimitive() && e.getAsJsonPrimitive().isString();
    }

    // ---- strict JSON ------------------------------------------------------------------

    /**
     * Gson 2.8.0's {@code JsonParser} (the SDK's) is always lenient and keeps the last of two
     * equal keys, so this reads the tree itself with a strict {@link JsonReader}.
     */
    private static JsonElement parseJson(String json) {
        JsonReader reader = new JsonReader(new StringReader(json));
        try {
            JsonElement element = read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new RouteException("route: invalid JSON — more text after the end");
            }
            return element;
        } catch (IOException e) {
            String why = String.valueOf(e.getMessage())
                    .replace("Use JsonReader.setLenient(true) to accept malformed JSON", "unexpected text");
            throw new RouteException("route: invalid JSON — " + why, e);
        }
    }

    private static JsonElement read(JsonReader in) throws IOException {
        switch (in.peek()) {
            case BEGIN_OBJECT: {
                JsonObject object = new JsonObject();
                in.beginObject();
                while (in.hasNext()) {
                    String key = in.nextName();
                    if (object.has(key)) {
                        throw new RouteException(location(in) + ": defined twice");
                    }
                    object.add(key, read(in));
                }
                in.endObject();
                return object;
            }
            case BEGIN_ARRAY: {
                JsonArray array = new JsonArray();
                in.beginArray();
                while (in.hasNext()) {
                    array.add(read(in));
                }
                in.endArray();
                return array;
            }
            case STRING:
                return new JsonPrimitive(in.nextString());
            case NUMBER:
                return new JsonPrimitive(new BigDecimal(in.nextString())); // exact, and 3 stays an int for args
            case BOOLEAN:
                return new JsonPrimitive(in.nextBoolean());
            case NULL:
                in.nextNull();
                return JsonNull.INSTANCE;
            default:
                throw new RouteException("route: invalid JSON — unexpected " + in.peek() + " at " + location(in));
        }
    }

    /** Where the reader is, written like the other messages: {@code waypoints.START}, {@code routine[2]}. */
    private static String location(JsonReader in) {
        String path = in.getPath();
        return path.startsWith("$.") ? path.substring(2) : path;
    }
}
