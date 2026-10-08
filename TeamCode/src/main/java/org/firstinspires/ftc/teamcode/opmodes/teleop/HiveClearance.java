package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.bylazar.configurables.annotations.Configurable;

/**
 * How far the robot's centre stays from the HIVE structure ({@code Field.HIVE}) when
 * CompTeleOp's assists drive somewhere: they go round the HIVE grown by this much instead of
 * through it. Inches.
 *
 * 21 = half the diagonal of an 18 × 18 in robot (12.7: the robot may be turned any way on the
 * path) + 8.3 for path-following and pose error. 15 (2.3 of margin) got the robot caught on the
 * HIVE. Revisit once the real robot size is measured. Panels edits apply on the next button press.
 */
@Configurable
public final class HiveClearance {

    public static double inches = 21;

    private HiveClearance() {}
}
