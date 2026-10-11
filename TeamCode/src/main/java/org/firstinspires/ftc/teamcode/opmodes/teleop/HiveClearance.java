package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.bylazar.configurables.annotations.Configurable;

/**
 * How close CompTeleOp's driving assists let any part of the robot come to the HIVE's floor parts
 * ({@code Field.HIVE_FLOOR}), inches. They take the quickest way that keeps this gap, round the
 * HIVE or through the lane under it ({@code DrivePlan}).
 *
 * 8.3: the gap that 21 in from the robot's centre left an 18 × 18 robot turned 45°, which stopped
 * it getting caught on the HIVE (2026-10-08). Raise it if the robot clips a rail. For an 18 in
 * wide robot the lane closes above 13.6, and every assist then goes round. Panels edits apply on
 * the next button press.
 */
@Configurable
public final class HiveClearance {

    public static double margin = 8.3;

    private HiveClearance() {}
}
