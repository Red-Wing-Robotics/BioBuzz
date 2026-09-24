package org.firstinspires.ftc.teamcode.auto;

import static org.firstinspires.ftc.teamcode.auto.Steps.instant;
import static org.firstinspires.ftc.teamcode.auto.Steps.parallel;
import static org.firstinspires.ftc.teamcode.auto.Steps.race;
import static org.firstinspires.ftc.teamcode.auto.Steps.sequence;
import static org.firstinspires.ftc.teamcode.auto.Steps.waitSeconds;
import static org.firstinspires.ftc.teamcode.auto.Steps.waitUntil;
import static org.firstinspires.ftc.teamcode.auto.Steps.withTimeout;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StepsTest {

    private final double[] clock = {0};
    private final AutoContext ctx = new AutoContext(null, null, () -> clock[0]);
    private final List<String> log = new ArrayList<>();

    /** A step that finishes after a set number of updates and records its lifecycle. */
    private Step ticks(String name, int updates) {
        return new Step() {
            int remaining;
            @Override public void start(AutoContext c) { remaining = updates; log.add(name + ":start"); }
            @Override public boolean update(AutoContext c) { return --remaining <= 0; }
            @Override public void end(AutoContext c, boolean interrupted) { log.add(name + (interrupted ? ":interrupted" : ":end")); }
            @Override public String name() { return name; }
        };
    }

    private boolean run(Step step, int maxLoops) {
        step.start(ctx);
        for (int i = 0; i < maxLoops; i++) {
            if (step.update(ctx)) {
                step.end(ctx, false);
                return true;
            }
        }
        return false;
    }

    @Test
    public void sequenceRunsInOrderAndInstantStepsAreFree() {
        Step s = sequence(instant("a", () -> log.add("a")), ticks("b", 2), instant("c", () -> log.add("c")));
        s.start(ctx);
        assertFalse(s.update(ctx));                 // a ran, b started and took its first tick
        assertEquals("b", s.name());
        assertTrue(s.update(ctx));                  // b finished, c ran, all done in this loop
        assertEquals(Arrays.asList("a", "b:start", "b:end", "c"), log);
    }

    @Test
    public void sequenceInterruptsActiveStepOnly() {
        Step s = sequence(ticks("a", 1), ticks("b", 5));
        s.start(ctx);
        s.update(ctx);                              // a done, b active
        s.end(ctx, true);
        assertEquals(Arrays.asList("a:start", "a:end", "b:start", "b:interrupted"), log);
    }

    @Test
    public void parallelFinishesWhenAllFinish() {
        Step p = parallel(ticks("a", 1), ticks("b", 3));
        assertTrue(run(p, 3));
        assertEquals(Arrays.asList("a:start", "b:start", "a:end", "b:end"), log);
    }

    @Test
    public void raceFinishesOnFirstAndInterruptsOthers() {
        Step r = race(ticks("slow", 10), ticks("fast", 2));
        assertTrue(run(r, 2));
        assertEquals(Arrays.asList("slow:start", "fast:start", "fast:end", "slow:interrupted"), log);
    }

    @Test
    public void waitSecondsUsesTheContextClock() {
        Step w = waitSeconds(1.5);
        w.start(ctx);
        clock[0] = 1.0;
        assertFalse(w.update(ctx));
        clock[0] = 1.5;
        assertTrue(w.update(ctx));
    }

    @Test
    public void withTimeoutInterruptsALongStep() {
        Step t = withTimeout(2.0, ticks("long", 100));
        t.start(ctx);
        assertFalse(t.update(ctx));
        clock[0] = 2.0;
        assertTrue(t.update(ctx));
        assertEquals(Arrays.asList("long:start", "long:interrupted"), log);
    }

    @Test
    public void waitUntilPollsCondition() {
        boolean[] flag = {false};
        Step w = waitUntil("flag", () -> flag[0]);
        w.start(ctx);
        assertFalse(w.update(ctx));
        flag[0] = true;
        assertTrue(w.update(ctx));
    }
}
