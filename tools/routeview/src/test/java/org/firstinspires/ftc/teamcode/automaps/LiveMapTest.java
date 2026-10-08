package org.firstinspires.ftc.teamcode.automaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.Field;
import org.firstinspires.ftc.teamcode.auto.route.Route;
import org.firstinspires.ftc.teamcode.auto.route.RouteParser;
import org.junit.Test;

/** Jimothy's map uses the coordinates docs/coordinates.md defines, through TeamCode's own classes. */
public class LiveMapTest {

    private static final Route ROUTE = RouteParser.parse("{ \"name\": \"Test\","
            + " \"waypoints\": { \"START\": [10, 20, 30], \"END\": [40, 20, 30] },"
            + " \"start\": \"START\","
            + " \"routine\": [ { \"follow\": { \"line\": [\"START\", \"END\"] } } ] }");

    private static JsonObject start(Alliance alliance) {
        return LiveMap.report(ROUTE, alliance).getAsJsonArray("waypoints").get(0).getAsJsonObject();
    }

    @Test
    public void redListsWaypointsAsTheRouteFileWritesThem() {
        JsonObject w = start(Alliance.RED);
        assertEquals("START", w.get("name").getAsString());
        assertEquals(10, w.get("x").getAsDouble(), 1e-9);
        assertEquals(20, w.get("y").getAsDouble(), 1e-9);
        assertEquals(30, w.get("headingDeg").getAsDouble(), 1e-9);
    }

    @Test
    public void blueListsThemTurned180AboutTheFieldCentre() {
        JsonObject w = start(Alliance.BLUE);
        assertEquals(Field.SIZE - 10, w.get("x").getAsDouble(), 1e-9);
        assertEquals(Field.SIZE - 20, w.get("y").getAsDouble(), 1e-9);
        assertEquals(210, w.get("headingDeg").getAsDouble(), 1e-9);
    }

    @Test
    public void theFieldIsDrawnToFieldSize() {
        String svg = LiveMap.report(ROUTE, Alliance.RED).get("svg").getAsString();
        assertTrue("the last axis tick is the far wall", svg.contains(">" + (int) Field.SIZE + " in<"));
    }
}
