package org.firstinspires.ftc.teamcode.teleop;

/**
 * One gamepad's inputs this loop and last loop, so a press or release is a comparison between
 * the two. Hardware-free: {@link TeleOpBase} copies the SDK {@code Gamepad} in once per loop;
 * tests set values directly.
 *
 * Reads have no side effects, unlike the SDK's {@code aWasPressed()} family (which reports a
 * press only to its first caller), so any number of bindings may read the same input in a loop.
 */
public final class GamepadState {

    private final boolean[] buttons = new boolean[Button.values().length];
    private final boolean[] lastButtons = new boolean[buttons.length];
    private final double[] axes = new double[Axis.values().length];
    private final double[] lastAxes = new double[axes.length];

    /** Start a new loop: the current inputs become last loop's. Call before setting this loop's values. */
    public void nextLoop() {
        System.arraycopy(buttons, 0, lastButtons, 0, buttons.length);
        System.arraycopy(axes, 0, lastAxes, 0, axes.length);
    }

    public GamepadState set(Button button, boolean down) {
        buttons[button.ordinal()] = down;
        return this;
    }

    /** Sticks −1..1 with up/right positive; triggers 0..1. */
    public GamepadState set(Axis axis, double value) {
        axes[axis.ordinal()] = value;
        return this;
    }

    public boolean button(Button button) {
        return buttons[button.ordinal()];
    }

    public double axis(Axis axis) {
        return axes[axis.ordinal()];
    }

    boolean lastButton(Button button) {
        return lastButtons[button.ordinal()];
    }

    double lastAxis(Axis axis) {
        return lastAxes[axis.ordinal()];
    }
}
