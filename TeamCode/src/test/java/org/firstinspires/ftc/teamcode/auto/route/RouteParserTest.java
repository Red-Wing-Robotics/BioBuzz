package org.firstinspires.ftc.teamcode.auto.route;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RouteParserTest {

    static String asset(String name) throws IOException {
        return new String(Files.readAllBytes(Paths.get("src/main/assets/autos/" + name + ".json")), StandardCharsets.UTF_8);
    }

    @Test
    public void parsesTheShippedRoute() throws IOException {
        Route route = RouteParser.parse(asset("leave_and_park"));
        assertEquals("Leave + Park", route.name);
        assertEquals("START", route.start);
        assertEquals(3, route.waypoints.size());
        assertEquals(60, route.waypoints.get("START").y, 1e-9);
        assertEquals(3, route.routine.size());
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
