package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.math.Pose;

/**
 * Carries the robot's pose from the end of AUTO to the TeleOp that follows, so TeleOp starts
 * where the robot really is instead of at its own start waypoint. OpModes run in one app
 * process, so a static field survives from one OpMode to the next.
 *
 * A saved pose is used only for {@link #MAX_AGE_SECONDS} after AUTO stops. In a match TeleOp is
 * INIT'd during the 8 s transition; after that the robot may have been moved by hand.
 */
public final class PoseHandoff {

    public static final double MAX_AGE_SECONDS = 30;

    /** A pose AUTO saved, and how long ago. */
    public static final class Saved {
        public final Pose pose;
        public final double ageSeconds;

        Saved(Pose pose, double ageSeconds) {
            this.pose = pose;
            this.ageSeconds = ageSeconds;
        }
    }

    private static Pose pose;
    private static long savedAtNanos;

    private PoseHandoff() {}

    /** Called by {@link AutoOpMode} when AUTO stops. */
    public static void save(Pose pose) {
        save(pose, System.nanoTime());
    }

    /** The pose AUTO ended at, or null if none was saved in the last {@link #MAX_AGE_SECONDS}. */
    public static Saved recent() {
        return recent(System.nanoTime());
    }

    static synchronized void save(Pose newPose, long nowNanos) {
        pose = newPose;
        savedAtNanos = nowNanos;
    }

    static synchronized Saved recent(long nowNanos) {
        if (pose == null) {
            return null;
        }
        double age = (nowNanos - savedAtNanos) / 1e9;
        return age <= MAX_AGE_SECONDS ? new Saved(pose, age) : null;
    }

    static synchronized void clear() {
        pose = null;
    }
}
