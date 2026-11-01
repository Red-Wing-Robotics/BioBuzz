package org.firstinspires.ftc.teamcode.teleop.schemes;

import static org.firstinspires.ftc.teamcode.teleop.Pad.GAMEPAD2;

import org.firstinspires.ftc.teamcode.teleop.Binding;
import org.firstinspires.ftc.teamcode.teleop.Button;
import org.firstinspires.ftc.teamcode.teleop.DriverCommands;
import org.firstinspires.ftc.teamcode.teleop.DriverInput;

/**
 * {@link AimShooterScheme} without the shot log: the operator's Y / X / B / A each toggle one
 * feeder servo (top, upper middle, lower middle, bottom) and the left bumper toggles the intake
 * on at full power in. While the intake is toggled on, holding the left trigger still runs it out
 * to unjam; while it is off, the triggers work as in {@link TwoDriverScheme}.
 */
public class AimFeederScheme extends AimShooterScheme {

    private final Binding topFeeder = button(GAMEPAD2, Button.Y, "press: top feeder on / off");
    private final Binding upMidFeeder = button(GAMEPAD2, Button.X, "press: upper middle feeder on / off");
    private final Binding downMidFeeder = button(GAMEPAD2, Button.B, "press: lower middle feeder on / off");
    private final Binding bottomFeeder = button(GAMEPAD2, Button.A, "press: bottom feeder on / off");
    private final Binding intake = button(GAMEPAD2, Button.LEFT_BUMPER, "press: intake on / off (left trigger still unjams)");

    private boolean topOn;
    private boolean upMidOn;
    private boolean downMidOn;
    private boolean bottomOn;
    private boolean intakeOn;

    public AimFeederScheme() {
        super(false);
    }

    @Override
    public String name() {
        return "two drivers + aim + shooter speed + feeders";
    }

    @Override
    public void map(DriverInput in, DriverCommands out) {
        super.map(in, out);
        if (in.pressed(topFeeder)) {
            topOn = !topOn;
        }
        if (in.pressed(upMidFeeder)) {
            upMidOn = !upMidOn;
        }
        if (in.pressed(downMidFeeder)) {
            downMidOn = !downMidOn;
        }
        if (in.pressed(bottomFeeder)) {
            bottomOn = !bottomOn;
        }
        if (in.pressed(intake)) {
            intakeOn = !intakeOn;
        }
        out.topFeeder = topOn;
        out.upMidFeeder = upMidOn;
        out.downMidFeeder = downMidOn;
        out.bottomFeeder = bottomOn;
        if (intakeOn && out.intake >= 0) {
            out.intake = 1; // full in, unless the left trigger is unjamming
        }
    }
}
