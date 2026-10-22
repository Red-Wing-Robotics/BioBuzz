package org.firstinspires.ftc.teamcode.subsystems;

import com.bylazar.configurables.annotations.Configurable;

import org.firstinspires.ftc.teamcode.vision.BallApproach;
import org.firstinspires.ftc.teamcode.vision.BallTracker;
import org.firstinspires.ftc.teamcode.vision.CameraMount;
import org.firstinspires.ftc.teamcode.vision.DriveToBall;

/**
 * Every number {@link Vision} uses, live-editable in Panels (dev builds,
 * http://192.168.43.1:8001 → Configurables). The mount and tracking values apply to the next
 * camera frame; the approach values to the next vision drive a route or assist builds, which for
 * a route means the next INIT. Copy values that work back here: Panels edits are not saved.
 */
@Configurable
public final class VisionTuning {

    // ---- the mount: docs/hardware.md "Limelight 3A mount", measured 2026-10-15 --------------
    /** Lens ahead of the tracking centre, inches. */
    public static double forward = 6.75;
    /** Lens left of the tracking centre, inches. */
    public static double left = 6.125;
    /** Lens centre above the tiles, inches. */
    public static double height = 7.75;
    /** Lens axis below horizontal, degrees. Check it optically (docs/hardware.md checklist). */
    public static double pitchDown = 33.3;
    /** Lens axis turned left of straight ahead, degrees. */
    public static double yaw = 0;
    /** Limelight 3A field of view, degrees (spec). */
    public static double hfov = 54.5;
    public static double vfov = 42;
    /** Detections this close to an image edge, degrees, are ignored. */
    public static double edgeMargin = 2;

    // ---- the camera ------------------------------------------------------------------------
    /** Pipeline slots on the Limelight. PLACEHOLDERS until the pipelines are set up (docs/hardware.md). */
    public static int ballPipeline = 0;
    public static int tagPipeline = 1;
    /** How often the SDK asks the camera for results. About twice the model's frame rate. */
    public static int pollRateHz = 50;
    /** Latency not in the camera's own figures, ms. Tune with the vision bench's spin test. */
    public static double extraMs = 0;

    // ---- tracking (BallTracker.Settings) ---------------------------------------------------
    public static double minConfidence = 0.44;
    public static int confirmFrames = 3;
    public static double matchRadius = 4;
    public static double maxRange = 40;
    public static double maxTurnRateDeg = 120;
    public static double lostMs = 400;
    public static double memoryMs = 3000;
    public static double wallTolerance = 4;
    /** One ball's box area over the size worked out from the mount. Calibrate on the vision bench: one ball, read "≈". */
    public static double blobAreaScale = 1.0;
    public static int maxBlob = 6;
    /** The confidence a box about two balls big or more needs (a clump scores lower than one ball). */
    public static double blobMinConfidence = 0.25;
    public static double blobBalls = 1.6;

    // ---- vision drives (DriveToBall.Settings) ----------------------------------------------
    /** Only balls this close to the tracking centre, inches. The camera sees about 36 in. */
    public static double within = 36;
    /**
     * Where to stop: tracking centre to ball centre, inches. 9 = half the robot's length, so the
     * front edge stops at the ball's centre, touching it (2026-10-22; it was 15).
     */
    public static double standoff = 9;
    /** How long a vision drive waits for a ball to be confirmed, seconds. Three frames take 0.15–0.3 s. */
    public static double lookSeconds = 1.0;
    public static double commit = 18;
    public static double replanMove = 2;
    public static double replanEveryMs = 250;
    /** Prefer a group of balls about as near as the nearest ball. A route can say {@code "prefer": "nearest"}. */
    public static boolean preferGroups = true;
    /** Balls within this of each other, inches, count as one group: touching, or nearly (2026-10-22; was 8). */
    public static double clusterRadius = 6;
    /** A bigger group wins if it is at most this much slower to reach than the nearest ball, seconds (0.5 s ≈ 15–20 in). */
    public static double similarSeconds = 0.5;
    /** The intake takes balls in at the back, not the front. Unknown until the intake is built. */
    public static boolean intakeAtBack = false;

    private VisionTuning() {}

    public static CameraMount mount() {
        return new CameraMount(forward, left, height, pitchDown, yaw, hfov, vfov, edgeMargin);
    }

    /** Copies the tracking values into {@code s}. */
    public static void apply(BallTracker.Settings s) {
        s.minConfidence = minConfidence;
        s.confirmFrames = confirmFrames;
        s.matchRadius = matchRadius;
        s.maxRange = maxRange;
        s.maxTurnRateDeg = maxTurnRateDeg;
        s.lostMs = lostMs;
        s.memoryMs = memoryMs;
        s.wallTolerance = wallTolerance;
        s.blobAreaScale = blobAreaScale;
        s.maxBlob = maxBlob;
        s.blobMinConfidence = blobMinConfidence;
        s.blobBalls = blobBalls;
    }

    /** Vision-drive settings from these values. The caller sets the rules and obstacles. */
    public static DriveToBall.Settings approach() {
        DriveToBall.Settings s = new DriveToBall.Settings();
        s.within = within;
        s.standoff = standoff;
        s.lookSeconds = lookSeconds;
        s.commit = commit;
        s.replanMove = replanMove;
        s.replanEveryMs = replanEveryMs;
        s.preferGroups = preferGroups;
        s.clusterRadius = clusterRadius;
        s.similarSeconds = similarSeconds;
        s.side = intakeAtBack ? BallApproach.Side.BACK : BallApproach.Side.FRONT;
        return s;
    }
}
