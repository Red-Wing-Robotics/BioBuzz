package org.firstinspires.ftc.teamcode.util;

/** Hardware-free math helpers. Keep everything here unit-testable. */
public final class MathUtil {

    private MathUtil() {}

    /**
     * Returns 0 when {@code |value| < threshold}, otherwise rescales the remaining range so the
     * output is still continuous and reaches ±1 at full stick deflection.
     */
    public static double deadband(double value, double threshold) {
        if (Math.abs(value) < threshold) {
            return 0.0;
        }
        double sign = Math.signum(value);
        return sign * (Math.abs(value) - threshold) / (1.0 - threshold);
    }

    /** {@code value} limited to {@code min}–{@code max}. */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
