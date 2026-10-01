package org.firstinspires.ftc.teamcode.teleop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DriverInputTest {

    private static final double EPS = 1e-9;

    private final DriverInput in = new DriverInput();
    private final Binding a = new Binding(Pad.GAMEPAD1, Button.A, null, "test");
    private final Binding trigger = new Binding(Pad.GAMEPAD2, null, Axis.RIGHT_TRIGGER, "test");

    private void nextLoop() {
        in.gamepad1.nextLoop();
        in.gamepad2.nextLoop();
    }

    @Test
    public void pressedOnlyInTheLoopTheButtonWentDown() {
        nextLoop();
        in.gamepad1.set(Button.A, true);
        assertTrue(in.pressed(a));
        assertTrue("reading twice in a loop must not consume the press", in.pressed(a));
        assertTrue(in.held(a));

        nextLoop(); // still down
        assertFalse(in.pressed(a));
        assertTrue(in.held(a));
    }

    @Test
    public void releasedOnlyInTheLoopTheButtonCameUp() {
        nextLoop();
        in.gamepad1.set(Button.A, true);
        assertFalse(in.released(a));

        nextLoop();
        in.gamepad1.set(Button.A, false);
        assertTrue(in.released(a));
        assertFalse(in.held(a));

        nextLoop();
        assertFalse(in.released(a));
    }

    @Test
    public void axisCountsAsHeldPastHalfTravel() {
        nextLoop();
        in.gamepad2.set(Axis.RIGHT_TRIGGER, 0.4);
        assertEquals(0.4, in.value(trigger), EPS);
        assertFalse(in.held(trigger));
        assertFalse(in.pressed(trigger));

        nextLoop();
        in.gamepad2.set(Axis.RIGHT_TRIGGER, 0.6);
        assertTrue(in.held(trigger));
        assertTrue(in.pressed(trigger));

        nextLoop();
        in.gamepad2.set(Axis.RIGHT_TRIGGER, 0.9);
        assertFalse(in.pressed(trigger));
    }

    @Test
    public void bindingReadsOnlyItsOwnGamepad() {
        nextLoop();
        in.gamepad2.set(Button.A, true);
        assertFalse(in.held(a));
        assertEquals(0.0, in.value(a), EPS);
    }

    @Test
    public void controlCardNamesTheInput() {
        assertEquals("gp1 a", a.input());
        assertEquals("gp2 right trigger", trigger.input());
    }

    @Test(expected = IllegalArgumentException.class)
    public void bindingNeedsExactlyOneInput() {
        new Binding(Pad.GAMEPAD1, Button.A, Axis.LEFT_TRIGGER, "both");
    }
}
