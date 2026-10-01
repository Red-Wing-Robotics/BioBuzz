package org.firstinspires.ftc.teamcode.teleop;

/**
 * What the drivers are asking the robot to do this loop: the output of a {@link ControlScheme}
 * and the input to {@link TeleOpBase}, which turns it into drive and subsystem calls. Plain
 * fields and no hardware, so schemes can be unit-tested.
 *
 * To add a driver action: add a field here, reset it in {@link #clear}, set it in every scheme's
 * {@code map}, and act on it in the TeleOp's {@code apply}.
 */
public final class DriverCommands {

    /** Drive powers in Pedro's convention: +forward ahead, +strafe left, +turn counter-clockwise. Already shaped and slow-scaled. */
    public double forward;
    public double strafe;
    public double turn;
    /** Slow mode is held. Already applied to the drive powers; kept for telemetry. */
    public boolean slow;

    /** Intake power: +1 full in, −1 full out. */
    public double intake;
    /** Shooter flywheels should be spinning. */
    public boolean shooterOn;
    /** Feed elements into the flywheels while true. The shooter decides whether it is ready. */
    public boolean fire;

    /** Back to "do nothing". Called before every {@link ControlScheme#map}. */
    public void clear() {
        forward = 0;
        strafe = 0;
        turn = 0;
        slow = false;
        intake = 0;
        shooterOn = false;
        fire = false;
    }
}
