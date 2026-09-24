package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.math.Pose;

/**
 * Which alliance an auto runs for. Waypoints are authored once in the RED frame; BLUE poses are
 * produced by rotating them 180° about the field centre (72, 72), which also flips the heading.
 *
 * TODO(field): confirm BIOBUZZ is rotationally symmetric (GARDENs are in opposite corners, which
 * suggests so). If the field is mirrored left/right instead, use
 * {@code PoseFactory.degrees().mirrorX(72)} for BLUE.
 */
public enum Alliance {
    RED(PoseFactory.degrees()),
    BLUE(PoseFactory.degrees().mirrorAroundPoint(72, 72));

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
