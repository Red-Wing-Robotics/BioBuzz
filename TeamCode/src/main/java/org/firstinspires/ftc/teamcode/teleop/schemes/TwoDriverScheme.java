package org.firstinspires.ftc.teamcode.teleop.schemes;

import static org.firstinspires.ftc.teamcode.teleop.Pad.GAMEPAD1;
import static org.firstinspires.ftc.teamcode.teleop.Pad.GAMEPAD2;

import org.firstinspires.ftc.teamcode.teleop.Axis;
import org.firstinspires.ftc.teamcode.teleop.Binding;
import org.firstinspires.ftc.teamcode.teleop.Button;
import org.firstinspires.ftc.teamcode.teleop.ControlScheme;
import org.firstinspires.ftc.teamcode.teleop.DriverCommands;
import org.firstinspires.ftc.teamcode.teleop.DriverInput;

/**
 * Match layout: the driver drives (gamepad1), the operator runs the mechanisms (gamepad2).
 * The bindings below are the control card: change a button here and every TeleOp using this
 * scheme follows.
 */
public class TwoDriverScheme extends ControlScheme {

    // ---- Driver (gamepad1) --------------------------------------------------------------
    private final Binding forward = axis(GAMEPAD1, Axis.LEFT_STICK_Y, "drive forward / back");
    private final Binding strafe = axis(GAMEPAD1, Axis.LEFT_STICK_X, "strafe left / right");
    private final Binding turn = axis(GAMEPAD1, Axis.RIGHT_STICK_X, "turn");
    private final Binding slow = button(GAMEPAD1, Button.RIGHT_BUMPER, "hold: slow mode");

    // ---- Operator (gamepad2) ------------------------------------------------------------
    private final Binding intakeIn = axis(GAMEPAD2, Axis.RIGHT_TRIGGER, "intake in");
    private final Binding intakeOut = axis(GAMEPAD2, Axis.LEFT_TRIGGER, "intake out (unjam)");
    private final Binding shooter = button(GAMEPAD2, Button.Y, "press: shooter on / off");
    private final Binding fire = button(GAMEPAD2, Button.RIGHT_BUMPER, "hold: fire");

    private boolean shooterOn;

    @Override
    public String name() {
        return "two drivers";
    }

    @Override
    public void map(DriverInput in, DriverCommands out) {
        robotCentricDrive(in, forward, strafe, turn, slow, out);
        out.intake = in.value(intakeIn) - in.value(intakeOut);
        if (in.pressed(shooter)) {
            shooterOn = !shooterOn;
        }
        out.shooterOn = shooterOn;
        out.fire = in.held(fire);
    }
}
