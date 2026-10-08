package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.math.Pose;

/**
 * Which alliance an OpMode plays. Poses are written once in the RED frame; BLUE ones are rotated
 * 180° about the field centre ({@link Field#CENTER}), which also turns the heading by 180°.
 * BIOBUZZ is rotationally symmetric, so this, not a left/right mirror, is right. Both are defined
 * in docs/coordinates.md.
 */
public enum Alliance {
    RED(PoseFactory.degrees()),
    BLUE(PoseFactory.degrees().mirrorAroundPoint(Field.CENTER, Field.CENTER));

    private final PoseFactory poses;

    Alliance(PoseFactory poses) {
        this.poses = poses;
    }

    /** Pose factory that maps RED-frame (x, y, heading°) into this alliance's frame. */
    public PoseFactory poses() {
        return poses;
    }

    public Pose pose(Waypoint waypoint) {
        return waypoint.pose(poses);
    }
}
