package org.firstinspires.ftc.teamcode.auto;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Driver Station telemetry of the latest OpMode, copied for Jimothy (see
 * {@link RouteViewRemote}): the lines the Driver Station shows, in its order. Kept after the
 * OpMode stops, so a finished run can still be read. Dev builds only (R704).
 *
 * The SDK sends each line under a key that sorts into display order: a NUL, then a character
 * counting up. Such lines are already "caption : value"; anything else shows as key and value.
 * Without the SDK, so it's unit tested; RouteViewRemote synchronizes nothing itself.
 */
final class TelemetryTap {

    private String opMode;
    private List<String> lines = Collections.emptyList();

    /** A new OpMode is INITing: forget the last one's lines. */
    synchronized void begin(String opModeName) {
        opMode = opModeName;
        lines = Collections.emptyList();
    }

    /** One telemetry message as the SDK sent it to the Driver Station: key → text. */
    synchronized void record(Map<String, String> data) {
        lines = lines(data);
    }

    /** {@code {"opMode": name, "lines": [...]}}, or null before any OpMode has run. */
    synchronized JsonObject toJson() {
        if (opMode == null) {
            return null;
        }
        JsonObject out = new JsonObject();
        out.addProperty("opMode", opMode);
        JsonArray list = new JsonArray();
        for (String line : lines) {
            list.add(line);
        }
        out.add("lines", list);
        return out;
    }

    static List<String> lines(Map<String, String> data) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, String> e : new TreeMap<>(data).entrySet()) {
            String key = e.getKey();
            out.add(key.startsWith("\0") ? e.getValue() : key + " : " + e.getValue());
        }
        return out;
    }
}
