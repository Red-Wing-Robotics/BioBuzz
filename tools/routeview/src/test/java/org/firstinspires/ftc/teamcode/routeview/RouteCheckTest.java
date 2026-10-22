package org.firstinspires.ftc.teamcode.routeview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;

/** Tests run in the repo root (build.gradle), so the shipped routes are at their usual path. */
public class RouteCheckTest {

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private static final RouteCheck.Registrations NONE =
            new RouteCheck.Registrations(Collections.<String>emptySet(), Collections.<String>emptySet(), Collections.<String>emptySet());

    private static String route(String routine) {
        return "{ \"waypoints\": { \"START\": [9, 72, 0], \"OUT\": [24, 72, 0] }, \"start\": \"START\","
                + " \"routine\": [" + routine + "] }";
    }

    @Test
    public void everyShippedRouteIsValidAndDrawsForBothAlliances() throws IOException {
        File[] files = new File(RouteView.AUTOS).listFiles((d, n) -> n.endsWith(".json"));
        assertTrue("no routes in " + new File(RouteView.AUTOS).getAbsolutePath(), files != null && files.length > 0);
        for (File f : files) {
            RouteCheck.Result r = RouteCheck.check(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8), NONE);
            assertNull(f.getName() + ": " + r.error, r.error);
            for (JsonObject report : Arrays.asList(r.red, r.blue)) {
                assertTrue(report.get("svg").getAsString().startsWith("<svg"));
                assertTrue(report.getAsJsonArray("rows").size() > 0);
            }
            assertEquals("RED", r.red.get("alliance").getAsString());
            assertEquals("BLUE", r.blue.get("alliance").getAsString());
        }
    }

    @Test
    public void reportsTheSameErrorTheRobotWouldAtInit() {
        RouteCheck.Result r = RouteCheck.check(route("{ \"follow\": { \"line\": [\"START\", \"NOWHERE\"] } }"), NONE);
        assertFalse(r.valid());
        assertTrue(r.error, r.error.startsWith("routine[0].follow.line[1]: unknown waypoint \"NOWHERE\""));
    }

    @Test
    public void reportsBrokenJson() {
        RouteCheck.Result r = RouteCheck.check("{ \"waypoints\": { ", NONE);
        assertFalse(r.valid());
        assertTrue(r.error, r.error.startsWith("route: invalid JSON"));
    }

    @Test
    public void reportsAWrongJsonTypeInsteadOfThrowing() {
        RouteCheck.Result r = RouteCheck.check(route(
                "{ \"follow\": { \"line\": [\"START\", \"OUT\"], \"heading\": { \"linear\": 90 } } }"), NONE);
        assertFalse(r.valid());
    }

    @Test
    public void unregisteredActionsAreWarningsNotErrors() {
        String json = route("{ \"action\": \"shooter.fire\" }, { \"waitUntil\": \"intake.full\" },"
                + " { \"parallel\": [ { \"action\": \"intake.run\" } ] },"
                + " { \"seek\": \"vision.ball\", \"args\": { \"colors\": \"pollen\" }, \"timeout\": 3 }, { \"driveTo\": \"OUT\" }");
        RouteCheck.Result r = RouteCheck.check(json, NONE);
        assertTrue(r.error, r.valid());
        assertEquals(4, r.warnings.size());

        RouteCheck.Registrations some = new RouteCheck.Registrations(
                new TreeSet<>(Arrays.asList("shooter.fire", "intake.run")), new TreeSet<>(Collections.singletonList("intake.full")),
                new TreeSet<>(Collections.singletonList("vision.ball")));
        assertTrue(RouteCheck.check(json, some).warnings.isEmpty());
    }

    @Test
    public void findsRegisteredNamesInTheSources() throws IOException {
        File src = tmp.newFolder("java");
        Files.write(new File(src, "Auto.java").toPath(), ("registry.action(\"shooter.fire\", args -> shooter.fire())\n"
                + "    .condition( \"intake.full\", intake::full)\n"
                + "    .seek(\"vision.ball\", vision::toBall);\n").getBytes(StandardCharsets.UTF_8));
        RouteCheck.Registrations found = RouteCheck.Registrations.scan(src);
        assertEquals(Collections.singleton("shooter.fire"), found.actions);
        assertEquals(Collections.singleton("intake.full"), found.conditions);
        assertEquals(Collections.singleton("vision.ball"), found.seeks);
    }
}
