package org.firstinspires.ftc.teamcode.auto;

import static org.junit.Assert.assertEquals;

import com.pedropathing.math.Pose;

import org.junit.Test;

public class AllianceTest {

    private static final double EPS = 1e-9;

    private static double degrees(Pose p) {
        return ((Math.toDegrees(p.heading()) % 360) + 360) % 360;
    }

    @Test
    public void redIsIdentity() {
        Pose p = Alliance.RED.pose(new Waypoint(10, 20, 30));
        assertEquals(10, p.x(), EPS);
        assertEquals(20, p.y(), EPS);
        assertEquals(30, degrees(p), EPS);
    }

    @Test
    public void blueRotates180AboutFieldCentre() {
        Pose p = Alliance.BLUE.pose(new Waypoint(10, 20, 30));
        assertEquals(134, p.x(), EPS);
        assertEquals(124, p.y(), EPS);
        assertEquals(210, degrees(p), EPS);
    }

    @Test
    public void fieldCentreIsFixedForBothAlliances() {
        Pose p = Alliance.BLUE.pose(new Waypoint(Field.CENTER, Field.CENTER, 0));
        assertEquals(72, Field.CENTER, EPS); // Pedro's documented centre: docs/coordinates.md
        assertEquals(Field.CENTER, p.x(), EPS);
        assertEquals(Field.CENTER, p.y(), EPS);
    }

    @Test
    public void blueOriginIsTheOppositeCorner() {
        Pose p = Alliance.BLUE.pose(new Waypoint(0, 0, 0));
        assertEquals(Field.SIZE, p.x(), EPS);
        assertEquals(Field.SIZE, p.y(), EPS);
        assertEquals(180, degrees(p), EPS);
    }
}
