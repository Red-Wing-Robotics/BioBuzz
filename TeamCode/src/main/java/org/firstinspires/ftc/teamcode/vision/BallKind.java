package org.firstinspires.ftc.teamcode.vision;

import org.firstinspires.ftc.teamcode.auto.Alliance;

/**
 * The balls Hive-Vision's model detects (its labels.txt, v1.1.0), with the sizes the geometry
 * needs. A detection is matched by its label first, so a model with its classes in another order
 * can't silently swap colours; the class id is the fallback when the label is empty.
 */
public enum BallKind {
    POLLEN("yellow_pollen", 0, 2.8),
    RED_NECTAR("red_nectar", 1, 3.6),
    BLUE_NECTAR("blue_nectar", 2, 3.6);

    /** The model's label for it. */
    public final String label;
    /** The model's class id for it. */
    public final int classId;
    /** Diameter, inches (RULES.md). Caliper the real balls: docs/hardware.md. */
    public final double diameter;

    BallKind(String label, int classId, double diameter) {
        this.label = label;
        this.classId = classId;
        this.diameter = diameter;
    }

    /** Height of the ball's centre above the tiles, inches: where a camera ray to it is cut. */
    public double centreHeight() {
        return diameter / 2;
    }

    public boolean isNectar() {
        return this != POLLEN;
    }

    /** NECTAR of the other alliance's colour, which the robot must never CONTROL (G408). */
    public boolean isOpponentNectar(Alliance alliance) {
        return this == (alliance == Alliance.RED ? BLUE_NECTAR : RED_NECTAR);
    }

    /**
     * The kind with this label, or, if the label is empty, this class id. Null for anything else:
     * a label this model doesn't have means it isn't one of our balls.
     */
    public static BallKind of(String label, int classId) {
        if (label != null && !label.isEmpty()) {
            for (BallKind k : values()) {
                if (k.label.equals(label)) {
                    return k;
                }
            }
            return null;
        }
        for (BallKind k : values()) {
            if (k.classId == classId) {
                return k;
            }
        }
        return null;
    }
}
