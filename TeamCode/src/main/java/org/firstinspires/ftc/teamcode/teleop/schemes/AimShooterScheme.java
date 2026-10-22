package org.firstinspires.ftc.teamcode.teleop.schemes;

import static org.firstinspires.ftc.teamcode.teleop.Pad.GAMEPAD2;

import org.firstinspires.ftc.teamcode.teleop.Binding;
import org.firstinspires.ftc.teamcode.teleop.Button;
import org.firstinspires.ftc.teamcode.teleop.DriverCommands;
import org.firstinspires.ftc.teamcode.teleop.DriverInput;
import org.firstinspires.ftc.teamcode.teleop.ShotResult;

/**
 * {@link AimScheme} with the operator's shooter controls for setting flywheel speed by hand:
 * right bumper turns both flywheels on / off, d-pad up / down raises / lowers their speed by
 * {@link #VELOCITY_STEP}. The shooter keeps the speed while off (subsystems/Shooter). A / X
 * log the last shot as a hit / miss, with the distance to the HIVE and the flywheel speeds, for
 * fitting speed against distance. The two-driver Y toggle and right-bumper fire are not bound
 * here.
 */
public class AimShooterScheme extends AimScheme {

    /** How much one d-pad press changes the flywheel speed, ticks/s. */
    public static final double VELOCITY_STEP = 100;

    private final Binding shooter = button(GAMEPAD2, Button.RIGHT_BUMPER, "press: shooters on / off");
    private final Binding faster = button(GAMEPAD2, Button.DPAD_UP, "press: shooters +" + (int) VELOCITY_STEP + " ticks/s");
    private final Binding slower = button(GAMEPAD2, Button.DPAD_DOWN, "press: shooters -" + (int) VELOCITY_STEP + " ticks/s");
    private final Binding logHit = button(GAMEPAD2, Button.A, "press: log shot as HIT (distance + speeds)");
    private final Binding logMiss = button(GAMEPAD2, Button.X, "press: log shot as MISS (distance + speeds)");

    private boolean shooterOn;

    public AimShooterScheme() {
        super(false);
    }

    @Override
    public String name() {
        return "two drivers + aim + shooter speed";
    }

    @Override
    public void map(DriverInput in, DriverCommands out) {
        super.map(in, out);
        if (in.pressed(shooter)) {
            shooterOn = !shooterOn;
        }
        out.shooterOn = shooterOn;
        if (in.pressed(faster)) {
            out.shooterVelocityChange += VELOCITY_STEP;
        }
        if (in.pressed(slower)) {
            out.shooterVelocityChange -= VELOCITY_STEP;
        }
        if (in.pressed(logHit)) {
            out.logShot = ShotResult.HIT;
        } else if (in.pressed(logMiss)) {
            out.logShot = ShotResult.MISS;
        }
    }
}
