package org.firstinspires.ftc.teamcode.vision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.AutoContext;
import org.firstinspires.ftc.teamcode.auto.Step;
import org.firstinspires.ftc.teamcode.auto.route.RouteException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** {@link DriveToBall} over made-up balls, with a fake drive step that records each plan. */
public class DriveToBallTest {

    private final double[] clock = {0};
    private final AutoContext ctx = new AutoContext(null, null, () -> clock[0]);
    private Pose robot = new Pose(20, 20, 0);
    private final List<BallTracker.Ball> balls = new ArrayList<>();
    private final List<Pose> stops = new ArrayList<>();
    private final List<String> log = new ArrayList<>();
    private boolean arrived;

    private final Balls source = new Balls() {
        @Override public List<BallTracker.Ball> confirmed() { return balls; }

        @Override
        public BallTracker.Ball find(int id) {
            for (BallTracker.Ball b : balls) {
                if (b.id == id) return b;
            }
            return null;
        }
    };

    /** The fake drive: plan n starts, finishes when {@link #arrived}, and logs how it ended. */
    private Step drive(Pose stop) {
        stops.add(stop);
        int n = stops.size();
        return new Step() {
            @Override public void start(AutoContext c) { log.add("start " + n); }
            @Override public boolean update(AutoContext c) { return arrived; }
            @Override public void end(AutoContext c, boolean interrupted) { log.add((interrupted ? "interrupted " : "end ") + n); }
        };
    }

    private DriveToBall started() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        DriveToBall d = new DriveToBall(source, () -> robot, this::drive, Alliance.RED, s);
        d.start(ctx);
        return d;
    }

    private boolean tick(DriveToBall d, double seconds) {
        clock[0] += seconds;
        return d.update(ctx);
    }

    /** The ball with this id, now at (x, y): what the tracker reports as its estimate firms up. */
    private void ballAt(int id, double x, double y) {
        balls.clear();
        balls.add(BallApproachTest.ball(id, BallKind.POLLEN, x, y));
    }

    @Test
    public void withNoBallItGivesUpAfterLooking() {
        DriveToBall d = started();
        assertFalse(tick(d, 0.5));
        assertTrue(tick(d, 0.6));
        assertEquals(DriveToBall.Outcome.NO_TARGET, d.outcome());
        assertTrue(stops.isEmpty());
    }

    @Test
    public void givingUpSaysWhy() {
        DriveToBall d = started();
        tick(d, 1.1);
        assertTrue(d.why(), d.why().contains("no ball confirmed"));

        ballAt(1, 20, 60); // 40 in away
        DriveToBall far = started();
        tick(far, 1.1);
        assertEquals(DriveToBall.Outcome.NO_TARGET, far.outcome());
        assertTrue(far.why(), far.why().contains("more than within 36"));
    }

    @Test
    public void drivesToTheBallAndArrives() {
        ballAt(1, 42, 26);
        DriveToBall d = started();
        assertFalse(tick(d, 0.02));
        assertEquals(1, stops.size());
        assertEquals(new DriveToBall.Settings().standoff, Math.hypot(stops.get(0).x() - 42, stops.get(0).y() - 26), 1e-9);
        arrived = true;
        assertTrue(tick(d, 0.02));
        assertEquals(DriveToBall.Outcome.ARRIVED, d.outcome());
        assertEquals(Arrays.asList("start 1", "end 1"), log);
    }

    @Test
    public void replansAsTheBallMovesButNoMoreOftenThanAllowed() {
        ballAt(1, 42, 26);
        DriveToBall d = started();
        tick(d, 0.02);
        ballAt(1, 45, 26);
        tick(d, 0.1);
        assertEquals("100 ms after the last plan: not yet", 1, stops.size());
        tick(d, 0.2);
        assertEquals(2, stops.size());
        assertEquals(new DriveToBall.Settings().standoff, Math.hypot(stops.get(1).x() - 45, stops.get(1).y() - 26), 1e-9);
        assertEquals("the old plan ends as finished, so the follower isn't stopped",
                Arrays.asList("start 1", "end 1", "start 2"), log);
    }

    @Test
    public void closeInTheTargetIsFrozen() {
        ballAt(1, 42, 26);
        DriveToBall d = started();
        tick(d, 0.02);
        robot = new Pose(30, 22, 0.3); // within 18 in of the ball
        tick(d, 0.02);
        ballAt(1, 47, 26);
        tick(d, 0.5);
        assertEquals(1, stops.size());
        assertTrue(d.name(), d.name().contains("closing in"));
    }

    @Test
    public void aBallThatTurnsOutToBeOverTheCentreLineIsAbandoned() {
        ballAt(1, 42, 26);
        DriveToBall d = started();
        tick(d, 0.02);
        ballAt(1, 80, 26);
        assertTrue(tick(d, 0.3));
        assertEquals(DriveToBall.Outcome.ABANDONED, d.outcome());
        assertTrue(d.why(), d.why().contains("G402"));
        assertEquals(Arrays.asList("start 1", "interrupted 1"), log);
    }

    @Test
    public void interruptingItStopsTheDrive() {
        ballAt(1, 42, 26);
        DriveToBall d = started();
        tick(d, 0.02);
        d.end(ctx, true);
        assertEquals(Arrays.asList("start 1", "interrupted 1"), log);
    }

    // ---- route args ------------------------------------------------------------------------

    private static JsonObject json(String text) {
        return new JsonParser().parse(text).getAsJsonObject();
    }

    @Test
    public void routeArgsOverrideTheDefaults() {
        DriveToBall.Settings defaults = new DriveToBall.Settings();
        DriveToBall.Settings s = ToBallArgs.parse(json("{\"colors\": \"pollen\", \"within\": 24, \"standoff\": 12, \"look\": 1}"), defaults);
        assertEquals(PickupRules.Colors.POLLEN, s.colors);
        assertEquals(24, s.within, 0);
        assertEquals(12, s.standoff, 0);
        assertEquals(1, s.lookSeconds, 0);
        assertTrue("groups preferred by default", s.preferGroups);
        assertFalse(ToBallArgs.parse(json("{\"prefer\": \"nearest\"}"), defaults).preferGroups);
        assertEquals("defaults untouched", PickupRules.Colors.OURS, defaults.colors);
        assertEquals(defaults.commit, ToBallArgs.parse(new JsonObject(), defaults).commit, 0);
    }

    @Test
    public void badRouteArgsSayWhatIsWrong() {
        assertRejected("{\"colour\": \"pollen\"}", "unknown key \"colour\"");
        assertRejected("{\"colors\": \"yellow\"}", "args.colors");
        assertRejected("{\"within\": \"far\"}", "args.within: must be a number of inches");
        assertRejected("{\"within\": 0}", "more than 0");
        assertRejected("{\"look\": -1}", "0 or more");
        assertRejected("{\"prefer\": \"biggest\"}", "args.prefer");
    }

    private static void assertRejected(String args, String messagePart) {
        try {
            ToBallArgs.parse(json(args), new DriveToBall.Settings());
            fail("expected rejection mentioning: " + messagePart);
        } catch (RouteException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(messagePart));
        }
    }
}
