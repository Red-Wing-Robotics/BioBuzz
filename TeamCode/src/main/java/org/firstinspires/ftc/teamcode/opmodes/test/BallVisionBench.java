package org.firstinspires.ftc.teamcode.opmodes.test;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.Field;
import org.firstinspires.ftc.teamcode.auto.Steps;
import org.firstinspires.ftc.teamcode.auto.Waypoint;
import org.firstinspires.ftc.teamcode.opmodes.teleop.HiveClearance;
import org.firstinspires.ftc.teamcode.subsystems.Vision;
import org.firstinspires.ftc.teamcode.subsystems.VisionTuning;
import org.firstinspires.ftc.teamcode.teleop.Assist;
import org.firstinspires.ftc.teamcode.teleop.Assists;
import org.firstinspires.ftc.teamcode.teleop.ControlScheme;
import org.firstinspires.ftc.teamcode.teleop.DriverCommands;
import org.firstinspires.ftc.teamcode.teleop.Resource;
import org.firstinspires.ftc.teamcode.teleop.TeleOpBase;
import org.firstinspires.ftc.teamcode.teleop.schemes.VisionBenchScheme;
import org.firstinspires.ftc.teamcode.vision.BallTracker;
import org.firstinspires.ftc.teamcode.vision.CameraMount;
import org.firstinspires.ftc.teamcode.vision.Detection;
import org.firstinspires.ftc.teamcode.vision.Frame;

import java.util.EnumSet;
import java.util.Locale;

/**
 * Checks the camera and calibrates its mount (procedure in this package's readme). Shows each
 * detection with where it puts the ball relative to the robot, the pitch the mount would need for
 * the ball to be at each check distance, and every tracked ball with its spread. Hold A to drive to
 * a ball (TeleOp's rules: no own-half limit); B forgets the tracked balls.
 */
@TeleOp(name = "Test: Ball Vision Bench", group = "test")
public class BallVisionBench extends TeleOpBase {

    /** Red half, audience end, front toward the blue wall: room ahead for balls, clear of the HIVE. */
    private static final Waypoint START = new Waypoint(12, 24, 0);
    /** Floor distances from the point under the lens to a ball straight ahead, for the pitch check. */
    private static final double[] CHECK_DISTANCES = {12, 18, 24};

    private Vision vision;

    @Override
    protected Alliance alliance() {
        return Alliance.RED;
    }

    @Override
    protected ControlScheme scheme() {
        return new VisionBenchScheme();
    }

    @Override
    protected Waypoint startWaypoint() {
        return START;
    }

    @Override
    protected boolean usePoseFromAuto() {
        return false;
    }

    @Override
    protected void onInit() {
        vision = add(new Vision(robot, follower::pose, alliance(), Vision.Job.BALLS));
    }

    @Override
    protected void registerAssists(Assists assists) {
        assists.define(Assist.TO_BALL, EnumSet.of(Resource.DRIVE),
                () -> vision.driveToBall(vision.teleOpSettings(Field.HIVE_FLOOR, HiveClearance.margin)));
        assists.define(Assist.FORGET_BALLS, EnumSet.noneOf(Resource.class),
                () -> Steps.instant("forget", vision::forget));
    }

    /** No mechanisms here: this is where the bench shows what the camera sees. */
    @Override
    protected void apply(DriverCommands commands) {
        CameraMount mount = VisionTuning.mount();
        Frame frame = vision.latestFrame();
        if (frame != null) {
            int n = 0;
            for (Detection d : frame.detections) {
                double[] r = mount.toRobot(d.txDeg, d.tyDeg, d.kind.centreHeight());
                String where = r == null ? "above the horizon"
                        : String.format(Locale.US, "%.1f in ahead, %.1f left", r[0], r[1]);
                String size = r == null || d.areaPct <= 0 ? ""
                        : String.format(Locale.US, "  area %.2f%% ≈ %.1f balls", d.areaPct,
                                d.areaPct / (mount.ballAreaPct(r[0], r[1], d.kind) * VisionTuning.blobAreaScale));
                telemetry.addData("seen " + (++n), "%s %.0f%%  tx %.1f  ty %.1f  → %s%s%s", d.kind, d.confidence * 100,
                        d.txDeg, d.tyDeg, where, size, mount.nearEdge(d.txDeg, d.tyDeg) ? "  (edge: not used)" : "");
                telemetry.addData("  pitch if", pitchCheck(d, mount));
            }
        }
        for (BallTracker.Ball b : vision.tracker().balls()) {
            telemetry.addData("ball " + b.id, "%s%s at (%.1f, %.1f)%s  seen %d×  spread %.1f in", b.kind,
                    b.blob() > 1 ? " ×" + b.blob() : "", b.x(), b.y(), b.confirmed() ? " sure" : "", b.sightings(), b.spread());
        }
    }

    /**
     * The pitch the mount would need for this ball's centre to be each check distance ahead of the
     * point under the lens: pitch = ty + atan((height − ball centre) / d), the docs/hardware.md check.
     */
    private static String pitchCheck(Detection d, CameraMount mount) {
        StringBuilder s = new StringBuilder();
        for (double distance : CHECK_DISTANCES) {
            double pitch = d.tyDeg + Math.toDegrees(Math.atan((mount.height - d.kind.centreHeight()) / distance));
            s.append(String.format(Locale.US, "%.0f in: %.1f°  ", distance, pitch));
        }
        return s.toString().trim();
    }
}
