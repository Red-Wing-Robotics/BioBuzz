package org.firstinspires.ftc.teamcode.vision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.junit.Test;

public class CameraMountTest {

    /** The mount measured 2026-10-15 (docs/hardware.md). */
    static final CameraMount MEASURED = new CameraMount(6.75, 6.125, 7.75, 33.3, 0, 54.5, 42, 2);

    private static final double EPS = 1e-9;

    @Test
    public void straightAheadIsHiveVisionsGroundRange() {
        CameraMount centred = new CameraMount(6.75, 0, 7.75, 33.3, 0, 54.5, 42, 2);
        for (double ty : new double[] {-18, -10, 0, 10}) {
            double[] r = centred.toRobot(0, ty, 1.4);
            double range = (7.75 - 1.4) / Math.tan(Math.toRadians(33.3 - ty));
            assertEquals("ty " + ty, 6.75 + range, r[0], 1e-9);
            assertEquals(0, r[1], EPS);
        }
    }

    @Test
    public void rightOfCentreIsToTheRobotsRight() {
        CameraMount centred = new CameraMount(0, 0, 7.75, 33.3, 0, 54.5, 42, 2);
        double[] right = centred.toRobot(10, -5, 1.4);
        double[] left = centred.toRobot(-10, -5, 1.4);
        assertTrue(right[1] < 0);
        assertEquals(-right[1], left[1], EPS);
        assertEquals(right[0], left[0], EPS);
    }

    @Test
    public void aRayAtOrAboveTheHorizonNeverMeetsTheFloor() {
        assertNull(MEASURED.toRobot(0, 33.3, 1.4));
        assertNull(MEASURED.toRobot(0, 40, 1.4));
        assertNull("plane above the lens", MEASURED.toRobot(0, -10, 9));
    }

    @Test
    public void imageAnglesAndRaysRoundTripOverTheVisibleFloor() {
        CameraMount yawed = new CameraMount(6.75, 6.125, 7.75, 33.3, 5, 54.5, 42, 2);
        Pose robot = new Pose(30, 40, 0.7);
        int checked = 0;
        for (double fwd = 0; fwd <= 45; fwd += 3) {
            for (double lft = -20; lft <= 30; lft += 3) {
                double[] field = CameraMount.toField(robot, new double[] {fwd, lft});
                double[] back = CameraMount.toRobotFrame(robot, field[0], field[1]);
                assertEquals(fwd, back[0], 1e-9);
                assertEquals(lft, back[1], 1e-9);

                double[] angles = yawed.toImage(fwd, lft, 1.8);
                if (angles == null || yawed.nearEdge(angles[0], angles[1])) {
                    continue;
                }
                double[] seen = yawed.toRobot(angles[0], angles[1], 1.8);
                assertNotNull(seen);
                assertEquals(fwd, seen[0], 1e-6);
                assertEquals(lft, seen[1], 1e-6);
                checked++;
            }
        }
        assertTrue("some of the grid is in view: " + checked, checked > 20);
    }

    @Test
    public void theMeasuredMountSeesBallsFromAboutFourAndAHalfToTwentyNineInchesOut() {
        // docs/hardware.md: ball centres from about 4.5 in to 29 in ahead of the lens.
        double near = MEASURED.toRobot(0, -21, 1.4)[0] - MEASURED.forward;
        double far = MEASURED.toRobot(0, 21, 1.4)[0] - MEASURED.forward;
        assertEquals(4.6, near, 0.1);
        assertEquals(29.1, far, 0.1);
    }

    @Test
    public void oneBallsBoxShrinksWithTheSquareOfItsDepth() {
        // 20 in ahead of the lens, in line with it: depth along the tilted axis is
        // 20 cos p + (lens height − ball centre) sin p.
        double p = Math.toRadians(33.3);
        double depth = 20 * Math.cos(p) + (7.75 - 1.4) * Math.sin(p);
        double r = 1.4 / depth;
        double expected = 100 * r * r / (Math.tan(Math.toRadians(27.25)) * Math.tan(Math.toRadians(21)));
        assertEquals(expected, MEASURED.ballAreaPct(6.75 + 20, 6.125, BallKind.POLLEN), 1e-9);
        assertTrue(MEASURED.ballAreaPct(6.75 + 10, 6.125, BallKind.POLLEN) > 2.5 * expected);
        assertTrue(Double.isInfinite(MEASURED.ballAreaPct(0, 6.125, BallKind.POLLEN)));
    }

    @Test
    public void nearTheEdgeIsNotUsed() {
        assertFalse(MEASURED.nearEdge(0, 0));
        assertTrue(MEASURED.nearEdge(26, 0));
        assertTrue(MEASURED.nearEdge(0, -20));
        assertFalse(MEASURED.nearEdge(-25, 18.9));
    }

    @Test
    public void robotFrameTurnsWithTheHeading() {
        double[] p = CameraMount.toField(new Pose(10, 20, Math.PI / 2), new double[] {5, 0});
        assertEquals(10, p[0], EPS);
        assertEquals(25, p[1], EPS);
    }
}
