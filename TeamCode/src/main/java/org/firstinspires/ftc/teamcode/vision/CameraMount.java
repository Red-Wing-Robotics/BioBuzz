/*
 * The ground-range geometry is adapted from Hive-Vision's BallTracker.groundRange and
 * BallChaseFollower's field projection (https://github.com/hivevisionftc/Hive-Vision),
 * Copyright (c) 2026 Harjas Sidhu, MIT License: see THIRD_PARTY_NOTICES.md at the repo root.
 * Changed: the full camera ray (tx as well as ty), yaw, and the ball-centre height per ball.
 */
package org.firstinspires.ftc.teamcode.vision;

import com.pedropathing.math.Pose;

/**
 * Where the camera sits on the robot and how it is aimed: turns a detection's image angles into a
 * point on the field, and back. Robot frame: inches from the tracking centre (the point the
 * Pinpoint pod offsets are measured from), + forward toward the launcher, + left. The numbers come
 * from docs/hardware.md ("Limelight 3A mount").
 *
 * A pinhole camera: a detection at (tx, ty) lies on the ray through the lens whose direction, per
 * unit along the lens axis, is tan(tx) to the right and tan(ty) up. The ball's centre is where
 * that ray meets the plane at the ball's centre height. Pitched down and with tx = 0 this is
 * Hive-Vision's range, (height − ball centre) / tan(pitch − ty).
 */
public final class CameraMount {

    public final double forward;
    public final double left;
    /** Lens centre above the tiles, inches. */
    public final double height;
    /** Lens axis below horizontal, degrees. */
    public final double pitchDownDeg;
    /** Lens axis turned left of straight ahead, degrees. */
    public final double yawDeg;
    public final double hfovDeg;
    public final double vfovDeg;
    /** Detections this close to an image edge, degrees, may be cut off and are not used. */
    public final double edgeMarginDeg;

    private final double cosPitch;
    private final double sinPitch;
    private final double cosYaw;
    private final double sinYaw;

    public CameraMount(double forward, double left, double height, double pitchDownDeg, double yawDeg,
                       double hfovDeg, double vfovDeg, double edgeMarginDeg) {
        this.forward = forward;
        this.left = left;
        this.height = height;
        this.pitchDownDeg = pitchDownDeg;
        this.yawDeg = yawDeg;
        this.hfovDeg = hfovDeg;
        this.vfovDeg = vfovDeg;
        this.edgeMarginDeg = edgeMarginDeg;
        this.cosPitch = Math.cos(Math.toRadians(pitchDownDeg));
        this.sinPitch = Math.sin(Math.toRadians(pitchDownDeg));
        this.cosYaw = Math.cos(Math.toRadians(yawDeg));
        this.sinYaw = Math.sin(Math.toRadians(yawDeg));
    }

    /**
     * Where the ray through (tx, ty) meets the plane {@code planeHeight} above the tiles, as
     * {forward, left} in the robot frame. Null if it never does: the ray is level or rising, or the
     * plane is not below the lens.
     */
    public double[] toRobot(double txDeg, double tyDeg, double planeHeight) {
        double drop = height - planeHeight;
        if (drop <= 0) {
            return null;
        }
        double up = Math.tan(Math.toRadians(tyDeg));
        double leftward = -Math.tan(Math.toRadians(txDeg));
        // The lens axis is (cos p, 0, −sin p) and the image's up is (sin p, 0, cos p).
        double dx = cosPitch + up * sinPitch;
        double dz = -sinPitch + up * cosPitch;
        if (dz >= -1e-9) {
            return null;
        }
        double t = drop / -dz;
        double x = t * dx;
        double y = t * leftward;
        return new double[] {forward + x * cosYaw - y * sinYaw, left + x * sinYaw + y * cosYaw};
    }

    /**
     * The image angles {tx, ty}, degrees, at which a point in the robot frame ({@code up} inches
     * above the tiles) appears. Null if it is behind the camera.
     */
    public double[] toImage(double fwd, double lft, double up) {
        double vx = fwd - forward;
        double vy = lft - left;
        double vz = up - height;
        double x = vx * cosYaw + vy * sinYaw;
        double y = -vx * sinYaw + vy * cosYaw;
        double along = x * cosPitch - vz * sinPitch;
        double imageUp = x * sinPitch + vz * cosPitch;
        if (along <= 1e-9) {
            return null;
        }
        return new double[] {Math.toDegrees(Math.atan(-y / along)), Math.toDegrees(Math.atan(imageUp / along))};
    }

    /**
     * How big one ball's box should be in the image, as a percentage of it, for a ball centred at
     * a point in the robot frame: its width and height in the image are its diameter over its
     * depth along the lens axis, against the image's width and height at unit depth. Infinite if
     * the point is behind the camera.
     */
    public double ballAreaPct(double fwd, double lft, BallKind kind) {
        double vx = fwd - forward;
        double vy = lft - left;
        double vz = kind.centreHeight() - height;
        double x = vx * cosYaw + vy * sinYaw;
        double depth = x * cosPitch - vz * sinPitch;
        if (depth <= 1e-9) {
            return Double.POSITIVE_INFINITY;
        }
        double r = kind.diameter / 2 / depth;
        return 100 * r * r / (Math.tan(Math.toRadians(hfovDeg / 2)) * Math.tan(Math.toRadians(vfovDeg / 2)));
    }

    /** Within {@link #edgeMarginDeg} of an image edge, or outside the image. */
    public boolean nearEdge(double txDeg, double tyDeg) {
        return Math.abs(txDeg) > hfovDeg / 2 - edgeMarginDeg || Math.abs(tyDeg) > vfovDeg / 2 - edgeMarginDeg;
    }

    /** True if a point in the robot frame would appear in the usable part of the image. */
    public boolean sees(double fwd, double lft, double up) {
        double[] a = toImage(fwd, lft, up);
        return a != null && !nearEdge(a[0], a[1]);
    }

    /** A robot-frame point {forward, left} on the field, with the robot at {@code robot}. */
    public static double[] toField(Pose robot, double[] p) {
        double c = Math.cos(robot.heading());
        double s = Math.sin(robot.heading());
        return new double[] {robot.x() + p[0] * c - p[1] * s, robot.y() + p[0] * s + p[1] * c};
    }

    /** A field point in the robot frame {forward, left}, with the robot at {@code robot}. */
    public static double[] toRobotFrame(Pose robot, double x, double y) {
        double c = Math.cos(robot.heading());
        double s = Math.sin(robot.heading());
        double dx = x - robot.x();
        double dy = y - robot.y();
        return new double[] {dx * c + dy * s, -dx * s + dy * c};
    }
}
