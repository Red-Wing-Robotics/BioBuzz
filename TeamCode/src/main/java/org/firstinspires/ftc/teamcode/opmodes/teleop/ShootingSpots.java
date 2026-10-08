package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.bylazar.configurables.annotations.Configurable;

import org.firstinspires.ftc.teamcode.auto.Waypoint;

/**
 * Where CompTeleOp's shooting-spot assists drive to: one off each end of our HIVE, in line with
 * its CELLs (x ≈ 49–69), so the robot can shoot at whichever CELL faces up. RED frame, mapped for
 * BLUE (docs/coordinates.md, which also says which way is the drivers' left; the BLUE rotation
 * keeps each spot on the same hand). The launcher fires out the front, so the heading points the
 * front at the HIVE.
 *
 * The assists drive round the HIVE ({@link HiveClearance}). A spot closer to the HIVE than that
 * clearance is fine: the robot goes round, then straight in from the nearest side.
 *
 * PLACEHOLDERS, not measured. To tune, run Red: TeleOp in a dev build: drive to a spot where
 * shots land, read the DS "pose" line, and enter it in Panels (http://192.168.43.1:8001 →
 * Configurables). Each button press reads the current values, so the next press tests the edit.
 * Copy final values back here and into docs/field.md; Panels edits are not saved.
 */
@Configurable
public final class ShootingSpots {

    /** Drivers' left: off the far end of the HIVE (higher y in the RED frame), facing it (−90°, toward the audience). */
    public static Waypoint left = new Waypoint(58, 117, -90);
    /** Drivers' right: off the audience end of the HIVE (lower y in the RED frame), facing it (90°, toward the far wall). */
    public static Waypoint right = new Waypoint(58, 23, 90);

    private ShootingSpots() {}
}
