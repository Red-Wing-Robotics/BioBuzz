package org.firstinspires.ftc.teamcode.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.pedropathing.algorithm.ForesightConfig;

import org.firstinspires.ftc.teamcode.RobotId;
import org.firstinspires.ftc.teamcode.pedro.Constants;
import org.junit.Test;

/** {@link Steps#atSpeed} against a robot's real Foresight settings, which every path shares. */
public class AtSpeedTest {

    private final AutoContext ctx = new AutoContext(null, null, () -> 0);
    private final ForesightConfig config = Constants.foresightConfig(RobotId.BELTESHAZZAR);

    private Step capped(double fraction, boolean[] done) {
        Step drive = new Step() {
            @Override public boolean update(AutoContext c) { return done[0]; }
        };
        return new Steps.AtSpeed(fraction, drive, c -> config);
    }

    @Test
    public void capsThePathSpeedWhileItRunsAndPutsItBackAfter() {
        double before = config.maxPathSpeed.get();
        boolean[] done = {false};
        Step step = capped(0.5, done);
        step.start(ctx);
        assertEquals(0.5, config.maxPathSpeed.get(), 0);
        step.update(ctx);
        done[0] = true;
        assertTrue(step.update(ctx));
        step.end(ctx, false);
        assertEquals(before, config.maxPathSpeed.get(), 0);
    }

    @Test
    public void anInterruptedDrivePutsItBackToo() {
        double before = config.maxPathSpeed.get();
        Step step = capped(0.5, new boolean[] {false});
        step.start(ctx);
        step.end(ctx, true);
        assertEquals(before, config.maxPathSpeed.get(), 0);
    }

    @Test
    public void fullSpeedChangesNothing() {
        double before = config.maxPathSpeed.get();
        Step step = capped(1, new boolean[] {false});
        step.start(ctx);
        assertEquals(before, config.maxPathSpeed.get(), 0);
        step.end(ctx, true);
    }
}
