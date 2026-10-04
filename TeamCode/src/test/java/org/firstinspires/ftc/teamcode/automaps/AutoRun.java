package org.firstinspires.ftc.teamcode.automaps;

import static org.firstinspires.ftc.teamcode.automaps.Geometry.footprint;
import static org.firstinspires.ftc.teamcode.automaps.Geometry.overlaps;
import static org.firstinspires.ftc.teamcode.automaps.Geometry.wallGap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.Waypoint;
import org.firstinspires.ftc.teamcode.auto.route.ActionRegistry;
import org.firstinspires.ftc.teamcode.auto.route.Route;
import org.firstinspires.ftc.teamcode.auto.route.RouteCompiler;
import org.firstinspires.ftc.teamcode.automaps.Geometry.Poly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What one route does for one alliance, worked out from the route file without running it: the
 * paths (sampled from the same Pedro {@link Path}s the robot follows, built by
 * {@link RouteCompiler}), the step list with estimated times, estimated points, and rule checks.
 * The rules it applies are written down in docs/auto-maps.md.
 */
final class AutoRun {

    /** One line of the routine list. Leaf steps are numbered; sequence/parallel/race headers are 0. */
    static final class Row {
        final int number;
        final int depth;
        final String text;
        final String time;

        Row(int number, int depth, String text, String time) {
            this.number = number;
            this.depth = depth;
            this.text = text;
            this.time = time;
        }
    }

    /** One followed path, sampled. */
    static final class Leg {
        final int number;
        final List<Pose> samples;
        final double length;

        Leg(int number, List<Pose> samples, double length) {
            this.number = number;
            this.samples = samples;
            this.length = length;
        }
    }

    /** Where a step's number is drawn on the field. */
    static final class Badge {
        final int number;
        final double x;
        final double y;

        Badge(int number, double x, double y) {
            this.number = number;
            this.x = x;
            this.y = y;
        }
    }

    /** One line of the points estimate. */
    static final class Score {
        final String label;
        final int points;
        final boolean earned;
        final String reason;

        Score(String label, int points, boolean earned, String reason) {
            this.label = label;
            this.points = points;
            this.earned = earned;
            this.reason = reason;
        }
    }

    /** A step's estimated duration; {@code unknown} when it depends on something not modelled. */
    private static final class Est {
        final double seconds;
        final boolean unknown;

        Est(double seconds, boolean unknown) {
            this.seconds = seconds;
            this.unknown = unknown;
        }
    }

    private static final List<String> KINDS = Arrays.asList(
            "sequence", "parallel", "race", "follow", "wait", "waitUntil", "action", "hold");

    final Alliance alliance;
    final Route route;
    final Pose start;
    Pose end;
    final List<Row> rows = new ArrayList<>();
    final List<Leg> legs = new ArrayList<>();
    final List<Badge> badges = new ArrayList<>();
    /** Waypoints the routine uses, in this alliance's frame. */
    final Map<String, Pose> waypoints = new LinkedHashMap<>();
    /** Waypoints used only as Bezier control points (the robot doesn't pass through them). */
    final Set<String> controlPoints = new HashSet<>();
    double seconds;
    boolean timeUnknown;
    final List<Score> scores = new ArrayList<>();
    int points;
    final List<String> problems = new ArrayList<>();

    private final RouteCompiler compiler;
    private Pose current;
    private int counter;
    private boolean hasActions;

    AutoRun(Route route, Alliance alliance) {
        this.route = route;
        this.alliance = alliance;
        this.compiler = new RouteCompiler(alliance, route.waypoints, new ActionRegistry());
        this.start = compiler.startPose(route);
        this.current = start;
        waypoints.put(route.start, start);

        Est total = sequence(route.routine, "routine", 0);
        seconds = total.seconds;
        timeUnknown = total.unknown;
        end = current;
        score();
        check();
    }

    // ---- walking the routine ------------------------------------------------------------

    private Est sequence(JsonArray steps, String at, int depth) {
        double sum = 0;
        boolean unknown = false;
        for (int i = 0; i < steps.size(); i++) {
            Est e = step(steps.get(i).getAsJsonObject(), at + "[" + i + "]", depth);
            sum += e.seconds;
            unknown |= e.unknown;
        }
        return new Est(sum, unknown);
    }

