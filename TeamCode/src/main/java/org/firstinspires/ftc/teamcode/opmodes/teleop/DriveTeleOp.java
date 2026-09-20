package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.RobotHardware;
import org.firstinspires.ftc.teamcode.pedro.Constants;
import org.firstinspires.ftc.teamcode.util.MathUtil;

/**
 * Robot-centric mecanum driving through the PedroPathing Follower.
 *
 * Gamepad 1:
 *   left stick      forward / strafe (relative to the robot's front)
 *   right stick X   turn
 *   right bumper    hold for slow mode
 *
 * Robot-centric by driver preference (see docs/decisions.md). The Follower still tracks the
 * Pinpoint pose so telemetry shows where the robot thinks it is.
 */
@TeleOp(name = "Drive TeleOp", group = "comp")
public class DriveTeleOp extends OpMode {

    private static final double STICK_DEADBAND = 0.05;
    private static final double SLOW_MODE_SCALE = 0.35;

    private RobotHardware robot;
    private Follower follower;
    private long loopCount;

    @Override
    public void init() {
        robot = new RobotHardware(hardwareMap);
        follower = Constants.create(hardwareMap); // recalibrates the Pinpoint IMU: keep the robot still
        telemetry.addLine("Drive TeleOp ready");
    }

    @Override
    public void loop() {
        robot.clearBulkCache();

        // Pedro convention: +forward = ahead, +strafe = left, +turn = counter-clockwise.
        double forward = MathUtil.deadband(-gamepad1.left_stick_y, STICK_DEADBAND);
        double strafe  = MathUtil.deadband(-gamepad1.left_stick_x, STICK_DEADBAND);
        double turn    = MathUtil.deadband(-gamepad1.right_stick_x, STICK_DEADBAND);
        double scale = gamepad1.right_bumper ? SLOW_MODE_SCALE : 1.0;

        follower.manual(new DrivePowers(forward * scale, strafe * scale, turn * scale));
        follower.update(); // reads the localizer, then applies the manual powers

        Pose pose = follower.pose();
        telemetry.addData("pose", "x %.1f  y %.1f  heading %.0f°",
                pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        telemetry.addData("slow", gamepad1.right_bumper);
        // Diagnostics: each line is one link in the stick -> motor chain.
        telemetry.addData("raw sticks", "ly %.2f  lx %.2f  rx %.2f",
                gamepad1.left_stick_y, gamepad1.left_stick_x, gamepad1.right_stick_x);
        telemetry.addData("powers", "fwd %.2f  strafe %.2f  turn %.2f", forward, strafe, turn);
        telemetry.addData("follower mode", follower.mode());
        telemetry.addData("wheels", follower.drivetrain.debug());
        telemetry.addData("loops", ++loopCount);
    }

    @Override
    public void stop() {
        follower.stop();
    }
}
