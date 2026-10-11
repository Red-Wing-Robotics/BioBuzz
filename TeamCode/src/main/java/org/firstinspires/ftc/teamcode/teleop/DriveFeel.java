package org.firstinspires.ftc.teamcode.teleop;

import com.bylazar.configurables.annotations.Configurable;

import org.firstinspires.ftc.teamcode.util.MathUtil;

/**
 * How the drive sticks feel, shared by every scheme. Live-editable in Panels (dev builds,
 * http://192.168.43.1:8001 → Configurables); copy values the drivers like back here — Panels
 * edits are not saved. Competition builds use the values below.
 *
 * Panels accepts any number, so each value is used within a sane range: a deadband of 1 would
 * divide by zero at full stick, and an exponent of 0 would make any touch full power.
 */
@Configurable
public final class DriveFeel {

    /** Stick travel ignored around center, so stick drift doesn't creep the robot. Used within 0–{@value #MAX_DEADBAND}. */
    public static double deadband = 0.05;
    /**
     * Response curve: 1 = linear; 2 = squared (finer control near center, still full power at the
     * edge). Used within {@value #MIN_EXPONENT}–{@value #MAX_EXPONENT}.
     */
    public static double exponent = 1.0;
    /** Drive power multiplier while slow mode is held. Used within 0–1: read it with {@link #slowModeScale()}. */
    public static double slowScale = 0.35;

    static final double MAX_DEADBAND = 0.5;
    static final double MIN_EXPONENT = 0.25;
    static final double MAX_EXPONENT = 4;

    private DriveFeel() {}

    /** Deadband, then the response curve. Keeps the sign; ±1 in gives ±1 out. */
    public static double shape(double stick) {
        double value = MathUtil.deadband(stick, MathUtil.clamp(deadband, 0, MAX_DEADBAND));
        if (value == 0.0) {
            return 0.0;
        }
        return Math.copySign(Math.pow(Math.abs(value), MathUtil.clamp(exponent, MIN_EXPONENT, MAX_EXPONENT)), value);
    }

    /** {@link #slowScale}, within 0–1, so a bad edit can't speed slow mode up or reverse it. */
    public static double slowModeScale() {
        return MathUtil.clamp(slowScale, 0, 1);
    }
}
