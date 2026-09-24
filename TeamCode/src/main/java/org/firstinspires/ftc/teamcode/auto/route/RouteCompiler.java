package org.firstinspires.ftc.teamcode.auto.route;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.Step;
import org.firstinspires.ftc.teamcode.auto.Steps;
import org.firstinspires.ftc.teamcode.auto.Waypoint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a route's {@code "routine"} array into a {@link Step} tree for one alliance, validating
 * every reference. Step syntax (exactly one kind key per step, plus optional modifiers):
 *
 * <pre>
 * { "sequence": [ ... ] }   { "parallel": [ ... ] }   { "race": [ ... ] }
 * { "follow": { "line": ["A", "B"] } }
 * { "follow": { "curve": ["A", "C1", "C2", "B"] } }        // control points
 * { "follow": { "through": ["A", "B", "C"] } }             // passes through every point
 *     optional "heading": "tangent" | "reverseTangent" | { "constant": deg }
 *                       | { "linear": [deg0, deg1] } | { "facing": "WAYPOINT" }
 *     (default: linear from the first waypoint's heading to the last's)
 * { "wait": seconds }
 * { "waitUntil": "condition.name" }
 * { "action": "shooter.spinUp", "args": { ... } }
 * { "hold": "WAYPOINT", "timeout": seconds }               // timeout required
 *
 * modifiers on any step:  "name": "shown on DS",  "timeout": seconds
 * </pre>
 */
public final class RouteCompiler {

    private static final Set<String> KINDS = new HashSet<>(Arrays.asList(
            "sequence", "parallel", "race", "follow", "wait", "waitUntil", "action", "hold"));
    private static final Set<String> MODIFIERS = new HashSet<>(Arrays.asList("name", "timeout", "args"));
    private static final Set<String> PATH_KINDS = new HashSet<>(Arrays.asList("line", "curve", "through"));

    private final Alliance alliance;
    private final Map<String, Waypoint> waypoints;
    private final ActionRegistry registry;

    public RouteCompiler(Alliance alliance, Map<String, Waypoint> waypoints, ActionRegistry registry) {
        this.alliance = alliance;
        this.waypoints = waypoints;
        this.registry = registry;
    }

    public Pose startPose(Route route) {
        return pose(route.start, "start");
    }

    public Step compile(Route route) {
        return compileSequence(route.routine, "routine");
    }

    // ---- steps ------------------------------------------------------------------------

    private Step compileSequence(JsonArray steps, String at) {
        Step[] compiled = new Step[steps.size()];
        for (int i = 0; i < steps.size(); i++) {
            compiled[i] = compileStep(steps.get(i), at + "[" + i + "]");
        }
        return Steps.sequence(compiled);
    }

    private Step[] compileList(JsonElement element, String at) {
        if (!element.isJsonArray()) {
            throw new RouteException(at + ": must be an array of steps");
        }
        JsonArray array = element.getAsJsonArray();
        Step[] compiled = new Step[array.size()];
        for (int i = 0; i < array.size(); i++) {
            compiled[i] = compileStep(array.get(i), at + "[" + i + "]");
        }
        return compiled;
    }

    Step compileStep(JsonElement element, String at) {
        if (!element.isJsonObject()) {
            throw new RouteException(at + ": a step must be an object");
        }
        JsonObject step = element.getAsJsonObject();

        String kind = null;
        for (Map.Entry<String, JsonElement> entry : step.entrySet()) {
            String key = entry.getKey();
            if (KINDS.contains(key)) {
                if (kind != null) {
                    throw new RouteException(at + ": has both \"" + kind + "\" and \"" + key + "\" — one kind per step");
                }
                kind = key;
            } else if (!MODIFIERS.contains(key)) {
                throw new RouteException(at + ": unknown key \"" + key + "\" (kinds: " + KINDS + ", modifiers: " + MODIFIERS + ")");
            }
        }
        if (kind == null) {
            throw new RouteException(at + ": needs one of " + KINDS);
        }
        String kindAt = at + "." + kind;
        JsonElement value = step.get(kind);

        Step compiled;
        switch (kind) {
            case "sequence":
                compiled = Steps.sequence(compileList(value, kindAt));
                break;
            case "parallel":
                compiled = Steps.parallel(compileList(value, kindAt));
                break;
            case "race":
                compiled = Steps.race(compileList(value, kindAt));
                break;
            case "follow":
                compiled = Steps.follow(describePath(value), compilePath(value, kindAt));
                break;
            case "wait":
                compiled = Steps.waitSeconds(seconds(value, kindAt));
                break;
            case "waitUntil":
                compiled = Steps.waitUntil(string(value, kindAt), registry.condition(string(value, kindAt), kindAt));
                break;
            case "action": {
                JsonElement args = step.get("args");
                if (args != null && !args.isJsonObject()) {
                    throw new RouteException(at + ".args: must be an object");
                }
                compiled = Steps.named(string(value, kindAt),
                        registry.action(string(value, kindAt), args == null ? new JsonObject() : args.getAsJsonObject(), kindAt));
                break;
            }
            case "hold":
                if (!step.has("timeout")) {
                    throw new RouteException(at + ": \"hold\" runs until interrupted — give it a \"timeout\"");
                }
                compiled = Steps.hold(string(value, kindAt), pose(string(value, kindAt), kindAt));
                break;
            default:
                throw new IllegalStateException(kind);
        }

        if (step.has("timeout")) {
            compiled = Steps.withTimeout(seconds(step.get("timeout"), at + ".timeout"), compiled);
        }
        if (step.has("name")) {
            compiled = Steps.named(string(step.get("name"), at + ".name"), compiled);
        }
        return compiled;
    }

