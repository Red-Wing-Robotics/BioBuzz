package org.firstinspires.ftc.teamcode.teleop.schemes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.teleop.Assist;
import org.firstinspires.ftc.teamcode.teleop.Axis;
import org.firstinspires.ftc.teamcode.teleop.Binding;
import org.firstinspires.ftc.teamcode.teleop.Button;
import org.firstinspires.ftc.teamcode.teleop.ControlScheme;
import org.firstinspires.ftc.teamcode.teleop.DriveFeel;
import org.firstinspires.ftc.teamcode.teleop.DriverCommands;
import org.firstinspires.ftc.teamcode.teleop.DriverInput;
import org.firstinspires.ftc.teamcode.teleop.GamepadState;
import org.firstinspires.ftc.teamcode.teleop.Pad;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks every control scheme. Add new schemes to {@link #allSchemes()} so they get the same checks.
 * Assumes DriveFeel defaults (deadband 0.05, linear), so full deflection maps to exactly ±1.
 */
public class ControlSchemesTest {

    private static final double EPS = 1e-9;

    private static List<ControlScheme> allSchemes() {
        return Arrays.asList(new TwoDriverScheme(), new SoloScheme());
    }

    private static DriverCommands map(ControlScheme scheme, DriverInput in) {
        DriverCommands out = new DriverCommands();
        scheme.map(in, out);
        return out;
    }

    /** One loop in which {@code button} on {@code pad} is down or up. */
    private static DriverCommands loop(ControlScheme scheme, DriverInput in, GamepadState pad, Button button, boolean down) {
        in.gamepad1.nextLoop();
        in.gamepad2.nextLoop();
        pad.set(button, down);
        return map(scheme, in);
    }

    @Test
    public void noInputIsBoundTwice() {
        for (ControlScheme scheme : allSchemes()) {
            Set<String> seen = new HashSet<>();
            for (Binding binding : scheme.bindings()) {
                assertTrue(scheme.name() + ": " + binding.input() + " is bound twice", seen.add(binding.input()));
            }
            assertFalse(scheme.name() + " has no bindings", seen.isEmpty());
        }
    }

    @Test
    public void everyBindingSaysWhatItDoes() {
        for (ControlScheme scheme : allSchemes()) {
            for (Binding binding : scheme.bindings()) {
                assertTrue(scheme.name() + ": " + binding.input() + " has no action",
                        binding.action != null && !binding.action.trim().isEmpty());
            }
        }
    }

    @Test
    public void sticksFollowPedroConvention() {
        for (ControlScheme scheme : allSchemes()) {
            DriverInput in = new DriverInput();
            in.gamepad1.set(Axis.LEFT_STICK_Y, 1.0); // pushed up
            assertEquals(scheme.name(), 1.0, map(scheme, in).forward, EPS);

            in.gamepad1.set(Axis.LEFT_STICK_Y, 0).set(Axis.LEFT_STICK_X, 1.0); // pushed right
            assertEquals(scheme.name() + ": Pedro +strafe is left", -1.0, map(scheme, in).strafe, EPS);

            in.gamepad1.set(Axis.LEFT_STICK_X, 0).set(Axis.RIGHT_STICK_X, 1.0); // pushed right
            assertEquals(scheme.name() + ": Pedro +turn is counter-clockwise", -1.0, map(scheme, in).turn, EPS);
        }
    }

    @Test
    public void slowModeScalesTheDrive() {
        for (ControlScheme scheme : allSchemes()) {
            DriverInput in = new DriverInput();
            in.gamepad1.set(Axis.LEFT_STICK_Y, 1.0).set(Button.RIGHT_BUMPER, true);
            DriverCommands out = map(scheme, in);
            assertTrue(scheme.name(), out.slow);
            assertEquals(scheme.name(), DriveFeel.slowScale, out.forward, EPS);
        }
    }

    @Test
    public void twoDriverOperatorRunsTheMechanisms() {
        ControlScheme scheme = new TwoDriverScheme();
        DriverInput in = new DriverInput();
        in.gamepad2.set(Axis.RIGHT_TRIGGER, 0.8);
        assertEquals(0.8, map(scheme, in).intake, EPS);
        in.gamepad2.set(Axis.RIGHT_TRIGGER, 0).set(Axis.LEFT_TRIGGER, 1.0);
        assertEquals(-1.0, map(scheme, in).intake, EPS);

        in.gamepad1.set(Button.RIGHT_BUMPER, true); // driver's slow mode
        assertFalse(map(scheme, in).fire);
        in.gamepad2.set(Button.RIGHT_BUMPER, true);
        assertTrue(map(scheme, in).fire);
    }

    @Test
    public void soloUsesOnlyGamepad1() {
        for (Binding binding : new SoloScheme().bindings()) {
            assertEquals(binding.input(), Pad.GAMEPAD1, binding.pad);
        }
    }

    @Test
    public void soloBRequestsReturnToStartOncePerPress() {
        ControlScheme scheme = new SoloScheme();
        DriverInput in = new DriverInput();
        assertEquals(Collections.singletonList(Assist.RETURN_TO_START),
                loop(scheme, in, in.gamepad1, Button.B, true).pressedAssists());
        assertTrue("still held: no second request",
                loop(scheme, in, in.gamepad1, Button.B, true).pressedAssists().isEmpty());
    }

    @Test
    public void driverDpadGoesToTheShootingSpots() {
        for (ControlScheme scheme : allSchemes()) {
            DriverInput in = new DriverInput();
            assertEquals(scheme.name(), Collections.singletonList(Assist.SHOOT_LEFT),
                    loop(scheme, in, in.gamepad1, Button.DPAD_LEFT, true).pressedAssists());
            loop(scheme, in, in.gamepad1, Button.DPAD_LEFT, false);
            assertEquals(scheme.name(), Collections.singletonList(Assist.SHOOT_RIGHT),
                    loop(scheme, in, in.gamepad1, Button.DPAD_RIGHT, true).pressedAssists());
        }
    }

    @Test
    public void shooterTogglesOnEachPressNotWhileHeld() {
        ControlScheme scheme = new TwoDriverScheme();
        DriverInput in = new DriverInput();
        assertTrue("press: on", loop(scheme, in, in.gamepad2, Button.Y, true).shooterOn);
        assertTrue("held: stays on", loop(scheme, in, in.gamepad2, Button.Y, true).shooterOn);
        assertTrue("released: stays on", loop(scheme, in, in.gamepad2, Button.Y, false).shooterOn);
        assertFalse("press again: off", loop(scheme, in, in.gamepad2, Button.Y, true).shooterOn);
    }
}
