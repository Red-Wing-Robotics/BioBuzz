package org.firstinspires.ftc.teamcode.teleop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.auto.AutoContext;
import org.firstinspires.ftc.teamcode.auto.Step;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AssistsTest {

    private static final Consumer<DriverCommands> NOTHING = c -> {};

    private final double[] clock = {0};
    private final AutoContext ctx = new AutoContext(null, null, () -> clock[0]);
    private final List<String> log = new ArrayList<>();
    private final Assists assists = new Assists();
    private final DriverCommands commands = new DriverCommands();

    /** Builds steps that finish after a set number of updates and record their lifecycle. */
    private Supplier<Step> ticks(String name, int updates) {
        return () -> new Step() {
            int remaining;
            @Override public void start(AutoContext c) { remaining = updates; log.add(name + ":start"); }
            @Override public boolean update(AutoContext c) { return --remaining <= 0; }
            @Override public void end(AutoContext c, boolean interrupted) { log.add(name + (interrupted ? ":interrupted" : ":end")); }
            @Override public String name() { return name; }
        };
    }

    /** One TeleOp loop as TeleOpBase runs it: the scheme's requests, then an update. */
    private void loop(boolean endgame, Consumer<DriverCommands> scheme) {
        commands.clear();
        scheme.accept(commands);
        assists.request(commands, ctx, endgame);
        assists.update(ctx);
    }

    private void loop(Consumer<DriverCommands> scheme) {
        loop(false, scheme);
    }

    @Test
    public void pressedAssistRunsToCompletionThenReleasesItsResources() {
        assists.define("park", EnumSet.of(Resource.DRIVE), ticks("park", 2));
        loop(c -> c.press("park"));
        assertTrue(assists.isRunning("park"));
        assertTrue(assists.owns(Resource.DRIVE));

        loop(NOTHING);
        assertFalse(assists.isRunning("park"));
        assertFalse(assists.owns(Resource.DRIVE));
        assertEquals(Arrays.asList("park:start", "park:end"), log);
    }

    @Test
    public void pressingAgainCancels() {
        assists.define("park", EnumSet.of(Resource.DRIVE), ticks("park", 10));
        loop(c -> c.press("park"));
        loop(c -> c.press("park"));
        assertFalse(assists.isRunning("park"));
        assertEquals(Arrays.asList("park:start", "park:interrupted"), log);
    }

    @Test
    public void heldAssistStopsOnRelease() {
        assists.define("aim", EnumSet.of(Resource.DRIVE), ticks("aim", 10));
        loop(c -> c.hold("aim"));
        loop(c -> c.hold("aim"));
        assertTrue(assists.isRunning("aim"));

        loop(NOTHING);
        assertFalse(assists.isRunning("aim"));
        assertEquals(Arrays.asList("aim:start", "aim:interrupted"), log);
    }

    @Test
    public void heldAssistThatFinishesWaitsForReleaseBeforeRestarting() {
        assists.define("aim", EnumSet.of(Resource.DRIVE), ticks("aim", 1));
        loop(c -> c.hold("aim"));                    // starts and finishes
        loop(c -> c.hold("aim"));                    // still held: no restart
        assertEquals(Arrays.asList("aim:start", "aim:end"), log);

        loop(NOTHING);
        loop(c -> c.hold("aim"));
        assertEquals(Arrays.asList("aim:start", "aim:end", "aim:start", "aim:end"), log);
    }

    @Test
    public void heldAssistCancelledByTheSticksWaitsForRelease() {
        assists.define("aim", EnumSet.of(Resource.DRIVE), ticks("aim", 10));
        loop(c -> c.hold("aim"));
        assists.cancel(Resource.DRIVE, ctx);         // a drive stick moved
        loop(c -> c.hold("aim"));                    // still held: no restart
        loop(c -> c.hold("aim"));
        assertFalse(assists.isRunning("aim"));
        assertEquals(Arrays.asList("aim:start", "aim:interrupted"), log);

        loop(NOTHING);
        loop(c -> c.hold("aim"));
        assertTrue(assists.isRunning("aim"));
    }

    @Test
    public void twoHeldAssistsSharingAResourceDontTakeTurns() {
        assists.define("aim", EnumSet.of(Resource.DRIVE), ticks("aim", 10));
        assists.define("strafe", EnumSet.of(Resource.DRIVE), ticks("strafe", 10));
        loop(c -> c.hold("aim"));
        loop(c -> { c.hold("aim"); c.hold("strafe"); }); // the newer one wins...
        loop(c -> { c.hold("aim"); c.hold("strafe"); }); // ...and keeps it
        assertFalse(assists.isRunning("aim"));
        assertTrue(assists.isRunning("strafe"));
        assertEquals(Arrays.asList("aim:start", "aim:interrupted", "strafe:start"), log);
    }

    @Test
    public void newAssistInterruptsOneSharingAResource() {
        assists.define("park", EnumSet.of(Resource.DRIVE), ticks("park", 10));
        assists.define("shootFromSpot", EnumSet.of(Resource.DRIVE, Resource.SHOOTER), ticks("shootFromSpot", 10));
        loop(c -> c.press("park"));
        loop(c -> c.press("shootFromSpot"));
        assertFalse(assists.isRunning("park"));
        assertTrue(assists.isRunning("shootFromSpot"));
        assertEquals(Arrays.asList("park:start", "park:interrupted", "shootFromSpot:start"), log);
    }

    @Test
    public void assistsWithNoSharedResourceRunTogether() {
        assists.define("aim", EnumSet.of(Resource.DRIVE), ticks("aim", 10));
        assists.define("fireAll", EnumSet.of(Resource.SHOOTER, Resource.FEEDER), ticks("fireAll", 10));
        loop(c -> c.hold("aim"));
        loop(c -> {
            c.hold("aim");
            c.press("fireAll");
        });
        assertTrue(assists.isRunning("aim"));
        assertTrue(assists.isRunning("fireAll"));
    }

    @Test
    public void cancellingAResourceStopsOnlyTheAssistsUsingIt() {
        assists.define("park", EnumSet.of(Resource.DRIVE), ticks("park", 10));
        assists.define("fireAll", EnumSet.of(Resource.SHOOTER), ticks("fireAll", 10));
        loop(c -> {
            c.press("park");
            c.press("fireAll");
        });
        assists.cancel(Resource.DRIVE, ctx); // what TeleOpBase does when the drive sticks move
        assertFalse(assists.owns(Resource.DRIVE));
        assertTrue(assists.owns(Resource.SHOOTER));
        assertTrue(log.contains("park:interrupted"));
    }

    @Test
    public void endgameOnlyAssistWaitsForTheEndgame() {
        assists.define("nectar", EnumSet.of(Resource.FEEDER), ticks("nectar", 10)).endgameOnly();
        loop(false, c -> c.press("nectar"));
        assertFalse(assists.isRunning("nectar"));
        assertEquals("nectar: endgame only", assists.notice(clock[0]));

        loop(true, c -> c.press("nectar"));
        assertTrue(assists.isRunning("nectar"));
    }

    @Test
    public void undefinedAssistShowsANoticeThatExpires() {
        loop(c -> c.press("teleport"));
        assertEquals("teleport: not available in this OpMode", assists.notice(clock[0]));
        clock[0] += 4;
        assertNull(assists.notice(clock[0]));
    }

    @Test
    public void stopCancelsEverything() {
        assists.define("park", EnumSet.of(Resource.DRIVE), ticks("park", 10));
        assists.define("fireAll", EnumSet.of(Resource.SHOOTER), ticks("fireAll", 10));
        loop(c -> {
            c.press("park");
            c.press("fireAll");
        });
        assists.cancelAll(ctx);
        assertTrue(assists.describe().isEmpty());
        assertTrue(log.containsAll(Arrays.asList("park:interrupted", "fireAll:interrupted")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void definingANameTwiceFails() {
        assists.define("park", EnumSet.of(Resource.DRIVE), ticks("park", 1));
        assists.define("park", EnumSet.of(Resource.DRIVE), ticks("park", 1));
    }
}
