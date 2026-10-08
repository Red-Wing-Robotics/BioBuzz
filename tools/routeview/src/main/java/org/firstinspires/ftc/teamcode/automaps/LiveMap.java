package org.firstinspires.ftc.teamcode.automaps;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.route.Route;

import java.util.Map;

/**
 * One alliance's page of docs/autos.pdf, for the browser: the field drawn by {@link FieldPainter}
 * as SVG, plus the {@link AutoRun} numbers (points, time, checks, routine, waypoints) as JSON.
 * Jimothy's only door into the auto-maps code, which is package-private.
 */
public final class LiveMap {

    /** Field size and the margins FieldPainter's labels need (axis ticks, AUDIENCE, alliance areas). */
    private static final double SIDE = 468;
    private static final double LEFT = 40;
    private static final double RIGHT = 40;
    private static final double BOTTOM = 30;
    private static final double TOP = 20;

    private LiveMap() {}

    /** Throws whatever {@link AutoRun} throws for a route it can't walk; check the route first. */
    public static JsonObject report(Route route, Alliance alliance) {
        AutoRun run = new AutoRun(route, alliance);

        SvgCanvas svg = new SvgCanvas(LEFT + SIDE + RIGHT, BOTTOM + SIDE + TOP);
        FieldPainter painter = new FieldPainter(svg, LEFT, BOTTOM, SIDE);
        painter.drawField();
        painter.drawRun(run);

        JsonObject out = new JsonObject();
        out.addProperty("alliance", alliance.name());
        out.addProperty("svg", svg.toSvg());
        out.addProperty("points", run.points);
        out.addProperty("seconds", run.seconds);
        out.addProperty("secondsText", AutoRun.secondsText(run.seconds));
        out.addProperty("timeUnknown", run.timeUnknown);
        out.addProperty("autoSeconds", MapSettings.AUTO_SECONDS);

        JsonArray scores = new JsonArray();
        for (AutoRun.Score s : run.scores) {
            JsonObject o = new JsonObject();
            o.addProperty("label", s.label);
            o.addProperty("points", s.points);
            o.addProperty("earned", s.earned);
            o.addProperty("reason", s.reason);
            scores.add(o);
        }
        out.add("scores", scores);

        JsonArray problems = new JsonArray();
        for (String p : run.problems) {
            problems.add(p);
        }
        out.add("problems", problems);

        JsonArray rows = new JsonArray();
        for (AutoRun.Row r : run.rows) {
            JsonObject o = new JsonObject();
            o.addProperty("number", r.number);
            o.addProperty("depth", r.depth);
            o.addProperty("text", r.text);
            o.addProperty("time", r.time);
            rows.add(o);
        }
        out.add("rows", rows);

        JsonArray waypoints = new JsonArray();
        for (Map.Entry<String, Pose> e : run.waypoints.entrySet()) {
            Pose p = e.getValue();
            JsonObject o = new JsonObject();
            o.addProperty("name", e.getKey());
            o.addProperty("x", p.x());
            o.addProperty("y", p.y());
            o.addProperty("headingDeg", (Math.toDegrees(p.heading()) % 360 + 360) % 360);
            o.addProperty("control", run.controlPoints.contains(e.getKey()));
            waypoints.add(o);
        }
        out.add("waypoints", waypoints);
        return out;
    }
}
