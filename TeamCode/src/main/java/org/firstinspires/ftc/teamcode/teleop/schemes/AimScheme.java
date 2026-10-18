package org.firstinspires.ftc.teamcode.teleop.schemes;

import static org.firstinspires.ftc.teamcode.teleop.Pad.GAMEPAD1;

import org.firstinspires.ftc.teamcode.teleop.AimTarget;
import org.firstinspires.ftc.teamcode.teleop.Binding;
import org.firstinspires.ftc.teamcode.teleop.Button;
import org.firstinspires.ftc.teamcode.teleop.DriverCommands;
import org.firstinspires.ftc.teamcode.teleop.DriverInput;

/**
 * {@link TwoDriverScheme} plus heading aim on the driver's X / B: while engaged, the robot keeps
 * its front pointed at that CELL of our HIVE and the turn stick does nothing; driving and
 * strafing work as usual. Press a button to aim at its CELL, again to stop; the other button
 * switches CELLs.
 */
public class AimScheme extends TwoDriverScheme {

    private final Binding aimLeft = button(GAMEPAD1, Button.X, "press: point at left CELL / stop (turn stick off)");
    private final Binding aimRight = button(GAMEPAD1, Button.B, "press: point at right CELL / stop (turn stick off)");

    private AimTarget aim;

    public AimScheme() {
        super();
    }

    /** See {@link TwoDriverScheme#TwoDriverScheme(boolean)}. */
    protected AimScheme(boolean shooterButtons) {
        super(shooterButtons);
    }

    @Override
    public String name() {
        return "two drivers + aim";
    }

    @Override
    public void map(DriverInput in, DriverCommands out) {
        super.map(in, out);
        if (in.pressed(aimLeft)) {
            aim = aim == AimTarget.LEFT_CELL ? null : AimTarget.LEFT_CELL;
        }
        if (in.pressed(aimRight)) {
            aim = aim == AimTarget.RIGHT_CELL ? null : AimTarget.RIGHT_CELL;
        }
        out.aim = aim;
        if (aim != null) {
            out.turn = 0; // the aim owns the heading
        }
    }
}
