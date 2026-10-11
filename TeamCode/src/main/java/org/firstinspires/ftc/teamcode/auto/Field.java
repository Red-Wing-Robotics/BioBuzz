package org.firstinspires.ftc.teamcode.auto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The field's numbers, in the coordinates defined in docs/coordinates.md: the one definition of
 * origin, axes, heading, size and the alliance mapping. Read it before writing a pose. Pure Java:
 * tools/routeview compiles it too.
 */
public final class Field {

    /**
     * Side of the square field, inches: Pedro's documented 144 (the visualizer uses 141.5, see
     * docs/coordinates.md). If our field measures differently inside the walls, change it here:
     * the BLUE rotation, the HIVE and the auto maps all follow.
     */
    public static final double SIZE = 144;

    /** The field centre is (CENTER, CENTER). BLUE poses are RED ones rotated 180° about it ({@link Alliance}). */
    public static final double CENTER = SIZE / 2;

    /** Side of the Pedro visualizer's BIOBUZZ image (/fields/biobuzz.webp), pixels. The image is the whole field. */
    public static final double IMAGE_PX = 1080;

    /**
     * The HIVE structure's outline seen from above (frame, CELLs, dampers). The same for both
     * alliances. Robots drive under most of it: only {@link #HIVE_FLOOR} reaches the floor. The
     * auto maps draw it and flag an auto that crosses it (automaps.BiobuzzField).
     */
    public static final KeepOut HIVE = new KeepOut(fromImage(355), fromImage(348), fromImage(725), fromImage(732));

    /**
     * The HIVE's red-side frame rail, on the floor along y. Measured in the visualizer image like
     * {@link #HIVE}: a PLACEHOLDER until measured on the real field.
     */
    public static final KeepOut HIVE_RAIL_RED = new KeepOut(fromImage(356), fromImage(390), fromImage(370), fromImage(688));

    /** The blue-side rail: the red one turned 180° about the centre. */
    public static final KeepOut HIVE_RAIL_BLUE = rotated(HIVE_RAIL_RED);

    /**
     * The HIVE's parts on the floor, which no part of a robot may touch. Between the two rails
     * (about 45 in) is a lane under the HIVE that robots drive through: nothing overhead comes low
     * enough to reach a robot, even as the HIVE tips.
     */
    public static final List<KeepOut> HIVE_FLOOR = Collections.unmodifiableList(Arrays.asList(HIVE_RAIL_RED, HIVE_RAIL_BLUE));

    /**
     * Inches for a position measured in the visualizer's BIOBUZZ image: x in pixels from its left
     * edge, or y in pixels from its bottom edge.
     */
    public static double fromImage(double px) {
        return px * SIZE / IMAGE_PX;
    }

    /** {@code k} turned 180° about the field centre, as BLUE poses are ({@link Alliance}). */
    private static KeepOut rotated(KeepOut k) {
        return new KeepOut(SIZE - k.maxX, SIZE - k.maxY, SIZE - k.minX, SIZE - k.minY);
    }

    private Field() {}
}
