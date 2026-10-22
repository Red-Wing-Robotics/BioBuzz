package org.firstinspires.ftc.teamcode.vision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.pedropathing.math.Pose;

import org.junit.Test;

public class PoseHistoryTest {

    private static final double EPS = 1e-9;

    @Test
    public void interpolatesBetweenSamples() {
        PoseHistory h = new PoseHistory(1000, 16);
        h.add(0, new Pose(0, 0, 0));
        h.add(100, new Pose(10, 20, 1.0));
        Pose mid = h.at(25);
        assertEquals(2.5, mid.x(), EPS);
        assertEquals(5, mid.y(), EPS);
        assertEquals(0.25, mid.heading(), EPS);
    }

    @Test
    public void headingTakesTheShortWayRoundPastPi() {
        PoseHistory h = new PoseHistory(1000, 16);
        h.add(0, new Pose(0, 0, 3.0));
        h.add(100, new Pose(0, 0, -3.0));
        double mid = h.at(50).heading();
        assertEquals(0, Math.atan2(Math.sin(mid - Math.PI), Math.cos(mid - Math.PI)), 1e-9);
        assertEquals((2 * Math.PI - 6.0) / 0.1, h.turnRate(50), 1e-6);
    }

    @Test
    public void outsideTheSpanGivesTheNearestEnd() {
        PoseHistory h = new PoseHistory(1000, 16);
        assertNull(h.at(0));
        h.add(100, new Pose(1, 1, 0));
        h.add(200, new Pose(2, 2, 0));
        assertEquals(1, h.at(0).x(), EPS);
        assertEquals(2, h.at(999).x(), EPS);
        assertEquals(0, new PoseHistory(1000, 16).turnRate(0), EPS);
    }

    @Test
    public void dropsPosesOlderThanTheSpanAndOverCapacity() {
        PoseHistory h = new PoseHistory(100, 4);
        for (int t = 0; t <= 1000; t += 10) {
            h.add(t, new Pose(t, 0, 0));
        }
        assertEquals("capacity 4: the oldest kept is t = 970", 970, h.at(0).x(), EPS);
        PoseHistory span = new PoseHistory(100, 64);
        for (int t = 0; t <= 1000; t += 10) {
            span.add(t, new Pose(t, 0, 0));
        }
        assertEquals("the last pose before the span's start is kept, for interpolating", 890, span.at(0).x(), EPS);
    }

    @Test
    public void ignoresTimesThatGoBackwards() {
        PoseHistory h = new PoseHistory(1000, 16);
        h.add(100, new Pose(1, 0, 0));
        h.add(50, new Pose(9, 0, 0));
        assertEquals(1, h.at(100).x(), EPS);
    }
}
