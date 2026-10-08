package org.firstinspires.ftc.teamcode.auto.route;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

public class RouteParserTest {

    /**
     * Every route file in assets/autos, by name. Tests on them check properties, never
     * coordinates, so tuning a route doesn't break the build.
     */
    static Map<String, String> shippedRoutes() throws IOException {
        File dir = new File("src/main/assets/autos");
        File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
        assertTrue("no route files in " + dir.getAbsolutePath(), files != null && files.length > 0);
        Map<String, String> routes = new TreeMap<>();
        for (File f : files) {
            routes.put(f.getName(), new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        }
        return routes;
    }

    @Test
    public void parsesEveryShippedRoute() throws IOException {
        for (Map.Entry<String, String> file : shippedRoutes().entrySet()) {
            Route route = RouteParser.parse(file.getValue());
            assertTrue(file.getKey() + ": start waypoint", route.waypoints.containsKey(route.start));
            assertTrue(file.getKey() + ": routine", route.routine.size() > 0);
        }
    }

    private static void assertRejected(String json, String messagePart) {
        try {
            RouteParser.parse(json);
            fail("expected rejection mentioning: " + messagePart);
        } catch (RouteException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(messagePart));
        }
    }

    @Test
    public void rejectsBadFiles() {
        assertRejected("not json", "invalid JSON");
        assertRejected("{\"waypoints\":{\"A\":[0,0,0]},\"start\":\"B\",\"routine\":[]}", "start waypoint \"B\"");
        assertRejected("{\"waypoints\":{\"A\":[0,0]},\"start\":\"A\",\"routine\":[]}", "[x, y, headingDeg]");
        assertRejected("{\"waypoints\":{\"A\":[0,0,0]},\"start\":\"A\"}", "\"routine\"");
        assertRejected("{\"waypoints\":{\"A\":[0,0,0]},\"start\":\"A\",\"routine\":[],\"typo\":1}", "unknown key \"typo\"");
    }
}
