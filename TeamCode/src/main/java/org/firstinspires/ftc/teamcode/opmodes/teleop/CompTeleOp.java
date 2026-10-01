package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.teleop.ControlScheme;
import org.firstinspires.ftc.teamcode.teleop.DriverCommands;
import org.firstinspires.ftc.teamcode.teleop.TeleOpBase;
import org.firstinspires.ftc.teamcode.teleop.schemes.SoloScheme;
import org.firstinspires.ftc.teamcode.teleop.schemes.TwoDriverScheme;

/**
 * The competition TeleOp. Button assignments live in the schemes (teleop/schemes/), not here:
 * this class wires {@link DriverCommands} to subsystems and registers one OpMode per variant.
 * The autos preselect the Red/Blue variant of their alliance.
 */
public abstract class CompTeleOp extends TeleOpBase {

    @Override
    protected ControlScheme scheme() {
        return new TwoDriverScheme();
    }

    @Override
    protected void apply(DriverCommands c) {
        // Mechanism subsystems (intake, shooter, feeders) are not written yet. As each one lands,
        // create it in onInit() and drive it from here, e.g. intake.setPower(c.intake).
    }

    @TeleOp(name = "Red: TeleOp", group = "comp")
    public static class Red extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.RED; }
    }

    @TeleOp(name = "Blue: TeleOp", group = "comp")
    public static class Blue extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.BLUE; }
    }

    /** One person on gamepad1, for practice and testing. Red frame. */
    @TeleOp(name = "Solo: TeleOp", group = "test")
    public static class Solo extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.RED; }
        @Override protected ControlScheme scheme() { return new SoloScheme(); }
    }
}
