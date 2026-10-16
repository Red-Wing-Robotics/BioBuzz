package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.bylazar.configurables.annotations.Configurable;

import org.firstinspires.ftc.teamcode.auto.Field;
import org.firstinspires.ftc.teamcode.auto.Waypoint;

/**
 * The centres of our HIVE's two CELLs, seen from above, which CompTeleOp's heading aim
 * ({@code AimTarget}) points the robot's front at. RED frame, mapped for BLUE
 * (docs/coordinates.md); the BLUE rotation keeps each CELL on the same hand of the drivers.
 * Heading is unused.
 *
 * From the visualizer image, the same rectangles the auto maps draw (automaps.BiobuzzField
 * RED_CELL_UP_AT_START / _DOWN_AT_START): x 366–518 px, y 393–526 px and 595–730 px.
 * PLACEHOLDERS until measured on the field; live-editable in Panels, read every loop.
 */
@Configurable
public final class HiveCells {

    /** Drivers' left: the far-wall end of the HIVE, down at the start of the match. */
    public static Waypoint left = new Waypoint(Field.fromImage((366 + 518) / 2.0), Field.fromImage((595 + 730) / 2.0), 0);
    /** Drivers' right: the audience end of the HIVE, up at the start of the match (holds the starting NECTAR). */
    public static Waypoint right = new Waypoint(Field.fromImage((366 + 518) / 2.0), Field.fromImage((393 + 526) / 2.0), 0);

    private HiveCells() {}
}
