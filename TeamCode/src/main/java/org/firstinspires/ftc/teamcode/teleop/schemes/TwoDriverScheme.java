package org.firstinspires.ftc.teamcode.teleop.schemes;

import static org.firstinspires.ftc.teamcode.teleop.Pad.GAMEPAD1;
import static org.firstinspires.ftc.teamcode.teleop.Pad.GAMEPAD2;

import org.firstinspires.ftc.teamcode.teleop.Assist;
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
    private final Binding shootLeft = button(GAMEPAD1, Button.DPAD_LEFT, "press: go to left shooting spot and hold (sticks cancel)");
    private final Binding shootRight = button(GAMEPAD1, Button.DPAD_RIGHT, "press: go to right shooting spot and hold (sticks cancel)");

    // ---- Operator (gamepad2) ------------------------------------------------------------
    private final Binding intakeIn = axis(GAMEPAD2, Axis.RIGHT_TRIGGER, "intake in");
    private final Binding intakeOut = axis(GAMEPAD2, Axis.LEFT_TRIGGER, "intake out (unjam)");
    /** Null in a subclass that asked for {@code shooterButtons = false} and binds its own. */
    private final Binding shooter;
    private final Binding fire;

    private boolean shooterOn;

    public TwoDriverScheme() {
        this(true);
    }

    /**
     * @param shooterButtons false leaves the operator's shooter toggle (Y) and fire (right bumper)
     *     unbound, for a subclass with its own shooter controls
     */
    protected TwoDriverScheme(boolean shooterButtons) {
        shooter = shooterButtons ? button(GAMEPAD2, Button.Y, "press: shooter on / off") : null;
        fire = shooterButtons ? button(GAMEPAD2, Button.RIGHT_BUMPER, "hold: fire") : null;
    }

    @Override
    public String name() {
        return "two drivers";
    }

    @Override
    public void map(DriverInput in, DriverCommands out) {
        robotCentricDrive(in, forward, strafe, turn, slow, out);
        out.intake = in.value(intakeIn) - in.value(intakeOut);
        if (shooter != null) {
            if (in.pressed(shooter)) {
                shooterOn = !shooterOn;
            }
            out.shooterOn = shooterOn;
            out.fire = in.held(fire);
        }
        if (in.pressed(shootLeft)) {
            out.press(Assist.SHOOT_LEFT);
        }
        if (in.pressed(shootRight)) {
            out.press(Assist.SHOOT_RIGHT);
        }
    }
}
