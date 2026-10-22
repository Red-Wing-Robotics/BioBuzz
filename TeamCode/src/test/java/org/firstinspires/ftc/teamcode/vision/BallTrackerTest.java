package org.firstinspires.ftc.teamcode.vision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.junit.Test;

import java.util.Arrays;

public class BallTrackerTest {

    private static final CameraMount MOUNT = CameraMountTest.MEASURED;
    /** Facing the blue wall; a ball at (45, 26) is 25 in ahead, in the middle of the image. */
    private static final Pose ROBOT = new Pose(20, 20, 0);

    private final BallTracker.Settings settings = new BallTracker.Settings();
    private final BallTracker tracker = new BallTracker(settings);
    private double frameId;
    private double now;

    /** How the camera would report a ball at field (x, y) with the robot at {@code robot}. */
    static Detection sees(BallKind kind, double x, double y, Pose robot, double confidence) {
        double[] r = CameraMount.toRobotFrame(robot, x, y);
        double[] a = MOUNT.toImage(r[0], r[1], kind.centreHeight());
        return new Detection(kind, a[0], a[1], confidence);
    }

    private static Detection pollen(double x, double y, Pose robot) {
        return sees(BallKind.POLLEN, x, y, robot, 0.9);
    }

    /** A new frame, 50 ms after the last, taken with the robot at {@code robot}. */
    private boolean frame(Pose robot, Detection... detections) {
        return frame(robot, 0, detections);
    }

    private boolean frame(Pose robot, double turnRate, Detection... detections) {
        now += 50;
        frameId++;
        return tracker.update(new Frame(frameId, now, Arrays.asList(detections)), robot, turnRate, MOUNT, now);
    }

    @Test
    public void projectsADetectionBackToWhereTheBallIs() {
        frame(ROBOT, pollen(45, 26, ROBOT));
        BallTracker.Ball b = tracker.balls().get(0);
        assertEquals(45, b.x(), 1e-6);
        assertEquals(26, b.y(), 1e-6);
    }

    @Test
    public void theSameFrameTwiceCountsOnce() {
        Frame f = new Frame(7, 50, Arrays.asList(pollen(45, 26, ROBOT)));
        assertTrue(tracker.update(f, ROBOT, 0, MOUNT, 50));
        assertFalse(tracker.update(f, ROBOT, 0, MOUNT, 60));
        assertEquals(1, tracker.balls().get(0).sightings());
    }

    @Test
    public void confirmedAfterThreeConsecutiveFrames() {
        frame(ROBOT, pollen(45, 26, ROBOT));
        frame(ROBOT, pollen(45, 26, ROBOT));
        assertTrue(tracker.confirmed().isEmpty());
        frame(ROBOT, pollen(45, 26, ROBOT));
        assertEquals(1, tracker.confirmed().size());
    }

    @Test
    public void aMissWhileInViewStartsTheCountAgain() {
        frame(ROBOT, pollen(45, 26, ROBOT));
        frame(ROBOT, pollen(45, 26, ROBOT));
        frame(ROBOT); // should have been seen: one-frame flickers don't add up to a ball
        frame(ROBOT, pollen(45, 26, ROBOT));
        assertTrue(tracker.confirmed().isEmpty());
        frame(ROBOT, pollen(45, 26, ROBOT));
        frame(ROBOT, pollen(45, 26, ROBOT));
        assertEquals(1, tracker.confirmed().size());
    }

    @Test
    public void aBallKeepsItsIdentityWhileTheRobotDrivesAndTurns() {
        Pose robot = ROBOT;
        frame(robot, pollen(45, 26, robot));
        int id = tracker.balls().get(0).id;
        for (int i = 1; i <= 5; i++) {
            robot = new Pose(20 + 2 * i, 20, 0.04 * i);
            frame(robot, pollen(45, 26, robot));
        }
        assertEquals(1, tracker.balls().size());
        BallTracker.Ball b = tracker.find(id);
        assertNotNull(b);
        assertEquals(45, b.x(), 1e-6);
        assertEquals(26, b.y(), 1e-6);
        assertEquals(0, b.spread(), 1e-6);
    }

    @Test
    public void differentColoursAtTheSameSpotAreDifferentBalls() {
        frame(ROBOT, pollen(45, 26, ROBOT), sees(BallKind.RED_NECTAR, 46, 26, ROBOT, 0.9));
        assertEquals(2, tracker.balls().size());
    }

    @Test
    public void aBallThatShouldBeInViewButIsntIsForgotten() {
        for (int i = 0; i < 3; i++) {
            frame(ROBOT, pollen(45, 26, ROBOT));
        }
        for (int i = 0; i < 8; i++) {
            frame(ROBOT); // 400 ms
        }
        assertEquals(1, tracker.balls().size());
        frame(ROBOT);
        assertTrue(tracker.balls().isEmpty());
    }

