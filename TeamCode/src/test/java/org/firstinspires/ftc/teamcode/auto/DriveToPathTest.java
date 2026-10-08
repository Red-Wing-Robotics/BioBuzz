package org.firstinspires.ftc.teamcode.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import org.firstinspires.ftc.teamcode.opmodes.teleop.HiveClearance;
import org.firstinspires.ftc.teamcode.opmodes.teleop.ShootingSpots;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/** The paths CompTeleOp's driving assists follow, checked against the HIVE with the real spot values. */
public class DriveToPathTest {

    /** An 18 × 18 in robot, turned any way, stays inside this radius of its centre. */
    private static final double ROBOT_REACH = Math.hypot(9, 9);

    private static List<KeepOut> hive() {
        return Collections.singletonList(Field.HIVE.grow(HiveClearance.inches));
    }

    @Test
    public void spotToSpotGoesRoundTheHiveForBothAlliances() {
        for (Alliance alliance : Alliance.values()) {
            Pose left = alliance.pose(ShootingSpots.left);
            Pose right = alliance.pose(ShootingSpots.right);
            assertKeepsClear(alliance + " left → right", Steps.pathTo(left, right, hive()));
            assertKeepsClear(alliance + " right → left", Steps.pathTo(right, left, hive()));
        }
    }

    @Test
    public void fromAnywhereOnOurSideToEitherSpotKeepsClear() {
        for (int x = 12; x <= 132; x += 12) {
            for (int y = 12; y <= 132; y += 12) {
                Pose from = Alliance.RED.poses().of(x, y, 0);
                if (Field.HIVE.grow(ROBOT_REACH).contains(x, y)) {
                    continue; // the robot can't be here
                }
                for (Waypoint spot : new Waypoint[] {ShootingSpots.left, ShootingSpots.right}) {
                    assertKeepsClear("from (" + x + ", " + y + ") to " + spot,
                            Steps.pathTo(from, Alliance.RED.pose(spot), hive()));
                }
            }
        }
    }

    @Test
    public void endsAtTheTargetPoseAndTurnsOnTheWay() {
        Pose from = Alliance.RED.pose(ShootingSpots.right);
        Pose to = Alliance.RED.pose(ShootingSpots.left);
        Path path = Steps.pathTo(from, to, hive());
        assertEquals(to.x(), path.endPose().x(), 1e-6);
        assertEquals(to.y(), path.endPose().y(), 1e-6);
        assertSameAngle(to.heading(), path.heading(1));
        assertSameAngle(from.heading(), path.heading(0));
    }

    private static void assertSameAngle(double expected, double actual) {
        double d = expected - actual;
        assertEquals(0, Math.atan2(Math.sin(d), Math.cos(d)), 1e-6);
    }

    @Test
    public void clearLineStaysStraight() {
        Pose from = Alliance.RED.poses().of(20, 20, 0);
        Pose to = Alliance.RED.poses().of(20, 120, 90);
        Path path = Steps.pathTo(from, to, hive());
        Pose middle = path.get(0.5);
        assertEquals(20, middle.x(), 1e-6);
        assertEquals(70, middle.y(), 1e-6);
    }

    @Test
    public void redLeftIsTheDriversLeft() {
        // docs/coordinates.md: red drivers face +x, so their left is +y (the far wall).
        assertTrue(ShootingSpots.left.y > ShootingSpots.right.y);
    }

    /** The robot's whole footprint, at any heading, stays off the HIVE at every point of the path. */
    private static void assertKeepsClear(String what, Path path) {
        for (int i = 0; i <= 400; i++) {
            Pose p = path.get(i / 400.0);
            double gap = distanceToHive(p.x(), p.y());
            assertTrue(String.format("%s: robot centre %.1f in from the HIVE at (%.1f, %.1f)", what, gap, p.x(), p.y()),
                    gap >= ROBOT_REACH);
        }
    }

    private static double distanceToHive(double x, double y) {
        KeepOut h = Field.HIVE;
        double dx = Math.max(Math.max(h.minX - x, 0), x - h.maxX);
        double dy = Math.max(Math.max(h.minY - y, 0), y - h.maxY);
        return Math.hypot(dx, dy);
    }
}
