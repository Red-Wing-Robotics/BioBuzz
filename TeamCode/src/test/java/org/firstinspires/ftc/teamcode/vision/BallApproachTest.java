package org.firstinspires.ftc.teamcode.vision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BallApproachTest {

    private static final double EPS = 1e-9;

    static BallTracker.Ball ball(int id, BallKind kind, double x, double y) {
        return new BallTracker.Ball(id, kind, new double[] {x, y}, 0.9, 0);
    }

    private static double angle(double a) {
        return Math.atan2(Math.sin(a), Math.cos(a));
    }

    @Test
    public void stopsStandoffShortFacingTheBall() {
        Pose stop = BallApproach.standoffPose(new Pose(0, 0, 0), 30, 40, 15, BallApproach.Side.FRONT);
        assertEquals(21, stop.x(), EPS);
        assertEquals(28, stop.y(), EPS);
        assertEquals(Math.atan2(4, 3), stop.heading(), EPS);
        Pose back = BallApproach.standoffPose(new Pose(0, 0, 0), 30, 40, 15, BallApproach.Side.BACK);
        assertEquals(0, angle(back.heading() - Math.atan2(4, 3) - Math.PI), EPS);
    }

    @Test
    public void onTopOfTheBallItKeepsItsHeading() {
        Pose stop = BallApproach.standoffPose(new Pose(30, 30, 0), 30, 30, 15, BallApproach.Side.FRONT);
        assertEquals(15, stop.x(), EPS);
        assertEquals(30, stop.y(), EPS);
        assertEquals(0, angle(stop.heading()), EPS);
    }

    @Test
    public void theRobotMustFitWhereItStops() {
        assertTrue(BallApproach.whyNotStopAt(new Pose(5, 50, 0), Alliance.RED, false, 0).contains("walls"));
        assertNull("TeleOp may cross", BallApproach.whyNotStopAt(new Pose(66, 20, 0), Alliance.RED, false, 0));
        assertTrue(BallApproach.whyNotStopAt(new Pose(66, 20, 0), Alliance.RED, true, 0).contains("G402"));
        assertNull(BallApproach.whyNotStopAt(new Pose(40, 72, 0), Alliance.RED, false, 0));
        assertTrue(BallApproach.whyNotStopAt(new Pose(40, 72, 0), Alliance.RED, true, 0).contains("HIVE"));
        // Turned 45° at the HIVE's corner: no robot corner is on the HIVE, but the HIVE's corner is under the robot.
        assertTrue(BallApproach.whyNotStopAt(new Pose(44, 43, Math.PI / 4), Alliance.RED, true, 0).contains("HIVE"));
        assertNull(BallApproach.whyNotStopAt(new Pose(30, 20, 0), Alliance.RED, true, 3));
    }

    @Test
    public void aBallAgainstTheWallIsApproachedSquareEnoughToFit() {
        // A GARDEN ball against the audience wall, seen from vision_seek's GARDEN waypoint, 7 in to
        // the side: straight on, a front corner would be in the wall.
        DriveToBall.Settings s = new DriveToBall.Settings();
        Pose robot = new Pose(11, 26, -Math.PI / 2);
        Pose straight = BallApproach.standoffPose(robot, 18, 1.4, s.standoff, BallApproach.Side.FRONT);
        assertTrue(BallApproach.whyNotStopAt(straight, Alliance.RED, true, 3).contains("walls"));

        Pose stop = BallApproach.stopFor(robot, 18, 1.4, Alliance.RED, s);
        assertNotNull(stop);
        assertEquals(s.standoff, Math.hypot(stop.x() - 18, stop.y() - 1.4), 1e-9);
        assertNull(BallApproach.whyNotStopAt(stop, Alliance.RED, true, s.margin));
        assertEquals("still facing the ball", 0,
                angle(stop.heading() - Math.atan2(1.4 - stop.y(), 18 - stop.x())), 1e-9);
    }

    @Test
    public void aBallInACornerTooTightToReachIsPassedOverAndSaysWhy() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        BallTracker.Ball corner = ball(9, BallKind.POLLEN, 1.4, 1.4);
        assertNull(BallApproach.stopFor(new Pose(20, 20, 0), 1.4, 1.4, Alliance.RED, s));
        assertTrue(BallApproach.whyNotChosen(corner, new Pose(20, 20, 0), Alliance.RED, s).startsWith("nowhere to stop"));
    }

    @Test
    public void choosesTheQuickestLegalWantedBallInRange() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        s.colors = PickupRules.Colors.OURS;
        List<BallTracker.Ball> balls = Arrays.asList(
                ball(1, BallKind.BLUE_NECTAR, 26, 4),    // nearest, but the opponent's (G408); off the way
                ball(2, BallKind.POLLEN, 50, 10),
                ball(3, BallKind.POLLEN, 42, 26),        // this one
                ball(4, BallKind.POLLEN, 90, 20));       // out of range, and over the centre line
        BallApproach.Choice c = BallApproach.choose(balls, new Pose(20, 20, 0), Alliance.RED, s);
        assertNotNull(c);
        assertEquals(3, c.ball.id);
        assertEquals(s.standoff, Math.hypot(c.stop.x() - 42, c.stop.y() - 26), 1e-9);

        s.colors = PickupRules.Colors.OUR_NECTAR;
        assertNull(BallApproach.choose(balls, new Pose(20, 20, 0), Alliance.RED, s));
    }

    // ---- groups ------------------------------------------------------------------------------

    private static final Pose ROBOT = new Pose(20, 20, 0);

    private static List<BallTracker.Ball> oneAndThree(double singleX, double singleY) {
        return Arrays.asList(
                ball(1, BallKind.POLLEN, singleX, singleY),
                ball(2, BallKind.POLLEN, 40, 30),
                ball(3, BallKind.POLLEN, 42.5, 31),
                ball(4, BallKind.POLLEN, 41, 33.5));
    }

    @Test
    public void threeTogetherBeatOneAtAboutTheSameDistanceAndTheStopFacesThem() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        BallApproach.Choice c = BallApproach.choose(oneAndThree(40, 12), ROBOT, Alliance.RED, s);
        assertEquals("the group's nearest ball", 2, c.ball.id);
        assertEquals(3, c.group.count);
        assertTrue(c.why, c.why.startsWith("3 together"));
        assertEquals(s.standoff, Math.hypot(c.stop.x() - 40, c.stop.y() - 30), 1e-9);
        assertEquals("facing through the ball at the group's middle", 0,
                angle(c.stop.heading() - Math.atan2(c.group.y - c.stop.y(), c.group.x - c.stop.x())), 1e-9);
    }

    @Test
    public void aSingleBallMuchNearerStillWins() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        BallApproach.Choice c = BallApproach.choose(oneAndThree(28, 18), ROBOT, Alliance.RED, s);
        assertEquals(1, c.ball.id);
        assertEquals(1, c.group.count);
        assertEquals("", c.why);
    }

    @Test
    public void equalGroupsGoToTheQuicker() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        List<BallTracker.Ball> balls = Arrays.asList(
                ball(1, BallKind.POLLEN, 40, 12), ball(2, BallKind.POLLEN, 42, 10),
                ball(3, BallKind.POLLEN, 42, 30), ball(4, BallKind.POLLEN, 44, 32));
        BallApproach.Choice c = BallApproach.choose(balls, ROBOT, Alliance.RED, s);
        assertEquals(2, c.group.count);
        assertEquals(1, c.ball.id);
    }

    @Test
    public void ballsItMayNotTakeDontMakeAGroup() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        s.within = 60;
        List<BallTracker.Ball> balls = Arrays.asList(
                ball(1, BallKind.POLLEN, 69, 20),
                ball(2, BallKind.POLLEN, 73, 20),         // within 6 in, but over the centre line (G402)
                ball(3, BallKind.BLUE_NECTAR, 69, 24));   // within 6 in, but the opponent's (G408)
        assertEquals(1, BallApproach.group(balls.get(0), balls, Alliance.RED, s).count);
    }

    @Test
    public void oneBoxHoldingThreeBallsBeatsALoneBallAboutAsNear() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        BallTracker.Ball lone = ball(1, BallKind.POLLEN, 40, 12);
        BallTracker.Ball blob = ball(2, BallKind.POLLEN, 41, 31);
        blob.sized(3, 6, true);
        BallApproach.Choice c = BallApproach.choose(Arrays.asList(lone, blob), ROBOT, Alliance.RED, s);
        assertEquals(2, c.ball.id);
        assertEquals(3, c.group.count);
    }

    @Test
    public void aBlobBesideALoneBallIsTheTargetNotTheNearerLoneBall() {
        // 2026-10-22 on the field: a lone ball next to a blob made one group, and the robot drove
        // to the group's nearest member, the lone ball.
        DriveToBall.Settings s = new DriveToBall.Settings();
        BallTracker.Ball lone = ball(1, BallKind.POLLEN, 38, 26);
        BallTracker.Ball blob = ball(2, BallKind.POLLEN, 43, 27);
        blob.sized(3, 6, true);
        BallApproach.Choice c = BallApproach.choose(Arrays.asList(lone, blob), ROBOT, Alliance.RED, s);
        assertEquals(2, c.ball.id);
        assertEquals(4, c.group.count);
        assertTrue(c.candidates, c.candidates.contains("→ POLLEN×3 (43, 27) group 4"));
        assertTrue(c.candidates, c.candidates.contains("POLLEN (38, 26) group 4"));
    }

    private static DriveToBall.Settings pollenOnly() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        s.colors = PickupRules.Colors.POLLEN;
        return s;
    }

    @Test
    public void aPollenSeekPassesOverAGroupWithANectarInIt() {
        // 2026-10-29 on the field: three POLLEN with a red NECTAR among them, and a lone POLLEN.
        Pose robot = new Pose(20, 20, 0);
        List<BallTracker.Ball> balls = Arrays.asList(
                ball(1, BallKind.POLLEN, 40, 26), ball(2, BallKind.POLLEN, 42.8, 26), ball(3, BallKind.POLLEN, 41.4, 28.4),
                ball(4, BallKind.RED_NECTAR, 44, 28), ball(5, BallKind.POLLEN, 40, 12));
        BallApproach.Choice c = BallApproach.choose(balls, robot, Alliance.RED, pollenOnly());
        assertEquals(5, c.ball.id);
        assertTrue(c.candidates, c.candidates.contains("✗ POLLEN (40, 26): nowhere to stop for it: the robot would touch the RED_NECTAR at (44, 28)"));
        assertTrue(c.candidates, c.candidates.contains("✗ RED_NECTAR (44, 28): not one of the colours asked for"));

        DriveToBall.Settings ours = new DriveToBall.Settings(); // our NECTAR wanted too: nothing to avoid
        assertEquals(4, BallApproach.choose(balls, robot, Alliance.RED, ours).group.count);
    }

    @Test
    public void aNectarOnTheWayRulesTheBallOutButOneBehindDoesnt() {
        // Facing the POLLEN, the NECTAR 12 in along the way: clear of the robot now and at the stop.
        Pose robot = new Pose(20, 20, Math.atan2(22, 28));
        BallTracker.Ball pollen = ball(1, BallKind.POLLEN, 48, 42);
        BallTracker.Ball onTheWay = ball(2, BallKind.RED_NECTAR, 29.4, 27.4);
        assertNull(BallApproach.choose(Arrays.asList(pollen, onTheWay), robot, Alliance.RED, pollenOnly()));
        String why = BallApproach.whyNotChosen(pollen, robot, Alliance.RED, pollenOnly(), Collections.singletonList(onTheWay));
        assertTrue(why, why.contains("drive over the RED_NECTAR at (29, 27)"));

        BallTracker.Ball behind = ball(2, BallKind.RED_NECTAR, 8, 21);
        assertEquals(1, BallApproach.choose(Arrays.asList(pollen, behind), robot, Alliance.RED, pollenOnly()).ball.id);
    }

    @Test
    public void opponentNectarIsAvoidedWhateverTheColours() {
        DriveToBall.Settings ours = new DriveToBall.Settings();
        List<BallTracker.Ball> avoid = BallApproach.avoided(Arrays.asList(
                ball(1, BallKind.POLLEN, 40, 26), ball(2, BallKind.RED_NECTAR, 44, 28), ball(3, BallKind.BLUE_NECTAR, 30, 30)),
                Alliance.RED, ours);
        assertEquals(1, avoid.size());
        assertEquals(BallKind.BLUE_NECTAR, avoid.get(0).kind);
    }

    @Test
    public void theBallsPassedOverAreListedWithWhy() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        s.colors = PickupRules.Colors.POLLEN;
        BallTracker.Ball pollen = ball(1, BallKind.POLLEN, 38, 26);
        BallTracker.Ball nectar = ball(2, BallKind.RED_NECTAR, 44, 6);
        BallApproach.Choice c = BallApproach.choose(Arrays.asList(pollen, nectar), ROBOT, Alliance.RED, s);
        assertTrue(c.candidates, c.candidates.startsWith("→ POLLEN (38, 26) group 1"));
        assertTrue(c.candidates, c.candidates.endsWith(" | ✗ RED_NECTAR (44, 6): not one of the colours asked for (POLLEN)"));
    }

    @Test
    public void aLoneBallMoreThanSixInchesAwayIsItsOwnGroup() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        BallTracker.Ball lone = ball(1, BallKind.POLLEN, 36, 26);
        BallTracker.Ball blob = ball(2, BallKind.POLLEN, 43, 27);
        blob.sized(3, 6, true);
        assertEquals(1, BallApproach.group(lone, Arrays.asList(lone, blob), Alliance.RED, s).count);
        assertEquals(3, BallApproach.group(blob, Arrays.asList(lone, blob), Alliance.RED, s).count);
    }

    @Test
    public void withTheOwnHalfRuleOffAnAutoSeekMayCrossTheCentreLine() {
        // 2026-10-29 on the field: from PARK (85, 108) the ball at (66, 107) had nowhere to stop
        // that was on our half and clear of the HIVE.
        DriveToBall.Settings s = new DriveToBall.Settings();
        s.margin = 3;
        s.hiveMargin = 3;
        Pose park = new Pose(85, 108, 0);
        BallTracker.Ball pocket = ball(1, BallKind.POLLEN, 66, 107);
        BallTracker.Ball overThere = ball(2, BallKind.POLLEN, 100, 120);
        assertTrue(BallApproach.whyNotChosen(pocket, park, Alliance.RED, s).startsWith("nowhere to stop"));
        assertTrue(BallApproach.whyNotChosen(overThere, park, Alliance.RED, s).contains("G402"));

        s.ownHalfOnly = false;
        assertNull(BallApproach.whyNotChosen(pocket, park, Alliance.RED, s));
        assertNull(BallApproach.whyNotChosen(overThere, park, Alliance.RED, s));
        Pose stop = BallApproach.stopFor(park, 66, 107, Alliance.RED, s);
        assertNull("still clear of the HIVE", BallApproach.whyNotStopAt(stop, Alliance.RED, true, false, 3));
    }

    @Test
    public void preferNearestIgnoresGroups() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        s.preferGroups = false;
        BallApproach.Choice c = BallApproach.choose(oneAndThree(40, 12), ROBOT, Alliance.RED, s);
        assertEquals(1, c.ball.id);
    }

    @Test
    public void replansOnlyWhenTheBallMovedNotTooSoonAndNotOnceCommitted() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        assertTrue(BallApproach.shouldReplan(3, 300, 30, s));
        assertFalse("moved too little", BallApproach.shouldReplan(1, 300, 30, s));
        assertFalse("too soon", BallApproach.shouldReplan(3, 100, 30, s));
        assertFalse("committed", BallApproach.shouldReplan(3, 300, 10, s));
    }
}
