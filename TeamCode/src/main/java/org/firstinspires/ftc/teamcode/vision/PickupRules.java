package org.firstinspires.ftc.teamcode.vision;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.Field;

/**
 * Which balls the robot may go after (RULES.md), checked before every vision drive and again as
 * the ball's position firms up. Positions are in the alliance frame (docs/coordinates.md).
 */
public final class PickupRules {

    /** Which colours a vision drive goes after. Opponent NECTAR is never one of them (G408). */
    public enum Colors {
        /** Yellow balls only. */
        POLLEN,
        /** Our alliance's NECTAR only. */
        OUR_NECTAR,
        /** POLLEN or our NECTAR. */
        OURS;

        public boolean wants(BallKind kind, Alliance alliance) {
            if (kind.isOpponentNectar(alliance)) {
                return false;
            }
            switch (this) {
                case POLLEN:
                    return kind == BallKind.POLLEN;
                case OUR_NECTAR:
                    return kind.isNectar();
                default:
                    return true;
            }
        }

        /** {@code "pollen"}, {@code "nectar"} or {@code "ours"}, as route args write it; null for anything else. */
        public static Colors parse(String text) {
            switch (text) {
                case "pollen":
                    return POLLEN;
                case "nectar":
                    return OUR_NECTAR;
                case "ours":
                    return OURS;
                default:
                    return null;
            }
        }
    }

    private PickupRules() {}

    /**
     * Why a ball of {@code kind} at (x, y) is not a target, or null if it is: opponent NECTAR
     * (G408); outside the walls; under the HIVE, whose outline is grown by {@code hiveMargin} (G409:
     * falling elements, no camping there); and in AUTO, on the other half (G402).
     */
    public static String whyNot(BallKind kind, double x, double y, Alliance alliance, boolean auto, double hiveMargin) {
        if (kind.isOpponentNectar(alliance)) {
            return "opponent NECTAR (G408)";
        }
        if (x < 0 || x > Field.SIZE || y < 0 || y > Field.SIZE) {
            return "outside the field";
        }
        if (Field.HIVE.grow(hiveMargin).contains(x, y)) {
            return "under the HIVE (G409)";
        }
        if (auto && !Field.onOwnHalf(alliance, x)) {
            return "on the other half (G402)";
        }
        return null;
    }
}
