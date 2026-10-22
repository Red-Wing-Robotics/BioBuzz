package org.firstinspires.ftc.teamcode.auto.route;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.AutoContext;
import org.firstinspires.ftc.teamcode.auto.Step;
import org.firstinspires.ftc.teamcode.auto.Steps;
import org.firstinspires.ftc.teamcode.auto.Waypoint;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class RouteCompilerTest {

    private static final double EPS = 1e-9;

    private static Map<String, Waypoint> waypoints() {
        Map<String, Waypoint> w = new LinkedHashMap<>();
        w.put("A", new Waypoint(10, 20, 0));
        w.put("B", new Waypoint(50, 20, 90));
        w.put("C", new Waypoint(50, 60, 90));
        return w;
    }

    private static RouteCompiler compiler(Alliance alliance, ActionRegistry registry) {
        return new RouteCompiler(alliance, waypoints(), registry);
    }

    private static Path path(Alliance alliance, String json) {
        return compiler(alliance, new ActionRegistry()).compilePath(new JsonParser().parse(json), "test");
    }

    private static double degrees(double radians) {
        return ((Math.toDegrees(radians) % 360) + 360) % 360;
    }

    @Test
    public void everyShippedRouteCompilesForBothAlliances() throws IOException {
        // How BLUE maps from RED is AllianceTest's job; this checks each route's references.
        for (Map.Entry<String, String> file : RouteParserTest.shippedRoutes().entrySet()) {
            Route route = RouteParser.parse(file.getValue());
            ActionRegistry anyName = new ActionRegistry();
            registerEveryName(route.routine, anyName);
            for (Alliance alliance : Alliance.values()) {
                RouteCompiler compiler = new RouteCompiler(alliance, route.waypoints, anyName);
                compiler.startPose(route);
                compiler.compile(route);
            }
        }
    }

    /** Actions and conditions are registered by OpModes on the robot; here any name compiles. */
    private static void registerEveryName(JsonElement e, ActionRegistry registry) {
        if (e.isJsonArray()) {
            for (JsonElement child : e.getAsJsonArray()) {
                registerEveryName(child, registry);
            }
        } else if (e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : e.getAsJsonObject().entrySet()) {
                JsonElement v = entry.getValue();
                boolean name = v.isJsonPrimitive() && v.getAsJsonPrimitive().isString();
                if (name && entry.getKey().equals("action")) {
                    registry.action(v.getAsString(), args -> Steps.instant(v.getAsString(), () -> {}));
                } else if (name && entry.getKey().equals("waitUntil")) {
                    registry.condition(v.getAsString(), () -> true);
                } else if (name && entry.getKey().equals("seek")) {
                    registry.seek(v.getAsString(), args -> Steps.instant(v.getAsString(), () -> {}));
                } else {
                    registerEveryName(v, registry);
                }
            }
        }
    }

    @Test
    public void linePathEndsAtTheAllianceFrameWaypoint() {
        Path red = path(Alliance.RED, "{\"line\":[\"A\",\"B\"]}");
        assertEquals(50, red.endPose().x(), EPS);
        assertEquals(20, red.endPose().y(), EPS);
        assertEquals(90, degrees(red.endPose().heading()), 1e-6);

        Path blue = path(Alliance.BLUE, "{\"line\":[\"A\",\"B\"]}");
        assertEquals(94, blue.endPose().x(), EPS);
        assertEquals(124, blue.endPose().y(), EPS);
        assertEquals(270, degrees(blue.endPose().heading()), 1e-6);
    }

    @Test
    public void constantHeadingIsMappedIntoTheAllianceFrame() {
        Path red = path(Alliance.RED, "{\"line\":[\"A\",\"B\"],\"heading\":{\"constant\":45}}");
        assertEquals(45, degrees(red.heading(0.5)), 1e-6);
        Path blue = path(Alliance.BLUE, "{\"line\":[\"A\",\"B\"],\"heading\":{\"constant\":45}}");
        assertEquals(225, degrees(blue.heading(0.5)), 1e-6);
    }

    @Test
    public void curveAndThroughAcceptSeveralWaypoints() {
        assertEquals(60, path(Alliance.RED, "{\"curve\":[\"A\",\"B\",\"C\"]}").endPose().y(), EPS);
        assertEquals(60, path(Alliance.RED, "{\"through\":[\"A\",\"B\",\"C\"],\"heading\":\"tangent\"}").endPose().y(), EPS);
    }

    @Test
    public void actionsResolveThroughTheRegistryWithArgs() {
        int[] received = {0};
        ActionRegistry registry = new ActionRegistry()
                .action("shooter.fire", args -> {
                    received[0] = args.get("count").getAsInt();
                    return ctx -> true;
                });
        Step step = compiler(Alliance.RED, registry)
                .compileStep(new JsonParser().parse("{\"action\":\"shooter.fire\",\"args\":{\"count\":3},\"timeout\":2}"), "t");
        assertEquals(3, received[0]);
        assertTrue(step.name().contains("shooter.fire"));
        assertTrue(step.name().contains("2.0s"));
        AutoContext ctx = new AutoContext(null, null, () -> 0);
        step.start(ctx);
        assertTrue(step.update(ctx));
    }

    private static void assertRejected(String stepJson, String messagePart) {
        try {
            compiler(Alliance.RED, new ActionRegistry()).compileStep(new JsonParser().parse(stepJson), "routine[0]");
            fail("expected rejection mentioning: " + messagePart);
        } catch (RouteException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(messagePart));
        }
    }

    @Test
    public void rejectsBadSteps() {
        assertRejected("{\"follow\":{\"line\":[\"A\",\"Z\"]}}", "unknown waypoint \"Z\"");
        assertRejected("{\"follow\":{\"line\":[\"A\"]}}", "exactly 2 waypoints");
        assertRejected("{\"action\":\"nope\"}", "unknown action \"nope\"");
        assertRejected("{\"waitUntil\":\"nope\"}", "unknown condition \"nope\"");
        assertRejected("{\"hold\":\"A\"}", "\"timeout\"");
        assertRejected("{\"wait\":1,\"follow\":{\"line\":[\"A\",\"B\"]}}", "one kind per step");
        assertRejected("{\"waitt\":1}", "unknown key \"waitt\"");
        assertRejected("{\"follow\":{\"line\":[\"A\",\"B\"],\"heading\":\"sideways\"}}", "unknown heading");
        assertRejected("{\"wait\":-1}", "negative");
        assertRejected("{\"wait\":1,\"args\":{}}", "\"args\" goes only with \"action\"");
    }

    @Test
    public void seekNeedsATimeoutAndARegisteredName() {
        assertRejected("{\"seek\":\"vision.ball\"}", "\"timeout\"");
        assertRejected("{\"seek\":\"nope\",\"timeout\":2}", "unknown seek \"nope\"");
    }

    @Test
    public void seekGetsItsArgsAndBadArgsSayWhichStep() {
        ActionRegistry registry = new ActionRegistry().seek("vision.ball", args -> {
            if (args.has("bad")) {
                throw new RouteException("args: unknown key \"bad\"");
            }
            return Steps.instant("ball", () -> {});
        });
        Step step = compiler(Alliance.RED, registry)
                .compileStep(new JsonParser().parse("{\"seek\":\"vision.ball\",\"args\":{\"within\":20},\"timeout\":3}"), "routine[0]");
        assertTrue(step.name(), step.name().contains("seek vision.ball"));
        try {
            compiler(Alliance.RED, registry)
                    .compileStep(new JsonParser().parse("{\"seek\":\"vision.ball\",\"args\":{\"bad\":1},\"timeout\":3}"), "routine[4]");
            fail("bad args accepted");
        } catch (RouteException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("routine[4].seek: args: unknown key"));
        }
    }

    @Test
    public void driveToGoesToAKnownWaypoint() {
        assertRejected("{\"driveTo\":\"Z\"}", "unknown waypoint \"Z\"");
        Step step = compiler(Alliance.RED, new ActionRegistry()).compileStep(new JsonParser().parse("{\"driveTo\":\"B\"}"), "t");
        assertEquals("drive to B", step.name());
    }

    @Test
    public void badHeadingValuesSayWhereTheyAre() {
        assertRejected("{\"follow\":{\"line\":[\"A\",\"B\"],\"heading\":{\"constant\":\"90deg\"}}}",
                "routine[0].follow.heading.constant: must be a number");
        assertRejected("{\"follow\":{\"line\":[\"A\",\"B\"],\"heading\":{\"linear\":90}}}",
                "routine[0].follow.heading.linear: needs [fromDeg, toDeg]");
        assertRejected("{\"follow\":{\"line\":[\"A\",\"B\"],\"heading\":{\"linear\":[0,\"x\"]}}}",
                "routine[0].follow.heading.linear[1]: must be a number");
        assertRejected("{\"follow\":{\"line\":[\"A\",\"B\"],\"heading\":{\"facing\":3}}}",
                "routine[0].follow.heading.facing: must be a string");
    }
}
