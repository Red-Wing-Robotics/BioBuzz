package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.subsystems.Feeders;
import org.firstinspires.ftc.teamcode.subsystems.Intake;
import org.firstinspires.ftc.teamcode.teleop.ControlScheme;
import org.firstinspires.ftc.teamcode.teleop.DriverCommands;
import org.firstinspires.ftc.teamcode.teleop.Resource;
import org.firstinspires.ftc.teamcode.teleop.schemes.AimFeederScheme;

/**
 * Aim Shooter TeleOp without the shot log, plus the intake and the four feeder servos
 * ({@link AimFeederScheme}): gp2 Y / X / B / A toggle the top / upper middle / lower middle /
 * bottom feeder, gp2 left bumper toggles the intake. Its own class so the other TeleOps don't
 * need the feeder servos in the RC config.
 */
public abstract class FeederIntakeTest extends CompTeleOp {

    private Intake intake;
    private Feeders feeders;

    @Override
    protected ControlScheme scheme() {
        return new AimFeederScheme();
    }

    @Override
    protected void onInit() {
        super.onInit();
        intake = new Intake(robot);
        feeders = new Feeders(robot);
    }

    @Override
    protected void apply(DriverCommands c) {
        super.apply(c);
        if (!assisting(Resource.INTAKE)) {
            intake.setPower(c.intake);
        }
        if (!assisting(Resource.FEEDER)) {
            feeders.set(c.topFeeder, c.upMidFeeder, c.downMidFeeder, c.bottomFeeder);
        }
        telemetry.addData("feeders", "top %s  up mid %s  down mid %s  bottom %s   intake %.1f",
                onOff(c.topFeeder), onOff(c.upMidFeeder), onOff(c.downMidFeeder), onOff(c.bottomFeeder), c.intake);
    }

    private static String onOff(boolean on) {
        return on ? "ON" : "off";
    }

    @TeleOp(name = "Red: FeederIntakeTest", group = "test")
    public static class Red extends FeederIntakeTest {
        @Override protected Alliance alliance() { return Alliance.RED; }
    }

    @TeleOp(name = "Blue: FeederIntakeTest", group = "test")
    public static class Blue extends FeederIntakeTest {
        @Override protected Alliance alliance() { return Alliance.BLUE; }
    }
}
