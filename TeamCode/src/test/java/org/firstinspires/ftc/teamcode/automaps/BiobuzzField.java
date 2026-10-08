package org.firstinspires.ftc.teamcode.automaps;

import static org.firstinspires.ftc.teamcode.auto.Field.fromImage;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.Field;
import org.firstinspires.ftc.teamcode.automaps.Geometry.Poly;

import java.util.Arrays;
import java.util.List;

/**
 * BIOBUZZ field elements for the auto maps, in field coordinates (docs/coordinates.md). Measured
 * in pixels in the Pedro visualizer's BIOBUZZ image (visualizer.pedropathing.com/fields/biobuzz.webp,
 * x from the left edge, y from the bottom edge) and converted with {@link Field#fromImage}, so they
 * follow {@link Field#SIZE}. Red elements first; blue ones are the 180° rotation. Keep in step with
 * docs/field.md.
 */
final class BiobuzzField {

    static final double SIZE = Field.SIZE;
    static final double CENTER = Field.CENTER;
    static final double TILE = SIZE / 6;

    static final Poly RED_LOADING_ZONE = rect(0, 721, 88, 898);
    static final Poly BLUE_LOADING_ZONE = RED_LOADING_ZONE.rotated(SIZE);

    static final Poly RED_GARDEN = rect(0, 0, 178, 20);
    static final Poly BLUE_GARDEN = RED_GARDEN.rotated(SIZE);

    /** Hexagons on the perimeter wall. FLOWERS belong to whoever owns them, not to an alliance. */
    static final double FLOWER_RADIUS = fromImage(22);
    static final List<Poly> FLOWERS = Arrays.asList(
            hexagon(18, 360),
            hexagon(360, 1062),
            hexagon(Field.IMAGE_PX - 18, Field.IMAGE_PX - 360),
            hexagon(Field.IMAGE_PX - 360, Field.IMAGE_PX - 1062));

    /** Everything the HIVE structure covers on the floor: robots can't drive here. It's {@link Field#HIVE}, which TeleOp assists drive round. */
    static final Poly HIVE_STRUCTURE = Poly.rect(Field.HIVE.minX, Field.HIVE.minY, Field.HIVE.maxX, Field.HIVE.maxY);
    static final Poly FRAME_RAIL_RED = rect(356, 390, 370, 688);
    static final Poly FRAME_RAIL_BLUE = FRAME_RAIL_RED.rotated(SIZE);
    static final Poly PIVOT_BEAM = rect(432, 533, 648, 547);
    static final Poly DAMPER_HIGH = rect(451, 553, 629, 571);
    static final Poly DAMPER_LOW = DAMPER_HIGH.rotated(SIZE);

    /**
     * Each alliance's HIVE has a CELL at each end. At the start of a match the CELL holding three
     * NECTAR faces up (RULES.md §3): the lower one for red, the upper one for blue.
     */
    static final Poly RED_CELL_UP_AT_START = rect(366, 393, 518, 526);
    static final Poly RED_CELL_DOWN_AT_START = rect(366, 595, 518, 730);
    static final Poly BLUE_CELL_UP_AT_START = RED_CELL_UP_AT_START.rotated(SIZE);
    static final Poly BLUE_CELL_DOWN_AT_START = RED_CELL_DOWN_AT_START.rotated(SIZE);
    static final double[][] RED_START_NECTAR = {
            {fromImage(384), fromImage(466)}, {fromImage(410), fromImage(466)}, {fromImage(436), fromImage(466)}};

    static Poly loadingZone(Alliance alliance) {
        return alliance == Alliance.RED ? RED_LOADING_ZONE : BLUE_LOADING_ZONE;
    }

    /** True if x is on {@code alliance}'s half (red owns columns A–C, x below the centre line). */
    static boolean onOwnHalf(Alliance alliance, double x) {
        return alliance == Alliance.RED ? x <= CENTER : x >= CENTER;
    }

    /** A rectangle given by its corners in image pixels. */
    private static Poly rect(double x0, double y0, double x1, double y1) {
        return Poly.rect(fromImage(x0), fromImage(y0), fromImage(x1), fromImage(y1));
    }

    private static Poly hexagon(double cx, double cy) {
        return Poly.regular(fromImage(cx), fromImage(cy), FLOWER_RADIUS, 6, 0);
    }

    private BiobuzzField() {}
}
