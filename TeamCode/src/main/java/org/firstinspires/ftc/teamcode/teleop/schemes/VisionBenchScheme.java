package org.firstinspires.ftc.teamcode.teleop.schemes;

import static org.firstinspires.ftc.teamcode.teleop.Pad.GAMEPAD1;

import org.firstinspires.ftc.teamcode.teleop.Assist;
import org.firstinspires.ftc.teamcode.teleop.Axis;
import org.firstinspires.ftc.teamcode.teleop.Binding;
import org.firstinspires.ftc.teamcode.teleop.Button;
import org.firstinspires.ftc.teamcode.teleop.ControlScheme;
import org.firstinspires.ftc.teamcode.teleop.DriverCommands;
import org.firstinspires.ftc.teamcode.teleop.DriverInput;

/**
 * The vision bench (opmodes/test/BallVisionBench), on gamepad1: drive as in {@link SoloScheme},
 * hold A to drive to a ball the camera sees, press B to forget every tracked ball.
 */
public class VisionBenchScheme extends ControlScheme {

    private final Binding forward = axis(GAMEPAD1, Axis.LEFT_STICK_Y, "drive forward / back");
    private final Binding strafe = axis(GAMEPAD1, Axis.LEFT_STICK_X, "strafe left / right");
    private final Binding turn = axis(GAMEPAD1, Axis.RIGHT_STICK_X, "turn");
    private final Binding slow = button(GAMEPAD1, Button.RIGHT_BUMPER, "hold: slow mode");
    private final Binding toBall = button(GAMEPAD1, Button.A, "hold: drive to a ball (sticks cancel)");
    private final Binding forget = button(GAMEPAD1, Button.B, "press: forget the tracked balls");

    @Override
    public String name() {
        return "vision bench";
    }

    @Override
    public void map(DriverInput in, DriverCommands out) {
        robotCentricDrive(in, forward, strafe, turn, slow, out);
        if (in.held(toBall)) {
            out.hold(Assist.TO_BALL);
        }
        if (in.pressed(forget)) {
            out.press(Assist.FORGET_BALLS);
        }
    }
}
