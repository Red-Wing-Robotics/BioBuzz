package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.follower.Follower;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.RobotHardware;
import org.firstinspires.ftc.teamcode.RobotId;

/**
 * PedroPathing 3.0 configuration — runtime source of truth for all tuned values.
 *
 * Both robots share the drivetrain and the Pinpoint setup here; what differs (pod offsets,
 * Foresight tuning) is in {@link Belteshazzar} and {@link Ashurbanipal}, picked by the hub's
 * {@link RobotId}. Re-run the tuners (see Tuning.java) on a robot whenever its drivetrain, pods,
 * or mass change, paste the results into that robot's file, and mirror them into docs/hardware.md.
 */
public final class Constants {

    private Constants() {}

    // ---- Drivetrain, both robots (directions verified with the Mecanum Tuner, 2026-09-20) ----
    public static final MecanumConfig drivetrainConfig = new MecanumConfig(c -> {
        c.frontLeftName.set(RobotHardware.FRONT_LEFT_DRIVE);
        c.frontRightName.set(RobotHardware.FRONT_RIGHT_DRIVE);
        c.backLeftName.set(RobotHardware.BACK_LEFT_DRIVE);
        c.backRightName.set(RobotHardware.BACK_RIGHT_DRIVE);
        c.frontLeftDirection.set(DcMotorSimple.Direction.REVERSE);
        c.backLeftDirection.set(DcMotorSimple.Direction.REVERSE);
        c.frontRightDirection.set(DcMotorSimple.Direction.FORWARD);
        c.backRightDirection.set(DcMotorSimple.Direction.FORWARD);
    });

    public static Mecanum createDrivetrain(HardwareMap hardwareMap) {
        return new Mecanum(hardwareMap, drivetrainConfig);
    }

    // ---- Localizer (goBILDA Pinpoint, Expansion Hub I2C bus 2) ------------------------
    private static final PinpointConfig BELTESHAZZAR_LOCALIZER =
            localizer(Belteshazzar.X_POD_OFFSET, Belteshazzar.Y_POD_OFFSET);
    private static final PinpointConfig ASHURBANIPAL_LOCALIZER =
            localizer(Ashurbanipal.X_POD_OFFSET, Ashurbanipal.Y_POD_OFFSET);

    private static PinpointConfig localizer(double xPodOffset, double yPodOffset) {
        return new PinpointConfig(c -> {
            c.name.set(RobotHardware.PINPOINT);
            c.xPodOffset.set(xPodOffset);
            c.yPodOffset.set(yPodOffset);
            c.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
            c.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.FORWARD);
            // TODO: confirm pod type (4-bar vs. swingarm).
            c.podType.set(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        });
    }

    public static PinpointConfig localizerConfig(RobotId robot) {
        return robot == RobotId.ASHURBANIPAL ? ASHURBANIPAL_LOCALIZER : BELTESHAZZAR_LOCALIZER;
    }

    /** For this hub's robot. Recalibrates the Pinpoint IMU (~0.5 s) — the robot must be still. */
    public static PinpointLocalizer createLocalizer(HardwareMap hardwareMap) {
        return createLocalizer(hardwareMap, RobotId.onHub());
    }

    public static PinpointLocalizer createLocalizer(HardwareMap hardwareMap, RobotId robot) {
        return new PinpointLocalizer(hardwareMap, localizerConfig(robot));
    }

    // ---- Foresight ----------------------------------------------------------------------
    public static ForesightConfig foresightConfig(RobotId robot) {
        return robot == RobotId.ASHURBANIPAL ? Ashurbanipal.FORESIGHT : Belteshazzar.FORESIGHT;
    }

    // ---- Follower -----------------------------------------------------------------------
    /** One Follower per OpMode. Constructing it recalibrates the Pinpoint IMU — keep still. */
    public static Follower create(HardwareMap hardwareMap, RobotId robot) {
        return new Follower(
                createLocalizer(hardwareMap, robot),
                createDrivetrain(hardwareMap),
                new Foresight(foresightConfig(robot)));
    }

    /** For the Driver Station at INIT: the robot, and whether its Pedro values are tuned. */
    public static String describe(RobotId robot) {
        boolean tuned = robot == RobotId.ASHURBANIPAL ? Ashurbanipal.TUNED : Belteshazzar.TUNED;
        return tuned ? robot.label : robot.label + " — PEDRO NOT TUNED (placeholder values)";
    }
}
