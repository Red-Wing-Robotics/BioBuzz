package org.firstinspires.ftc.teamcode.vision;

/**
 * One ball the camera reported in one frame. The angles are measured from the image centre, not
 * the Limelight's crosshair, so moving the crosshair in its web UI can't shift every distance.
 */
public final class Detection {

    public final BallKind kind;
    /** Degrees, + to the right of the image centre. */
    public final double txDeg;
    /** Degrees, + above the image centre. */
    public final double tyDeg;
    /** 0–1. */
    public final double confidence;
    /**
     * The detector's box, as a percentage of the image (0–100); 0 if unknown. Touching balls often
     * come as one box, so a box much bigger than one ball's is several balls.
     */
    public final double areaPct;

    public Detection(BallKind kind, double txDeg, double tyDeg, double confidence) {
        this(kind, txDeg, tyDeg, confidence, 0);
    }

    public Detection(BallKind kind, double txDeg, double tyDeg, double confidence, double areaPct) {
        this.kind = kind;
        this.txDeg = txDeg;
        this.tyDeg = tyDeg;
        this.confidence = confidence;
        this.areaPct = areaPct;
    }
}
