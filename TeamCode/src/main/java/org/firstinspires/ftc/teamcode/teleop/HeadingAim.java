package org.firstinspires.ftc.teamcode.teleop;

import com.bylazar.configurables.annotations.Configurable;
import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.util.MathUtil;

/**
 * Turns the robot so its front (the launcher) keeps pointing at a field point while the driver
 * drives and strafes: a PD controller on the heading error, giving a turn power in Pedro's
 * convention (+ counter-clockwise). Hardware-free; {@link TeleOpBase} feeds it the Follower's
 * pose every loop an {@link AimTarget} is engaged.
 *
 * Gains are live-editable in Panels (dev builds, http://192.168.43.1:8001 → Configurables); copy
 * values that work back here. PLACEHOLDERS, not tuned on the robot. Tune kP up until the robot
 * snaps to the target without oscillating, then add kD if it overshoots.
 */
@Configurable
public final class HeadingAim {

    /** Turn power per radian of heading error. */
    public static double kP = 1.0;
    /** Turn power per radian/second of change in the heading error: damps overshoot. */
    public static double kD = 0.05;
    /** Largest turn power the aim may use. Used within 0–1. */
    public static double maxTurn = 0.8;
    /** Closer than this to the target, inches, the bearing swings wildly, so the aim stops turning. */
    public static double minDistance = 6;

    private double lastError;
    private boolean hasLast;

    /**
     * Turn power that points the robot's front at ({@code targetX}, {@code targetY}), alliance
     * frame. {@code dtSeconds} is the time since the last call, for the derivative term.
     */
    public double turn(Pose robot, double targetX, double targetY, double dtSeconds) {
        double dx = targetX - robot.x();
        double dy = targetY - robot.y();
        if (Math.hypot(dx, dy) < minDistance) {
            reset();
            return 0;
        }
        double error = wrap(Math.atan2(dy, dx) - robot.heading());
        double rate = hasLast && dtSeconds > 0 ? wrap(error - lastError) / dtSeconds : 0;
        lastError = error;
        hasLast = true;
        double limit = MathUtil.clamp(maxTurn, 0, 1);
        return MathUtil.clamp(kP * error + kD * rate, -limit, limit);
    }

    /** Heading error at the last {@link #turn} call, radians, + meaning the target is counter-clockwise. 0 if none. */
    public double error() {
        return hasLast ? lastError : 0;
    }

    /** Forget the last error, so the next aim starts without a derivative kick. Call whenever aiming stops. */
    public void reset() {
        hasLast = false;
        lastError = 0;
    }

    /** {@code radians} wrapped into −π..π. */
    static double wrap(double radians) {
        double r = radians % (2 * Math.PI);
        if (r > Math.PI) {
            r -= 2 * Math.PI;
        } else if (r < -Math.PI) {
            r += 2 * Math.PI;
        }
        return r;
    }
}
