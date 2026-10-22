package org.firstinspires.ftc.teamcode.opmodes.test;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.RobotHardware;

/**
 * Wiring check for the competition robot's four drive motors: hold a button on gamepad1 and only
 * that wheel should spin, forward. If a different wheel spins, its cable is in the wrong port or
 * the RC config names are swapped; if the right wheel spins backward, its direction is wrong.
 * Expected ports are in docs/hardware.md.
 *
 * <pre>
 *   X = front left    Y = front right
 *   A = back left     B = back right
 * </pre>
 *
 * Standalone, like the StarterBot OpMode: it reads gamepad1 directly instead of using a control
 * scheme, so it doesn't need a pose or the Follower. Put the robot on blocks with the wheels off
 * the floor. Nothing moves until a button is held.
 */
@TeleOp(name = "Test: Drive Motors", group = "test")
public class DriveMotorTest extends OpMode {

    /** Power a held button gives its motor. Low, so a wrongly wired wheel can't do much harm. */
    private static final double TEST_POWER = 0.3;

    /** One drive motor and what the docs say it should be. */
    private static final class Wheel {
        final String label;
        final String button;
        final String expectedPort;
        final DcMotorEx motor;
        /** Last power sent, so telemetry and change checks don't ask the hub. */
        double power;

        Wheel(String label, String button, String expectedPort, DcMotorEx motor) {
            this.label = label;
            this.button = button;
            this.expectedPort = expectedPort;
            this.motor = motor;
        }
    }

    private RobotHardware robot;
    private Wheel frontLeft;
    private Wheel frontRight;
    private Wheel backLeft;
    private Wheel backRight;

    @Override
    public void init() {
        robot = new RobotHardware(hardwareMap); // turns on bulk reads
        // Same directions as pedro/Constants, so positive power should spin each wheel forward.
        frontLeft = wheel("front left", "X", "Expansion Hub motor 0",
                RobotHardware.FRONT_LEFT_DRIVE, DcMotorSimple.Direction.REVERSE);
        frontRight = wheel("front right", "Y", "Control Hub motor 0",
                RobotHardware.FRONT_RIGHT_DRIVE, DcMotorSimple.Direction.FORWARD);
        backLeft = wheel("back left", "A", "Expansion Hub motor 1",
                RobotHardware.BACK_LEFT_DRIVE, DcMotorSimple.Direction.REVERSE);
        backRight = wheel("back right", "B", "Control Hub motor 1",
                RobotHardware.BACK_RIGHT_DRIVE, DcMotorSimple.Direction.FORWARD);
    }

    private Wheel wheel(String label, String button, String expectedPort, String configName,
                        DcMotorSimple.Direction direction) {
        DcMotorEx motor = robot.getHardwareMap().get(DcMotorEx.class, configName);
        motor.setDirection(direction);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER); // plain power, encoder still counts
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        return new Wheel(label, button, expectedPort, motor);
    }

    @Override
    public void init_loop() {
        telemetry.addLine("Robot on blocks, wheels off the floor.");
        telemetry.addLine("Hold X = front left, Y = front right, A = back left, B = back right.");
        telemetry.addLine("Only that wheel should spin, forward.");
    }

    @Override
    public void loop() {
        robot.clearBulkCache();
        run(frontLeft, gamepad1.x);
        run(frontRight, gamepad1.y);
        run(backLeft, gamepad1.a);
        run(backRight, gamepad1.b);

        telemetry.addLine("Hold X = FL, Y = FR, A = BL, B = BR");
        show(frontLeft);
        show(frontRight);
        show(backLeft);
        show(backRight);
    }

    private static void run(Wheel wheel, boolean held) {
        double power = held ? TEST_POWER : 0;
        if (wheel.power != power) { // only write on a change: fewer hardware calls
            wheel.power = power;
            wheel.motor.setPower(power);
        }
    }

    private void show(Wheel wheel) {
        // Power is what we sent; the encoder (bulk-read) confirms the wheel actually turned.
        telemetry.addData(wheel.label + " (" + wheel.button + ")", "power %.2f  encoder %d",
                wheel.power, wheel.motor.getCurrentPosition());
        telemetry.addData("   port", "%s (expected %s)",
                wheel.motor.getConnectionInfo(), wheel.expectedPort);
    }

    @Override
    public void stop() {
        for (Wheel wheel : new Wheel[] {frontLeft, frontRight, backLeft, backRight}) {
            if (wheel != null) {
                wheel.motor.setPower(0);
            }
        }
    }
}
