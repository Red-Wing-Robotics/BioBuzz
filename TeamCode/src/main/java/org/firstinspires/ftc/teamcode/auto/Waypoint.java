package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.math.Pose;

/**
 * A field position for autonomous paths: inches and degrees, always written for the RED
 * alliance. Fields are public and non-final so Panels can edit them live when the holding class
 * is {@code @Configurable}. Convert to a Pedro {@link Pose} for an alliance with
 * {@link Alliance#pose(Waypoint)}.
 */
public class Waypoint {
    public double x;
    public double y;
    public double headingDeg;

    public Waypoint() {} // required by Panels for editing

    public Waypoint(double x, double y, double headingDeg) {
        this.x = x;
        this.y = y;
        this.headingDeg = headingDeg;
    }

    public Pose pose(PoseFactory poses) {
        return poses.of(x, y, headingDeg);
    }

    @Override
    public String toString() {
        return String.format("(%.1f, %.1f, %.0f°)", x, y, headingDeg);
    }
}
