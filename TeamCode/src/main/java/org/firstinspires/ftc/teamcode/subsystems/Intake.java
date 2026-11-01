package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.RobotHardware;

/**
 * The intake motor, {@link RobotHardware#INTAKE}, run open-loop: +1 full in, −1 full out.
 *
 * Does not count elements yet: G407 (never CONTROL more than 4) is on the drivers until it does.
 */
public class Intake {

    // TODO verify on the robot (docs/hardware.md): positive power must pull elements in.
    private static final DcMotorSimple.Direction DIRECTION = DcMotorSimple.Direction.FORWARD;

    private final DcMotor motor;
    /** What was last sent to the motor, so unchanged commands cost no hardware writes. */
    private double commanded = Double.NaN;

    /** Configures the motor; moves nothing (G403). */
    public Intake(RobotHardware robot) {
        motor = robot.getHardwareMap().get(DcMotor.class, RobotHardware.INTAKE);
        motor.setDirection(DIRECTION);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    /** +1 full in, −1 full out (unjam), 0 stop. Clamped to −1..1. */
    public void setPower(double power) {
        double p = Math.max(-1, Math.min(1, power));
        if (p == commanded) {
            return;
        }
        commanded = p;
        motor.setPower(p);
    }
}