    // ---- paths ------------------------------------------------------------------------

    /** Public so tests can inspect compiled paths ({@code endPose()}, {@code heading(t)}). */
    public Path compilePath(JsonElement element, String at) {
        if (!element.isJsonObject()) {
            throw new RouteException(at + ": must be an object like { \"line\": [\"A\", \"B\"] }");
        }
        JsonObject spec = element.getAsJsonObject();

        String kind = null;
        for (Map.Entry<String, JsonElement> entry : spec.entrySet()) {
            String key = entry.getKey();
            if (PATH_KINDS.contains(key)) {
                if (kind != null) {
                    throw new RouteException(at + ": has both \"" + kind + "\" and \"" + key + "\"");
                }
                kind = key;
            } else if (!key.equals("heading")) {
                throw new RouteException(at + ": unknown key \"" + key + "\" (allowed: line, curve, through, heading)");
            }
        }
        if (kind == null) {
            throw new RouteException(at + ": needs one of " + PATH_KINDS);
        }

        Pose[] poses = poses(spec.get(kind), at + "." + kind);
        Path path;
        switch (kind) {
            case "line":
                if (poses.length != 2) {
                    throw new RouteException(at + ".line: needs exactly 2 waypoints");
                }
                path = Paths.line(poses[0], poses[1]);
                break;
            case "curve":
                if (poses.length < 2) {
                    throw new RouteException(at + ".curve: needs at least 2 waypoints");
                }
                path = Paths.curve(poses);
                break;
            case "through":
                if (poses.length < 2) {
                    throw new RouteException(at + ".through: needs at least 2 waypoints");
                }
                path = Paths.through(poses);
                break;
            default:
                throw new IllegalStateException(kind);
        }
        return applyHeading(path, spec.get("heading"), poses, at + ".heading");
    }

    private Path applyHeading(Path path, JsonElement heading, Pose[] poses, String at) {
        if (heading == null) {
            return path.linear(poses[0], poses[poses.length - 1]);
        }
        if (heading.isJsonPrimitive() && heading.getAsJsonPrimitive().isString()) {
            switch (heading.getAsString()) {
                case "tangent":
                    return path.tangent();
                case "reverseTangent":
                    return path.reverseTangent();
                default:
                    throw new RouteException(at + ": unknown heading \"" + heading.getAsString() + "\" (tangent, reverseTangent, or an object)");
            }
        }
        if (heading.isJsonObject()) {
            JsonObject h = heading.getAsJsonObject();
            if (h.size() != 1) {
                throw new RouteException(at + ": use exactly one of constant, linear, facing");
            }
            if (h.has("constant")) {
                return path.constant(headingRadians(h.get("constant").getAsDouble()));
            }
            if (h.has("linear")) {
                JsonArray pair = h.get("linear").getAsJsonArray();
                if (pair.size() != 2) {
                    throw new RouteException(at + ".linear: needs [fromDeg, toDeg]");
                }
                return path.linear(headingRadians(pair.get(0).getAsDouble()), headingRadians(pair.get(1).getAsDouble()));
            }
            if (h.has("facing")) {
                return path.facingPoint(pose(h.get("facing").getAsString(), at + ".facing"));
            }
        }
        throw new RouteException(at + ": expected \"tangent\", \"reverseTangent\", or { constant | linear | facing }");
    }

    // ---- helpers ----------------------------------------------------------------------

    private Pose[] poses(JsonElement element, String at) {
        if (!element.isJsonArray()) {
            throw new RouteException(at + ": must be an array of waypoint names");
        }
        JsonArray names = element.getAsJsonArray();
        Pose[] poses = new Pose[names.size()];
        for (int i = 0; i < names.size(); i++) {
            poses[i] = pose(string(names.get(i), at + "[" + i + "]"), at + "[" + i + "]");
        }
        return poses;
    }

    private Pose pose(String waypoint, String at) {
        Waypoint w = waypoints.get(waypoint);
        if (w == null) {
            throw new RouteException(at + ": unknown waypoint \"" + waypoint + "\" (have " + waypoints.keySet() + ")");
        }
        return alliance.pose(w);
    }

    /** A RED-frame heading in degrees, mapped into this alliance's frame. */
    private double headingRadians(double redDegrees) {
        return alliance.poses().of(0, 0, redDegrees).heading();
    }

    private static String describePath(JsonElement spec) {
        if (spec.isJsonObject()) {
            for (String kind : PATH_KINDS) {
                JsonElement names = spec.getAsJsonObject().get(kind);
                if (names != null && names.isJsonArray()) {
                    List<String> list = new java.util.ArrayList<>();
                    for (JsonElement n : names.getAsJsonArray()) {
                        list.add(n.isJsonPrimitive() ? n.getAsString() : "?");
                    }
                    return kind + " " + String.join("→", list);
                }
            }
        }
        return "path";
    }

    private static String string(JsonElement element, String at) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new RouteException(at + ": must be a string");
        }
        return element.getAsString();
    }

    private static double seconds(JsonElement element, String at) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new RouteException(at + ": must be a number of seconds");
        }
        double s = element.getAsDouble();
        if (s < 0) {
            throw new RouteException(at + ": must not be negative");
        }
        return s;
    }
}
