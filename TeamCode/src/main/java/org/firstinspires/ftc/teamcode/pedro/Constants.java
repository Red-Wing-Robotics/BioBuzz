package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.PinpointConfig;
import com.pedropathing.revhub.localizers.PinpointLocalizer;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.RobotHardware;

/**
 * PedroPathing 3.0 configuration — runtime source of truth for all tuned values.
 *
 * Re-run the tuners (see Tuning.java) whenever the drivetrain, pods, or robot mass change, and
 * mirror the numbers into docs/hardware.md. Tuned 2026-09-20.
 */
public final class Constants {

    private Constants() {}

    // ---- Drivetrain (directions verified with the Mecanum Tuner, 2026-09-20) --------------
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
    public static final PinpointConfig localizerConfig = new PinpointConfig(c -> {
        c.name.set(RobotHardware.PINPOINT);
        // Pod positions relative to the robot's tracking center, in inches (x = forward pod,
        // y = strafe pod). Measured 2026-09-20; written as inches + 32nds as measured.
        c.xPodOffset.set(-4.0 - 23.0 / 32.0);
        c.yPodOffset.set(-2.0 - 11.0 / 32.0);
        c.xPodDirection.set(GoBildaPinpointDriver.EncoderDirection.REVERSED);
        c.yPodDirection.set(GoBildaPinpointDriver.EncoderDirection.FORWARD);
        // TODO: confirm pod type (4-bar vs. swingarm).
        c.podType.set(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
    });

    /** Constructing this recalibrates the Pinpoint IMU (~0.5 s) — the robot must be still. */
    public static PinpointLocalizer createLocalizer(HardwareMap hardwareMap) {

        return new PinpointLocalizer(hardwareMap, localizerConfig);
    }

    // ---- Foresight (from the Foresight Tuner, 2026-09-20) ------------------------------
    public static final ForesightConfig foresightConfig = new ForesightConfig(c -> {
        Controller primaryTranslationalForward = Controller.proportional(0.14103543776948207);
        Controller secondaryTranslationalForward = Controller.proportional(0.05210882571952031);
        Controller primaryTranslationalLateral = Controller.proportional(0.20738696920139088);
        Controller secondaryTranslationalLateral = Controller.proportional(0.07662394363803793);

        c.forwardTranslational.set(Controller.piecewise(secondaryTranslationalForward).put(2.5, primaryTranslationalForward));
        c.strafeTranslational.set(Controller.piecewise(secondaryTranslationalLateral).put(2.5, primaryTranslationalLateral));

        c.coast.set(Controller.proportionalFeedforward(0.015097332028744338));
        c.brake.set(Controller.proportionalFeedforward(0.012832732224432687));

        c.headingFeedback.set(Controller.proportional(3.148080136406957));
        c.headingBrakeCoefficients.set(Vector2D.cartesian(0.040507150506330435, 0.007241973381428488));

        c.linearBrakeCoefficients.set(Matrix.diag(0.0660279446532025, 0.0669220937164809));
        c.quadraticBrakeCoefficients.set(Matrix.diag(0.0010079693007956328, 8.537729592257382E-4));

        c.maxAchievableForwardVelocity.set(69.64886002787485);
        c.maxAchievableStrafeVelocity.set(59.05690003315992);
        c.naturalForwardDeceleration.set(37.22672207181109);
        c.naturalStrafeDeceleration.set(51.94072519855322);
    });

    // ---- Follower -----------------------------------------------------------------------
    /** One Follower per OpMode. Constructing it recalibrates the Pinpoint IMU — keep still. */
    public static Follower create(HardwareMap hardwareMap) {
        return new Follower(
                createLocalizer(hardwareMap),
                createDrivetrain(hardwareMap),
                new Foresight(foresightConfig));
    }
}
