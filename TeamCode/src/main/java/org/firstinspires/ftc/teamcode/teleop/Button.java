package org.firstinspires.ftc.teamcode.teleop;

/**
 * Digital gamepad buttons a {@link Binding} can use. Xbox names; on a PlayStation controller
 * A = cross, B = circle, X = square, Y = triangle, BACK = share.
 *
 * START and GUIDE are left out on purpose: START+A / START+B assigns the gamepads on the Driver
 * Station, and GUIDE is not on every controller.
 */
public enum Button {
    A, B, X, Y,
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    LEFT_BUMPER, RIGHT_BUMPER,
    LEFT_STICK_BUTTON, RIGHT_STICK_BUTTON,
    BACK
}
