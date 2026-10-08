package org.firstinspires.ftc.teamcode.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The safety rules for OpModes run from Jimothy. Dev flavor only, like the guard. */
public class RemoteRunGuardTest {

    private static final String AUTO = "Red: Leave + Park";

    /** INIT and START from Jimothy at t = 0; returns the guard, RUNNING. */
    private static RemoteRunGuard runningFromRouteView() {
        RemoteRunGuard g = new RemoteRunGuard();
        assertNull(g.init());
        g.phase(AUTO, "INIT", 0);
        assertNull(g.start(0));
        g.phase(AUTO, "RUNNING", 0);
        assertTrue(g.startedHere);
        return g;
    }

    @Test
    public void stopsWhenRouteViewGoesQuiet() {
        RemoteRunGuard g = runningFromRouteView();
        for (long t = 500; t <= 3000; t += 500) {
            g.heard(t);
            assertNull("heartbeats keep it running at " + t, g.check(t + 100));
        }
        assertNull(g.check(3000 + RemoteRunGuard.HEARTBEAT_TIMEOUT_MS));
        String why = g.check(3000 + RemoteRunGuard.HEARTBEAT_TIMEOUT_MS + 1);
        assertNotNull(why);
        assertTrue(why, why.contains("stopped answering"));
        assertNull("stops once", g.check(10_000));
    }

    @Test
    public void stopsAtTheAutoLimitEvenWithHeartbeats() {
        RemoteRunGuard g = runningFromRouteView();
        long t = 0;
        while (t < RemoteRunGuard.AUTO_LIMIT_MS) {
            t += 400;
            g.heard(t);
            if (t <= RemoteRunGuard.AUTO_LIMIT_MS) {
                assertNull(g.check(t));
            }
        }
        String why = g.check(RemoteRunGuard.AUTO_LIMIT_MS + 1);
        assertNotNull(why);
        assertTrue(why, why.contains("30 s"));
    }

    @Test
    public void leavesAnOpModeStartedOnTheDriverStationAlone() {
        RemoteRunGuard g = new RemoteRunGuard();
        g.heard(0);
        g.phase(AUTO, "INIT", 0);
        g.phase(AUTO, "RUNNING", 0); // START pressed on the DS, not here
        assertFalse(g.startedHere);
        assertNull(g.check(60_000));
    }

    @Test
    public void routeViewInitThenDriverStationStartIsTheDriverStations() {
        RemoteRunGuard g = new RemoteRunGuard();
        assertNull(g.init());
        g.phase(AUTO, "INIT", 0);
        g.phase(AUTO, "RUNNING", 0);
        assertFalse(g.startedHere);
    }

    @Test
    public void refusesToInitOverAnActiveOpModeOrStartWithoutInit() {
        RemoteRunGuard g = new RemoteRunGuard();
        assertNotNull(g.start(0));
        g.phase(AUTO, "INIT", 0);
        assertTrue(g.init().contains(AUTO));
        g.phase(AUTO, "RUNNING", 0);
        assertNotNull(g.start(0));
    }

    @Test
    public void remembersAnInitThatEndedAtOnce() {
        RemoteRunGuard g = new RemoteRunGuard();
        assertNull(g.init());
        g.phase(AUTO, "INIT", 1000);
        g.phase(null, "IDLE", 1040); // init() threw; the SDK stopped it
        assertEquals(AUTO + " ended in INIT after 0.0 s", g.lastEnded);
        assertNull(g.init());
        assertNull("a new INIT clears it", g.lastEnded);
    }

    @Test
    public void stopEndsTheRunAndIsRemembered() {
        RemoteRunGuard g = runningFromRouteView();
        assertTrue(g.stop());
        assertFalse(g.startedHere);
        assertNull("no watchdog stop after a stop", g.check(60_000));
        g.phase(null, "IDLE", 100);
        assertEquals("IDLE", g.phase);
        assertNull(g.active);
        assertEquals("stopped from Jimothy", g.lastStop);
        assertFalse("nothing to stop now", g.stop());
    }
}
