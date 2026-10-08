package org.firstinspires.ftc.teamcode.auto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class KeepOutTest {

    private static final KeepOut BOX = new KeepOut(40, 40, 100, 100);
    private static final List<KeepOut> ZONES = Collections.singletonList(BOX);

    @Test
    public void blocksOnlyLinesThroughTheInside() {
        assertTrue(BOX.blocks(70, 0, 70, 140));      // straight through
        assertTrue(BOX.blocks(0, 0, 140, 140));      // diagonally through
        assertTrue(BOX.blocks(70, 70, 70, 140));     // starts inside
        assertFalse(BOX.blocks(0, 0, 30, 140));      // beside it
        assertFalse(BOX.blocks(40, 0, 40, 140));     // along an edge
        assertFalse(BOX.blocks(0, 80, 80, 0));       // touches a corner only
        assertFalse(BOX.blocks(0, 120, 10, 130));    // nowhere near
    }

    @Test
    public void clearLineIsDirect() {
        List<double[]> way = KeepOut.route(20, 10, 20, 130, ZONES);
        assertEquals(2, way.size());
        assertArrayEquals(new double[] {20, 10}, way.get(0), 0);
        assertArrayEquals(new double[] {20, 130}, way.get(1), 0);
    }

    @Test
    public void goesRoundTheShorterSideByTheCorners() {
        // From below to above, nearer the left edge: round the left side.
        List<double[]> way = KeepOut.route(50, 20, 50, 120, ZONES);
        assertEquals(4, way.size());
        assertArrayEquals(new double[] {40, 40}, way.get(1), 0.02);
        assertArrayEquals(new double[] {40, 100}, way.get(2), 0.02);
        assertLegsClear(way);
    }

    @Test
    public void oneCornerIsEnoughWhenItIs() {
        List<double[]> way = KeepOut.route(20, 70, 70, 120, ZONES);
        assertEquals(3, way.size());
        assertArrayEquals(new double[] {40, 100}, way.get(1), 0.02);
        assertLegsClear(way);
    }

    @Test
    public void startingInsideBacksStraightOutFirst() {
        List<double[]> way = KeepOut.route(70, 95, 70, 20, ZONES);
        assertArrayEquals(new double[] {70, 100}, way.get(1), 0.02); // top edge is nearest
        assertLegsClear(way.subList(1, way.size()));
    }

    @Test
    public void endingInsideComesStraightInFromTheNearestEdge() {
        List<double[]> way = KeepOut.route(70, 20, 95, 70, ZONES);
        double[] last = way.get(way.size() - 1);
        double[] beforeLast = way.get(way.size() - 2);
        assertArrayEquals(new double[] {95, 70}, last, 0);
        assertArrayEquals(new double[] {100, 70}, beforeLast, 0.02); // right edge is nearest
        assertLegsClear(way.subList(0, way.size() - 1));
    }

    @Test
    public void growWidensEverySide() {
        KeepOut grown = BOX.grow(5);
        assertEquals(35, grown.minX, 0);
        assertEquals(35, grown.minY, 0);
        assertEquals(105, grown.maxX, 0);
        assertEquals(105, grown.maxY, 0);
    }

    private static void assertLegsClear(List<double[]> way) {
        for (int i = 0; i + 1 < way.size(); i++) {
            double[] a = way.get(i);
            double[] b = way.get(i + 1);
            assertFalse("leg " + i + " crosses the zone", BOX.blocks(a[0], a[1], b[0], b[1]));
        }
    }
}