    private Est step(JsonObject step, String at, int depth) {
        String kind = null;
        for (String k : KINDS) {
            if (step.has(k)) {
                kind = k;
            }
        }
        if (kind == null) {
            throw new IllegalArgumentException(at + ": no step kind");
        }
        String name = step.has("name") ? step.get("name").getAsString() : null;
        Double timeout = step.has("timeout") ? step.get("timeout").getAsDouble() : null;
        JsonElement value = step.get(kind);

        Est est;
        switch (kind) {
            case "sequence":
            case "parallel":
            case "race":
                est = group(kind, name, value.getAsJsonArray(), at + "." + kind, depth, timeout);
                return est;
            case "follow":
                est = follow(name, value, at + ".follow", depth, timeout);
                return est;
            case "wait":
                est = new Est(value.getAsDouble(), false);
                return leaf(depth, "wait " + secondsText(est.seconds), est, timeout);
            case "waitUntil":
                return leaf(depth, "wait until " + value.getAsString(), new Est(0, true), timeout);
            case "action":
                hasActions = true;
                return leaf(depth, (name != null ? name + ": " : "") + "action " + value.getAsString(), new Est(0, true), timeout);
            case "hold": {
                String target = value.getAsString();
                Pose pose = alliance.pose(route.waypoints.get(target));
                waypoints.put(target, pose);
                current = pose;
                return leaf(depth, (name != null ? name + ": " : "") + "hold " + target, new Est(0, true), timeout);
            }
            default:
                throw new IllegalStateException(kind);
        }
    }

    private Est group(String kind, String name, JsonArray children, String at, int depth, Double timeout) {
        int headerIndex = rows.size();
        rows.add(null);
        Pose before = current;
        Pose after = current;
        double sum = 0, max = 0, min = Double.MAX_VALUE;
        boolean unknown = false;
        for (int i = 0; i < children.size(); i++) {
            if (!kind.equals("sequence")) {
                current = before; // parallel children all start from the same place
            }
            Est e = step(children.get(i).getAsJsonObject(), at + "[" + i + "]", depth + 1);
            sum += e.seconds;
            max = Math.max(max, e.seconds);
            if (!e.unknown) {
                min = Math.min(min, e.seconds);
            }
            unknown |= e.unknown;
            after = current;
        }
        current = after;
        double seconds = kind.equals("sequence") ? sum : kind.equals("parallel") ? max : (min == Double.MAX_VALUE ? 0 : min);
        Est est = bound(new Est(seconds, unknown), timeout);
        String text = (name != null ? name + ": " : "") + kind + (kind.equals("race") ? " (first to finish)" : "");
        rows.set(headerIndex, new Row(0, depth, text, timeText(est, timeout)));
        return est;
    }

    private Est follow(String name, JsonElement spec, String at, int depth, Double timeout) {
        Path path = compiler.compilePath(spec, at);
        int number = ++counter;
        List<Pose> samples = new ArrayList<>();
        double length = 0;
        for (int i = 0; i <= MapSettings.PATH_SAMPLES; i++) {
            double t = (double) i / MapSettings.PATH_SAMPLES;
            Pose p = path.get(t);
            Pose pose = new Pose(p.x(), p.y(), path.heading(t));
            if (!samples.isEmpty()) {
                Pose prev = samples.get(samples.size() - 1);
                length += Math.hypot(pose.x() - prev.x(), pose.y() - prev.y());
            }
            samples.add(pose);
        }
        legs.add(new Leg(number, samples, length));
        Pose mid = samples.get(samples.size() / 2);
        badges.add(new Badge(number, mid.x(), mid.y()));

        JsonObject obj = spec.getAsJsonObject();
        String pathKind = obj.has("line") ? "line" : obj.has("curve") ? "curve" : "through";
        JsonArray names = obj.getAsJsonArray(pathKind);
        StringBuilder describe = new StringBuilder(pathKind).append(' ');
        for (int i = 0; i < names.size(); i++) {
            String wp = names.get(i).getAsString();
            describe.append(i == 0 ? "" : " → ").append(wp);
            boolean control = pathKind.equals("curve") && i > 0 && i < names.size() - 1;
            if (!waypoints.containsKey(wp) || !control) {
                waypoints.put(wp, alliance.pose(route.waypoints.get(wp)));
            }
            if (control) {
                controlPoints.add(wp);
            } else {
                controlPoints.remove(wp);
            }
        }
        current = samples.get(samples.size() - 1);

        Est est = bound(new Est(moveSeconds(length), false), timeout);
        String text = (name != null ? name + ": " : "") + describe + ", " + String.format(Locale.US, "%.0f in", length);
        rows.add(new Row(number, depth, text, timeText(est, timeout)));
        return est;
    }

    private Est leaf(int depth, String text, Est est, Double timeout) {
        int number = ++counter;
        badges.add(new Badge(number, current.x(), current.y()));
        Est bounded = bound(est, timeout);
        rows.add(new Row(number, depth, text, timeText(bounded, timeout)));
        return bounded;
    }

    /** A timeout caps the step; an unknown duration becomes "at most the timeout". */
    private static Est bound(Est est, Double timeout) {
        if (timeout == null) {
            return est;
        }
        if (est.unknown) {
            return new Est(timeout, false);
        }
        return new Est(Math.min(est.seconds, timeout), false);
    }

    /** Trapezoidal motion: accelerate, cruise, decelerate, settle (MapSettings). */
    static double moveSeconds(double length) {
        double v = MapSettings.SPEED;
        double a = MapSettings.ACCEL;
        double rampDistance = v * v / a; // accelerating + decelerating
        double t = length >= rampDistance ? length / v + v / a : 2 * Math.sqrt(length / a);
        return t + MapSettings.SETTLE;
    }

