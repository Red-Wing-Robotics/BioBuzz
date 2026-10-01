package org.firstinspires.ftc.teamcode.teleop;

/**
 * Analog gamepad inputs a {@link Binding} can use. Sticks read −1..1 with pushing <b>up</b> or
 * <b>right</b> positive (the SDK reports stick Y as negative when pushed up; {@link TeleOpBase}
 * flips it). Triggers read 0 (released) to 1 (fully pulled).
 */
public enum Axis {
    LEFT_STICK_X, LEFT_STICK_Y,
    RIGHT_STICK_X, RIGHT_STICK_Y,
    LEFT_TRIGGER, RIGHT_TRIGGER
}
