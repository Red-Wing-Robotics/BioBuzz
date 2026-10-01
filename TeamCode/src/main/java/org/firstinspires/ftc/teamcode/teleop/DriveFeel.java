package org.firstinspires.ftc.teamcode.teleop;

import com.bylazar.configurables.annotations.Configurable;

import org.firstinspires.ftc.teamcode.util.MathUtil;

/**
 * How the drive sticks feel, shared by every scheme. Live-editable in Panels (dev builds,
 * http://192.168.43.1:8001 → Configurables); copy values the drivers like back here — Panels
 * edits are not saved. Competition builds use the values below.
 */
@Configurable
public final class DriveFeel {

    /** Stick travel ignored around center, so stick drift doesn't creep the robot. */
    public static double deadband = 0.05;
    /** Response curve: 1 = linear; 2 = squared (finer control near center, still full power at the edge). */
    public static double exponent = 1.0;
    /** Drive power multiplier while slow mode is held. */
    public static double slowScale = 0.35;

    private DriveFeel() {}

    /** Deadband, then the response curve. Keeps the sign; ±1 in gives ±1 out. */
    public static double shape(double stick) {
        double value = MathUtil.deadband(stick, deadband);
        if (value == 0.0) {
            return 0.0; // pow(0, 0) is 1: don't let exponent 0 drive a centered stick
        }
        return Math.copySign(Math.pow(Math.abs(value), exponent), value);
    }
}
