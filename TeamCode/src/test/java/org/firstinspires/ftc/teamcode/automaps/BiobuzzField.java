package org.firstinspires.ftc.teamcode.automaps;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.automaps.Geometry.Poly;

import java.util.Arrays;
import java.util.List;

/**
 * BIOBUZZ field elements in the Pedro Pathing visualizer's frame: inches, origin at the corner by
 * the red GARDEN, +x from the red alliance wall toward the blue one, +y from the audience toward
 * the far wall, field 141.5 in square. Measured from the visualizer's BIOBUZZ image
 * (visualizer.pedropathing.com/fields/biobuzz.webp, 1080 px = 141.5 in), red elements first; blue
 * ones are the 180° rotation. Keep in step with docs/field.md.
 */
final class BiobuzzField {

    static final double SIZE = 141.5;
    static final double CENTER = SIZE / 2;
    static final double TILE = SIZE / 6;

    static final Poly RED_LOADING_ZONE = Poly.rect(0, 94.5, 11.5, 117.7);
    static final Poly BLUE_LOADING_ZONE = RED_LOADING_ZONE.rotated(SIZE);

    static final Poly RED_GARDEN = Poly.rect(0, 0, 23.3, 2.6);
    static final Poly BLUE_GARDEN = RED_GARDEN.rotated(SIZE);

    /** Hexagons on the perimeter wall. FLOWERS belong to whoever owns them, not to an alliance. */
    static final double FLOWER_RADIUS = 2.9;
    static final List<Poly> FLOWERS = Arrays.asList(
            Poly.regular(2.4, 47.2, FLOWER_RADIUS, 6, 0),
            Poly.regular(47.2, 139.1, FLOWER_RADIUS, 6, 0),
            Poly.regular(SIZE - 2.4, SIZE - 47.2, FLOWER_RADIUS, 6, 0),
            Poly.regular(SIZE - 47.2, SIZE - 139.1, FLOWER_RADIUS, 6, 0));

    /** Everything the HIVE structure covers on the floor: robots can't drive here. */
    static final Poly HIVE_STRUCTURE = Poly.rect(46.5, 45.6, 95.0, 95.9);
    static final Poly FRAME_RAIL_RED = Poly.rect(46.6, 51.1, 48.5, 90.1);
    static final Poly FRAME_RAIL_BLUE = FRAME_RAIL_RED.rotated(SIZE);
    static final Poly PIVOT_BEAM = Poly.rect(56.6, 69.8, 84.9, 71.7);
    static final Poly DAMPER_HIGH = Poly.rect(59.1, 72.5, 82.4, 74.8);
    static final Poly DAMPER_LOW = DAMPER_HIGH.rotated(SIZE);

    /**
     * Each alliance's HIVE has a CELL at each end. At the start of a match the CELL holding three
     * NECTAR faces up (RULES.md §3): the lower one for red, the upper one for blue.
     */
    static final Poly RED_CELL_UP_AT_START = Poly.rect(48.0, 51.5, 67.9, 68.9);
    static final Poly RED_CELL_DOWN_AT_START = Poly.rect(48.0, 78.0, 67.9, 95.6);
    static final Poly BLUE_CELL_UP_AT_START = RED_CELL_UP_AT_START.rotated(SIZE);
    static final Poly BLUE_CELL_DOWN_AT_START = RED_CELL_DOWN_AT_START.rotated(SIZE);
    static final double[][] RED_START_NECTAR = {{50.3, 61.0}, {53.7, 61.0}, {57.1, 61.0}};

    static Poly loadingZone(Alliance alliance) {
        return alliance == Alliance.RED ? RED_LOADING_ZONE : BLUE_LOADING_ZONE;
    }

    /** True if x is on {@code alliance}'s half (red owns columns A–C, x below the centre line). */
    static boolean onOwnHalf(Alliance alliance, double x) {
        return alliance == Alliance.RED ? x <= CENTER : x >= CENTER;
    }

    private BiobuzzField() {}
}
