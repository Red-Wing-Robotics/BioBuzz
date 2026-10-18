package org.firstinspires.ftc.teamcode.teleop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    /**
     * Keep the robot's front pointed at this, or null for none. While set, the scheme leaves
     * {@link #turn} at 0 and TeleOpBase computes the turn ({@link HeadingAim}).
     */
    public AimTarget aim;

    /** Intake power: +1 full in, −1 full out. */
    public double intake;
    /** Shooter flywheels should be spinning. */
    public boolean shooterOn;
    /** Change the shooter's target velocity by this much this loop, ticks/s (0: leave it). The shooter remembers the target. */
    public double shooterVelocityChange;
    /** Feed elements into the flywheels while true. The shooter decides whether it is ready. */
    public boolean fire;
    /** Write a row to the shot log this loop, marked with this result; null for none. */
    public ShotResult logShot;

    private final List<String> pressedAssists = new ArrayList<>();
    private final List<String> heldAssists = new ArrayList<>();

    /** Start the named {@link Assist}, or cancel it if it is running. Call in the loop its button is pressed. */
    public void press(String assist) {
        pressedAssists.add(assist);
    }

    /** Run the named {@link Assist} while this is called every loop; it is cancelled the first loop it isn't. */
    public void hold(String assist) {
        heldAssists.add(assist);
    }

    public List<String> pressedAssists() {
        return Collections.unmodifiableList(pressedAssists);
    }

    public List<String> heldAssists() {
        return Collections.unmodifiableList(heldAssists);
    }

    /** Back to "do nothing". Called before every {@link ControlScheme#map}. */
    public void clear() {
        forward = 0;
        strafe = 0;
        turn = 0;
        slow = false;
        aim = null;
        intake = 0;
        shooterOn = false;
        shooterVelocityChange = 0;
        fire = false;
        logShot = null;
        pressedAssists.clear();
        heldAssists.clear();
    }
}
