package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.bylazar.configurables.annotations.Configurable;

import org.firstinspires.ftc.teamcode.auto.Waypoint;

/**
 * Where CompTeleOp's shooting-spot assists drive to: one on each side of the HIVE, so the robot
 * can shoot at whichever CELL faces up. RED frame, inches and degrees, mirrored for BLUE. "Left"
 * and "right" are as the drivers see them from their alliance area; the 180° mirror keeps them on
 * the same hand for BLUE. Heading is the way the robot's front faces: set it so the launcher
 * points at the CELL.
 *
 * PLACEHOLDERS, not measured. To tune, run Red: TeleOp in a dev build: drive to a spot where
 * shots land, read the DS "pose" line, and enter it in Panels (http://192.168.43.1:8001 →
 * Configurables). Each button press reads the current values, so the next press tests the edit.
 * Copy final values back here; Panels edits are not saved.
 */
@Configurable
public final class ShootingSpots {

    /** Drivers' left of the HIVE (higher y in the RED frame), facing the HIVE centre. */
    public static Waypoint left = new Waypoint(58, 23, 0);
    /** Drivers' right of the HIVE (lower y in the RED frame), facing the HIVE centre. */
    public static Waypoint right = new Waypoint(58, 117, 90);

    private ShootingSpots() {}
}