    @Test
    public void aBallOutOfViewIsRememberedForAWhile() {
        for (int i = 0; i < 3; i++) {
            frame(ROBOT, pollen(45, 26, ROBOT));
        }
        Pose away = new Pose(20, 20, Math.PI); // turned round: the ball is behind
        for (int i = 0; i < 40; i++) {
            frame(away); // 2 s
        }
        assertEquals("still remembered", 1, tracker.confirmed().size());
        for (int i = 0; i < 25; i++) {
            frame(away);
        }
        assertTrue("forgotten after 3 s", tracker.balls().isEmpty());
    }

    @Test
    public void gardenBallsFromTheFirstFieldTestSpotAreAtTheImageEdge() {
        // 2026-10-15: from (16, 34) facing the audience the camera saw the GARDEN balls, but at
        // ty 19.5°, inside the 2° edge margin, so none were used and the seek gave up.
        Pose firstSpot = new Pose(16, 34, -Math.PI / 2);
        frame(firstSpot, pollen(12, 1.4, firstSpot), pollen(18, 1.4, firstSpot));
        assertTrue(tracker.balls().isEmpty());
        assertEquals("2 seen, 0 used: 2 at the image edge", tracker.lastFrameSummary());

        Pose garden = new Pose(11, 26, -Math.PI / 2); // vision_seek.json's GARDEN now
        frame(garden, pollen(12, 1.4, garden), pollen(18, 1.4, garden));
        assertEquals("2 seen, 2 used", tracker.lastFrameSummary());
    }

    @Test
    public void aBallPlacedJustPastAWallIsAgainstItAndOneFarPastIsIgnored() {
        Pose facingAudience = new Pose(20, 20, -Math.PI / 2);
        frame(facingAudience, pollen(24, -1.5, facingAudience)); // range error: estimated 1.5 in into the wall
        assertEquals(1, tracker.balls().size());
        assertEquals(BallKind.POLLEN.diameter / 2, tracker.balls().get(0).y(), 1e-9);

        tracker.clear();
        frame(facingAudience, pollen(24, -8, facingAudience));
        assertTrue(tracker.balls().isEmpty());
        assertEquals("1 seen, 0 used: 1 beyond the walls", tracker.lastFrameSummary());
    }

    /** A detection whose box is {@code balls} times one ball's area at that spot. */
    private static Detection box(double x, double y, Pose robot, double balls) {
        Detection d = pollen(x, y, robot);
        double[] r = CameraMount.toRobotFrame(robot, x, y);
        return new Detection(d.kind, d.txDeg, d.tyDeg, d.confidence, balls * MOUNT.ballAreaPct(r[0], r[1], BallKind.POLLEN));
    }

    @Test
    public void aBoxThreeBallsBigCountsAsThree() {
        frame(ROBOT, box(45, 26, ROBOT, 3.1), box(38, 30, ROBOT, 1.0), pollen(42, 21, ROBOT));
        assertEquals(3, tracker.find(1).blob());
        assertEquals(1, tracker.find(2).blob());
        assertEquals("no area reported: one ball", 1, tracker.find(3).blob());
    }

    /** {@link #box} at a given confidence. */
    private static Detection box(double x, double y, Pose robot, double balls, double confidence) {
        Detection d = box(x, y, robot, balls);
        return new Detection(d.kind, d.txDeg, d.tyDeg, confidence, d.areaPct);
    }

    @Test
    public void aClumpScoredLowStillCountsButALoneBallScoredLowDoesnt() {
        // 2026-10-22 on the field: "3 seen, 1 used: 2 unsure", and the blob was never tracked.
        frame(ROBOT, box(45, 26, ROBOT, 3, 0.30), box(38, 30, ROBOT, 1, 0.30));
        assertEquals(1, tracker.balls().size());
        assertEquals(3, tracker.balls().get(0).blob());
        assertEquals("2 seen, 1 used: 1 unsure (30% ≈1.0 balls)", tracker.lastFrameSummary());

        frame(ROBOT, box(45, 26, ROBOT, 3, 0.20)); // too unsure even for a clump
        assertEquals("1 seen, 0 used: 1 unsure (20% ≈3.0 balls)", tracker.lastFrameSummary());
    }

    @Test
    public void theCountIsSmoothedOverFrames() {
        frame(ROBOT, box(45, 26, ROBOT, 3));
        frame(ROBOT, box(45, 26, ROBOT, 1)); // halfway: 2
        assertEquals(2, tracker.balls().get(0).blob());
    }

    @Test
    public void gatesKeepOutUnsureEdgeFarAndBlurredSightings() {
        frame(ROBOT, sees(BallKind.POLLEN, 45, 26, ROBOT, 0.3));
        frame(ROBOT, new Detection(BallKind.POLLEN, 26.5, -5, 0.9));
        settings.maxRange = 20;
        frame(ROBOT, pollen(45, 26, ROBOT));
        settings.maxRange = 40;
        frame(ROBOT, Math.toRadians(150), pollen(45, 26, ROBOT));
        assertTrue(tracker.balls().isEmpty());

        now += 50;
        tracker.update(new Frame(++frameId, now, Arrays.asList(pollen(45, 26, ROBOT))), null, 0, MOUNT, now);
        assertTrue("no pose at capture: nothing is placed", tracker.balls().isEmpty());

        frame(ROBOT, pollen(45, 26, ROBOT));
        assertEquals(1, tracker.balls().size());
    }
}
