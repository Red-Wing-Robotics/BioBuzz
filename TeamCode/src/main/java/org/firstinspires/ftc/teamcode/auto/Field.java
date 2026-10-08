package org.firstinspires.ftc.teamcode.auto;

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
     * The HIVE structure's floor footprint (frame, rails, dampers): no part of a robot fits
     * there. The same for both alliances. Drive round it grown by the robot's reach. The auto maps
     * draw it (automaps.BiobuzzField).
     */
    public static final KeepOut HIVE = new KeepOut(fromImage(355), fromImage(348), fromImage(725), fromImage(732));

    /**
     * Inches for a position measured in the visualizer's BIOBUZZ image: x in pixels from its left
     * edge, or y in pixels from its bottom edge.
     */
    public static double fromImage(double px) {
        return px * SIZE / IMAGE_PX;
    }

    private Field() {}
}
