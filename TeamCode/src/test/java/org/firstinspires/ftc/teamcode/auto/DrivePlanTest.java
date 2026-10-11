package org.firstinspires.ftc.teamcode.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.opmodes.teleop.HiveClearance;
import org.firstinspires.ftc.teamcode.opmodes.teleop.ShootingSpots;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * The plans CompTeleOp's driving assists follow, checked against the HIVE's floor rails with the
 * real spot and clearance values: the robot's whole footprint, at the heading the plan gives it,
 * at every point of every part.
 */
public class DrivePlanTest {

    private static final double MARGIN = HiveClearance.margin;
    /** Sampling and the route's 0.01 in edge gap. */
    private static final double TOLERANCE = 0.05;
    private static final int SAMPLES = 300;

    @Test
    public void spotToSpotKeepsClearForBothAlliances() {
        for (Alliance alliance : Alliance.values()) {
            Pose left = alliance.pose(ShootingSpots.left);
            Pose right = alliance.pose(ShootingSpots.right);
            assertKeepsClear(alliance + " left → right", left, right, MARGIN);
            assertKeepsClear(alliance + " right → left", right, left, MARGIN);
        }
    }

    @Test
    public void fromAnywhereToAnySpotKeepsClear() {
        List<Pose> spots = new ArrayList<>();
        for (Alliance alliance : Alliance.values()) {
            spots.add(alliance.pose(ShootingSpots.left));
            spots.add(alliance.pose(ShootingSpots.right));
        }
        for (int x = 12; x <= 132; x += 12) {
            for (int y = 12; y <= 132; y += 12) {
                if (nearARail(x, y, MARGIN)) {
                    continue; // startingBesideARailNeverTouchesIt covers these
                }
                for (int heading : new int[] {0, 45}) {
                    Pose from = Alliance.RED.poses().of(x, y, heading);
                    for (Pose spot : spots) {
                        assertKeepsClear(String.format("(%d, %d, %d°) → (%.0f, %.0f)", x, y, heading, spot.x(), spot.y()),
                                from, spot, MARGIN);
                    }
                }
            }
        }
    }

    @Test
    public void startingBesideARailNeverTouchesIt() {
        // Turning on the spot beside a rail leaves half the robot's width plus the margin, less its reach.
        double turnGap = RobotSize.WIDTH / 2 + MARGIN - RobotSize.REACH;
        Pose[] starts = {
                Alliance.RED.poses().of(66, 70, 30),   // in the lane, 4.4 in off the red rail
                Alliance.RED.poses().of(62, 60, 0),    // in the lane, square
                Alliance.RED.poses().of(36, 72, 90),   // outside the red rail
                Alliance.RED.poses().of(48, 36, 45),   // off the rail's audience end, a corner 3.3 in from it
        };
        for (Pose from : starts) {
            double startGap = gapToRails(from.x(), from.y(), from.heading());
            assertTrue("test start " + from + " touches a rail", startGap > 0);
            for (Waypoint spot : new Waypoint[] {ShootingSpots.left, ShootingSpots.right}) {
                Pose to = Alliance.RED.pose(spot);
                for (double[] p : poses(DrivePlan.plan(from, to, Field.HIVE_FLOOR, MARGIN), from)) {
                    double gap = gapToRails(p[0], p[1], p[2]);
                    assertTrue(String.format("from %s to %s: %.2f in from a rail at (%.1f, %.1f, %.0f°)",
                                    from, spot, gap, p[0], p[1], Math.toDegrees(p[2])),
                            gap >= Math.min(startGap, turnGap) - TOLERANCE);
                }
            }
        }
    }

    @Test
    public void takesTheLaneUnderTheHiveWhileItIsWideEnough() {
        Pose from = Alliance.RED.poses().of(72, 20, 90);
        Pose to = Alliance.RED.poses().of(72, 124, 90);
        DrivePlan under = DrivePlan.plan(from, to, Field.HIVE_FLOOR, MARGIN);
        assertEquals("straight up the lane", 2, under.points.size());
        assertKeepsClear("up the lane", from, to, MARGIN);

        // HiveClearance: for an 18 in wide robot the lane closes above 13.6.
        DrivePlan round = DrivePlan.plan(from, to, Field.HIVE_FLOOR, 14);
        assertTrue("goes round once the lane is closed", round.points.size() > 2);
        assertKeepsClear("round the HIVE", from, to, 14);
        assertTrue(under.seconds < round.seconds);
    }

    @Test
    public void clearLineIsOnePathTurningAsItDrives() {
        Pose from = Alliance.RED.poses().of(12, 20, 0);
        Pose to = Alliance.RED.poses().of(12, 120, 90);
        DrivePlan plan = DrivePlan.plan(from, to, Field.HIVE_FLOOR, MARGIN);
        assertEquals(1, plan.parts.size());
        Path path = plan.parts.get(0).path;
        assertEquals(12, path.get(0.5).x(), 1e-6);
        assertEquals(70, path.get(0.5).y(), 1e-6);
        assertSameAngle(0, path.heading(0), 1e-6);
        assertSameAngle(Math.PI / 2, path.heading(1), 1e-6);
    }

    @Test
    public void alreadyThereOnlyTurns() {
        Pose from = Alliance.RED.poses().of(20, 20, 0);
        assertTrue(DrivePlan.plan(from, Alliance.RED.poses().of(20.5, 20, 0), Field.HIVE_FLOOR, MARGIN).parts.isEmpty());

        List<DrivePlan.Part> parts = DrivePlan.plan(from, Alliance.RED.poses().of(20, 20, 90), Field.HIVE_FLOOR, MARGIN).parts;
        assertEquals(1, parts.size());
        assertTrue(parts.get(0).isTurn());
        assertSameAngle(Math.PI / 2, parts.get(0).heading, 1e-9);
    }