    private static String timeText(Est est, Double timeout) {
        if (est.unknown) {
            return "?";
        }
        String s = "~" + secondsText(est.seconds);
        return timeout != null && est.seconds >= timeout ? "≤ " + secondsText(timeout) : s;
    }

    static String secondsText(double s) {
        return String.format(Locale.US, s < 10 ? "%.1f s" : "%.0f s", s);
    }

    // ---- points --------------------------------------------------------------------------

    private void score() {
        Poly endFoot = foot(end);
        double gap = wallGap(endFoot, BiobuzzField.SIZE);
        boolean leave = gap > 0.25;
        scores.add(new Score("LEAVE", MapSettings.LEAVE_POINTS, leave, leave
                ? String.format(Locale.US, "ends %.0f in from the nearest wall", gap)
                : "still touching the wall at the end"));

        Poly zone = BiobuzzField.loadingZone(alliance);
        boolean park = overlaps(endFoot, zone, -0.1);
        String color = alliance == Alliance.RED ? "red" : "blue";
        scores.add(new Score("PARK", MapSettings.PARK_POINTS, park, park
                ? "ends partly in the " + color + " LOADING ZONE"
                : String.format(Locale.US, "ends outside the %s LOADING ZONE (x %.1f–%.1f, y %.1f–%.1f)",
                        color, zone.minX(), zone.maxX(), zone.minY(), zone.maxY())));

        scores.add(new Score("HIVE TIPS", MapSettings.TIP_POINTS, false, hasActions
                ? "not estimated: launch actions aren't counted yet"
                : "none: the route has no launch actions"));

        for (Score s : scores) {
            if (s.earned) {
                points += s.points;
            }
        }
    }

    // ---- rule checks -----------------------------------------------------------------------

    private void check() {
        Poly startFoot = foot(start);
        double startGap = wallGap(startFoot, BiobuzzField.SIZE);
        if (startGap < -0.25) {
            problems.add(String.format(Locale.US,
                    "START puts the robot %.1f in into the wall (robot %.0f x %.0f in). Move START so the robot just touches the wall (G304).",
                    -startGap, MapSettings.ROBOT_LENGTH, MapSettings.ROBOT_WIDTH));
        } else if (startGap > 0.5) {
            problems.add(String.format(Locale.US, "START is %.1f in from the wall; the robot must touch it (G304).", startGap));
        }
        if (overlaps(startFoot, BiobuzzField.loadingZone(alliance), -0.1)) {
            problems.add("START is in the LOADING ZONE (G304).");
        }
        for (Poly flower : BiobuzzField.FLOWERS) {
            if (overlaps(startFoot, flower, 0)) {
                problems.add("START touches a FLOWER (G304).");
            }
        }
        if (!allOnOwnHalf(startFoot)) {
            problems.add("START is not entirely on our half of the field (G303).");
        }

        for (Leg leg : legs) {
            boolean crossed = false, hive = false, wall = false;
            for (Pose p : leg.samples) {
                Poly f = foot(p);
                crossed |= !allOnOwnHalf(f);
                hive |= overlaps(f, BiobuzzField.HIVE_STRUCTURE, 0);
                wall |= wallGap(f, BiobuzzField.SIZE) < Math.min(startGap, 0) - 0.5;
            }
            if (crossed) {
                problems.add("Step " + leg.number + " crosses into the other alliance's half (G402: risky in AUTO).");
            }
            if (hive) {
                problems.add("Step " + leg.number + " drives into the HIVE structure.");
            }
            if (wall) {
                problems.add("Step " + leg.number + " drives the robot into a wall.");
            }
        }

        if (seconds > MapSettings.AUTO_SECONDS) {
            problems.add("Estimated " + secondsText(seconds) + ", over the 30 s AUTO.");
        }

        if (alliance == Alliance.BLUE) {
            Waypoint red = route.waypoints.get(route.start);
            double dx = start.x() - (BiobuzzField.SIZE - red.x);
            double dy = start.y() - (BiobuzzField.SIZE - red.y);
            if (Math.hypot(dx, dy) > 0.5) {
                problems.add(String.format(Locale.US,
                        "Alliance.BLUE rotates about (72, 72), but the field centre is (%.2f, %.2f): every blue point is %.1f in off in x and y.",
                        BiobuzzField.CENTER, BiobuzzField.CENTER, dx));
            }
        }
    }

    private boolean allOnOwnHalf(Poly p) {
        for (double x : p.x) {
            if (!BiobuzzField.onOwnHalf(alliance, x)) {
                return false;
            }
        }
        return true;
    }

    static Poly foot(Pose pose) {
        return footprint(pose, MapSettings.ROBOT_LENGTH, MapSettings.ROBOT_WIDTH);
    }
}
