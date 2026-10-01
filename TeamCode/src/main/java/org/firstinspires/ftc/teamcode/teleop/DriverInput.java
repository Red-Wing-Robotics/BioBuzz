package org.firstinspires.ftc.teamcode.teleop;

/**
 * Both gamepads, read through a scheme's {@link Binding}s. Each binding knows its gamepad, so a
 * scheme names gamepad1/gamepad2 only where it declares its bindings.
 */
public final class DriverInput {

    public final GamepadState gamepad1 = new GamepadState();
    public final GamepadState gamepad2 = new GamepadState();

    /** Stick −1..1 (up/right positive), trigger 0..1, or 1/0 for a button. */
    public double value(Binding binding) {
        return binding.value(state(binding), false);
    }

    /** Button down, or axis past half travel. */
    public boolean held(Binding binding) {
        return binding.held(state(binding), false);
    }

    /** True only in the loop where the input went down. */
    public boolean pressed(Binding binding) {
        GamepadState state = state(binding);
        return binding.held(state, false) && !binding.held(state, true);
    }

    /** True only in the loop where the input came up. */
    public boolean released(Binding binding) {
        GamepadState state = state(binding);
        return !binding.held(state, false) && binding.held(state, true);
    }

    private GamepadState state(Binding binding) {
        return binding.pad == Pad.GAMEPAD1 ? gamepad1 : gamepad2;
    }
}
