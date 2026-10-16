package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;

/** Belteshazzar's Pedro values (the first robot). Tuned 2026-09-20. Shared values are in {@link Constants}. */
final class Belteshazzar {

    private Belteshazzar() {}

    static final boolean TUNED = true;

    // ---- Pinpoint pod offsets ---------------------------------------------------------------
    // Relative to the robot's tracking center, in inches (x = forward pod, y = strafe pod).
    // Measured 2026-09-20; written as inches + 32nds as measured.
    static final double X_POD_OFFSET = -4.0 - 23.0 / 32.0;
    static final double Y_POD_OFFSET = -2.0 - 11.0 / 32.0;

    // ---- Foresight (from the Foresight Tuner, 2026-09-20) ------------------------------------
    static final ForesightConfig FORESIGHT = new ForesightConfig(c -> {
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
}
