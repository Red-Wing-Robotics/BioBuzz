package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.RobotHardware;

/**
 * The two shooter flywheels, {@link RobotHardware#TOP_SHOOTER} and
 * {@link RobotHardware#BOTTOM_SHOOTER}, run together at one target velocity under the hub's
 * velocity PID (RUN_USING_ENCODER).
 *
 * The target starts at {@link #DEFAULT_VELOCITY} and is kept while the flywheels are off, so
 * turning them back on returns to the last speed the operator chose.
 */
public class Shooter {

    /** Flywheel speed the first time the shooter turns on, encoder ticks per second. */
    public static final double DEFAULT_VELOCITY = 1500;
    /**
     * Highest target allowed, ticks/s. goBILDA Yellow Jacket motors free-run at about 2800
     * ticks/s at the motor encoder whatever the gearbox; leave headroom for the PID to hold speed
     * under load.
     */
    public static final double MAX_VELOCITY = 2800;

    // TODO verify on the robot (docs/hardware.md): both flywheels must push the element out.
    private static final DcMotorSimple.Direction TOP_DIRECTION = DcMotorSimple.Direction.REVERSE;
    private static final DcMotorSimple.Direction BOTTOM_DIRECTION = DcMotorSimple.Direction.FORWARD;

    private final DcMotorEx top;
    private final DcMotorEx bottom;
    private double targetVelocity = DEFAULT_VELOCITY;
    private boolean on;
    /** What was last sent to the motors, so unchanged commands cost no hardware writes. */
    private double commanded = Double.NaN;

    /** Configures the motors; moves nothing (G403). */
    public Shooter(RobotHardware robot) {
        top = robot.getHardwareMap().get(DcMotorEx.class, RobotHardware.TOP_SHOOTER);
        bottom = robot.getHardwareMap().get(DcMotorEx.class, RobotHardware.BOTTOM_SHOOTER);
        configure(top, TOP_DIRECTION);
        configure(bottom, BOTTOM_DIRECTION);
    }

    private static void configure(DcMotorEx motor, DcMotorSimple.Direction direction) {
        motor.setDirection(direction);
        motor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT); // spin down, don't brake the flywheels
    }

    /** Spin the flywheels at the target velocity, or let them coast to a stop. */
    public void setOn(boolean on) {
        this.on = on;
        apply();
    }

    /** Raise (+) or lower (−) the target by {@code delta} ticks/s, within 0..{@link #MAX_VELOCITY}. Kept while off. */
    public void changeVelocity(double delta) {
        if (delta != 0) {
            targetVelocity = clampVelocity(targetVelocity + delta);
            apply();
        }
    }

    public boolean isOn() {
        return on;
    }

    /** The speed the flywheels run at when on, ticks/s. */
    public double targetVelocity() {
        return targetVelocity;
    }

    /** Measured speed of the top flywheel, ticks/s (bulk-cached). */
    public double topVelocity() {
        return top.getVelocity();
    }

    /** Measured speed of the bottom flywheel, ticks/s (bulk-cached). */
    public double bottomVelocity() {
        return bottom.getVelocity();
    }

    private static double clampVelocity(double velocity) {
        return Math.max(0, Math.min(MAX_VELOCITY, velocity));
    }

    private void apply() {
        double velocity = on ? targetVelocity : 0;
        if (velocity == commanded) {
            return;
        }
        commanded = velocity;
        if (velocity == 0) {
            // setVelocity(0) would have the PID brake the flywheels; zero power lets them coast.
            top.setPower(0);
            bottom.setPower(0);
        } else {
            top.setVelocity(velocity);
            bottom.setVelocity(velocity);
        }
    }
}
