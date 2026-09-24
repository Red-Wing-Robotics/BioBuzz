package org.firstinspires.ftc.teamcode.auto.route;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.JsonParser;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.AutoContext;
import org.firstinspires.ftc.teamcode.auto.Step;
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
    public void shippedRouteCompilesForBothAlliances() throws IOException {
        Route route = RouteParser.parse(RouteParserTest.asset("leave_and_park"));
        for (Alliance alliance : Alliance.values()) {
            RouteCompiler c = new RouteCompiler(alliance, route.waypoints, new ActionRegistry());
            Step step = c.compile(route);
            assertEquals("leave wall", step.name());               // first step of the sequence
            Pose start = c.startPose(route);
            assertEquals(alliance == Alliance.RED ? 8 : 136, start.x(), EPS);
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
    }
}