    @Test
    public void cornerCostsTheTimeToSlowForIt() {
        assertEquals(0, DrivePlan.cornerSeconds(0), 1e-9);
        assertEquals(DrivePlan.SPEED / DrivePlan.ACCEL, DrivePlan.cornerSeconds(Math.PI / 2), 1e-9);
    }

    @Test
    public void redLeftIsTheDriversLeft() {
        // docs/coordinates.md: red drivers face +x, so their left is +y (the far wall).
        assertTrue(ShootingSpots.left.y > ShootingSpots.right.y);
    }

    // ---- helpers ----------------------------------------------------------------------------

    /**
     * The whole footprint stays {@code margin} from every rail; beside a rail the heading is
     * square to the field; and the plan ends at {@code to}.
     */
    private static void assertKeepsClear(String what, Pose from, Pose to, double margin) {
        List<double[]> poses = poses(DrivePlan.plan(from, to, Field.HIVE_FLOOR, margin), from);
        for (double[] p : poses) {
            double gap = gapToRails(p[0], p[1], p[2]);
            assertTrue(String.format("%s: %.2f in from a rail at (%.1f, %.1f, %.0f°)", what, gap, p[0], p[1], Math.toDegrees(p[2])),
                    gap >= margin - TOLERANCE);
            if (nearARail(p[0], p[1], margin - TOLERANCE)) {
                assertSameAngle(what + " beside a rail at (" + p[0] + ", " + p[1] + ")", DrivePlan.square(p[2]), p[2], 1e-6);
            }
        }
        double[] end = poses.get(poses.size() - 1);
        assertEquals(what + ": ends at x", to.x(), end[0], 1e-6);
        assertEquals(what + ": ends at y", to.y(), end[1], 1e-6);
        assertSameAngle(what + ": ends at the heading", to.heading(), end[2], DrivePlan.TURNED);
    }

    /** Close enough to a rail that the plan must hold a square heading there. */
    private static boolean nearARail(double x, double y, double margin) {
        for (KeepOut rail : Field.HIVE_FLOOR) {
            if (rail.grow(RobotSize.REACH + margin).contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every pose the plan puts the robot in, as {x, y, heading}: along each path, and swept
     * through each turn on the spot from the heading before it.
     */
    private static List<double[]> poses(DrivePlan plan, Pose from) {
        List<double[]> out = new ArrayList<>();
        out.add(new double[] {from.x(), from.y(), from.heading()});
        for (DrivePlan.Part part : plan.parts) {
            double[] before = out.get(out.size() - 1);
            if (part.isTurn()) {
                double turn = DrivePlan.wrap(part.heading - before[2]);
                for (int i = 0; i <= SAMPLES; i++) {
                    out.add(new double[] {part.x, part.y, before[2] + turn * i / SAMPLES});
                }
            } else {
                for (int i = 0; i <= SAMPLES; i++) {
                    double t = (double) i / SAMPLES;
                    Pose p = part.path.get(t);
                    out.add(new double[] {p.x(), p.y(), part.path.heading(t)});
                }
            }
        }
        return out;
    }

    /** Least distance from the robot's footprint at this pose to any rail; 0 if it touches one. */
    private static double gapToRails(double x, double y, double heading) {
        double least = Double.POSITIVE_INFINITY;
        for (KeepOut rail : Field.HIVE_FLOOR) {
            if (distance(x, y, rail) - RobotSize.REACH < least) {
                least = Math.min(least, gap(x, y, heading, rail));
            }
        }
        return least;
    }

    /** Samples the footprint's outline every quarter inch; a rail is longer than the robot, so any overlap crosses it. */
    private static double gap(double x, double y, double heading, KeepOut rail) {
        double c = Math.cos(heading);
        double s = Math.sin(heading);
        double[][] corners = {
                {RobotSize.LENGTH / 2, RobotSize.WIDTH / 2}, {-RobotSize.LENGTH / 2, RobotSize.WIDTH / 2},
                {-RobotSize.LENGTH / 2, -RobotSize.WIDTH / 2}, {RobotSize.LENGTH / 2, -RobotSize.WIDTH / 2}};
        double least = Double.POSITIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            double[] a = corners[i];
            double[] b = corners[(i + 1) % 4];
            int steps = (int) Math.ceil(Math.hypot(b[0] - a[0], b[1] - a[1]) * 4);
            for (int k = 0; k <= steps; k++) {
                double u = a[0] + (b[0] - a[0]) * k / steps; // along the robot's front
                double v = a[1] + (b[1] - a[1]) * k / steps; // to its left
                least = Math.min(least, distance(x + u * c - v * s, y + u * s + v * c, rail));
            }
        }
        return least;
    }

    private static double distance(double x, double y, KeepOut k) {
        double dx = Math.max(Math.max(k.minX - x, 0), x - k.maxX);
        double dy = Math.max(Math.max(k.minY - y, 0), y - k.maxY);
        return Math.hypot(dx, dy);
    }

    private static void assertSameAngle(double expected, double actual, double tolerance) {
        assertSameAngle("", expected, actual, tolerance);
    }

    private static void assertSameAngle(String what, double expected, double actual, double tolerance) {
        assertEquals(what, 0, DrivePlan.wrap(expected - actual), tolerance);
    }
}
