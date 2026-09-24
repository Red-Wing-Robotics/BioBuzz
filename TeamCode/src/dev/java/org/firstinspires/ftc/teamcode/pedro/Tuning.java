package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.tuning.autotune.Procedure;
import com.pedropathing.tuning.autotune.Tuner;

import org.firstinspires.ftc.teamcode.pedro.procedures.ForesightTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.MecanumTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.PinpointTuner;
import org.firstinspires.ftc.teamcode.pedro.procedures.Tests;

/**
 * Registers Pedro tuning procedures.
 *
 * The tuning library scans TeamCode for static, no-argument methods annotated {@code @Tuner}
 * that return a {@link Procedure}, and lists them in a web UI served by the Robot Controller:
 *
 *   connect to the Control Hub's Wi-Fi, then open   http://192.168.43.1:10158
 *
 * Tuners are NOT Driver Station OpModes — the web UI launches them itself. Run them in order
 * (Mecanum → Pinpoint → Foresight → Tests) and paste each result into Constants.java.
 */
public final class Tuning {

    private Tuning() {}

    @Tuner
    public static Procedure mecanumTuner() {
        return new MecanumTuner();
    }

    @Tuner
    public static Procedure pinpointTuner() {
        return new PinpointTuner();
    }

    @Tuner
    public static Procedure foresightTuner() {
        return new ForesightTuner(Constants::createLocalizer, Constants::createDrivetrain);
    }

    /** Hold / line / path tests against the tuned Follower. */
    @Tuner
    public static Procedure tests() {
        return new Tests(
                Constants::createDrivetrain,
                Constants::createLocalizer,
                () -> new Foresight(Constants.foresightConfig));
    }
}
