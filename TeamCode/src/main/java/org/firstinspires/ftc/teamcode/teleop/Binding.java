package org.firstinspires.ftc.teamcode.teleop;

import java.util.Locale;

/**
 * One line of a control card: a gamepad input and what it does. Schemes declare bindings with
 * {@link ControlScheme#button} / {@link ControlScheme#axis} and read them through
 * {@link DriverInput}.
 *
 * An axis binding can also be read as a button: it counts as held past half travel, which lets a
 * trigger toggle something.
 */
public final class Binding {

    /** An axis read as a button is held beyond this much travel. */
    static final double AXIS_HELD = 0.5;

    public final Pad pad;
    /** Set for a button binding; null for an axis binding. */
    public final Button button;
    /** Set for an axis binding; null for a button binding. */
    public final Axis axis;
    /** What the input does, worded for the drivers ("hold: fire"). */
    public final String action;

    Binding(Pad pad, Button button, Axis axis, String action) {
        if (pad == null || (button == null) == (axis == null)) {
            throw new IllegalArgumentException("a binding needs a pad and exactly one of button / axis");
        }
        this.pad = pad;
        this.button = button;
        this.axis = axis;
        this.action = action;
    }

    /** The input as the control card names it, e.g. "gp2 right bumper". */
    public String input() {
        String name = button != null ? button.name() : axis.name();
        return pad.label + " " + name.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    double value(GamepadState state, boolean last) {
        if (button != null) {
            return (last ? state.lastButton(button) : state.button(button)) ? 1.0 : 0.0;
        }
        return last ? state.lastAxis(axis) : state.axis(axis);
    }

    boolean held(GamepadState state, boolean last) {
        return Math.abs(value(state, last)) > AXIS_HELD;
    }

    @Override
    public String toString() {
        return input() + ": " + action;
    }
}
