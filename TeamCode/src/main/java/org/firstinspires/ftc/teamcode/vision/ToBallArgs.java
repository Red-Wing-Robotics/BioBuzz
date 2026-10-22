package org.firstinspires.ftc.teamcode.vision;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.firstinspires.ftc.teamcode.auto.route.RouteException;

import java.util.Map;

/**
 * A route's {@code "args"} for a ball seek:
 *
 * <pre>
 * { "seek": "vision.ball", "args": { "colors": "pollen", "within": 30, "standoff": 9, "look": 0.5 }, "timeout": 4 }
 * </pre>
 *
 * All optional: {@code colors} is {@code "pollen"}, {@code "nectar"} (ours) or {@code "ours"}
 * (both); {@code within} and {@code standoff} are inches from the robot's tracking centre;
 * {@code look} is how long to wait for a ball, seconds; {@code prefer} is {@code "most"} (a
 * bigger group of balls about as near wins; the default) or {@code "nearest"}. Bad args fail at
 * INIT.
 */
public final class ToBallArgs {

    private ToBallArgs() {}

    /** {@code defaults} with {@code args} applied. */
    public static DriveToBall.Settings parse(JsonObject args, DriveToBall.Settings defaults) {
        DriveToBall.Settings s = defaults.copy();
        for (Map.Entry<String, JsonElement> e : args.entrySet()) {
            JsonElement v = e.getValue();
            switch (e.getKey()) {
                case "colors": {
                    PickupRules.Colors colors = v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()
                            ? PickupRules.Colors.parse(v.getAsString()) : null;
                    if (colors == null) {
                        throw new RouteException("args.colors: \"pollen\", \"nectar\" or \"ours\"");
                    }
                    s.colors = colors;
                    break;
                }
                case "within":
                    s.within = number(v, "args.within", "inches", false);
                    break;
                case "standoff":
                    s.standoff = number(v, "args.standoff", "inches", true);
                    break;
                case "look":
                    s.lookSeconds = number(v, "args.look", "seconds", true);
                    break;
                case "prefer": {
                    String p = v.isJsonPrimitive() && v.getAsJsonPrimitive().isString() ? v.getAsString() : "";
                    if (!p.equals("most") && !p.equals("nearest")) {
                        throw new RouteException("args.prefer: \"most\" or \"nearest\"");
                    }
                    s.preferGroups = p.equals("most");
                    break;
                }
                default:
                    throw new RouteException("args: unknown key \"" + e.getKey() + "\" (allowed: colors, within, standoff, look, prefer)");
            }
        }
        return s;
    }

    private static double number(JsonElement v, String at, String unit, boolean zeroAllowed) {
        if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) {
            throw new RouteException(at + ": must be a number of " + unit);
        }
        double n = v.getAsDouble();
        if (n < 0 || (!zeroAllowed && n == 0)) {
            throw new RouteException(at + ": must be " + (zeroAllowed ? "0 or more" : "more than 0"));
        }
        return n;
    }
}
