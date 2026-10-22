package org.firstinspires.ftc.teamcode.vision;

import com.pedropathing.math.Pose;

/**
 * The robot's recent poses, so a camera frame can be placed on the field from where the robot
 * was when the image was taken, not where it is by the time the result arrives. That matters:
 * 60 ms late while turning at 180°/s puts a ball 30 in away about 6 in to the side.
 *
 * A fixed ring buffer: adding a pose allocates nothing.
 */
public final class PoseHistory {

    private final double spanMs;
    private final double[] t;
    private final double[] x;
    private final double[] y;
    private final double[] h;
    private int first;
    private int size;

    /** Keeps poses for {@code spanMs}, and at most {@code capacity} of them. */
    public PoseHistory(double spanMs, int capacity) {
        this.spanMs = spanMs;
        this.t = new double[capacity];
        this.x = new double[capacity];
        this.y = new double[capacity];
        this.h = new double[capacity];
    }

    /** Record where the robot is at {@code timeMs}. Times must not go backwards; older ones are ignored. */
    public void add(double timeMs, Pose pose) {
        if (size > 0 && timeMs < t[index(size - 1)]) {
            return;
        }
        if (size == t.length) {
            first = (first + 1) % t.length;
            size--;
        }
        int i = index(size);
        t[i] = timeMs;
        x[i] = pose.x();
        y[i] = pose.y();
        h[i] = pose.heading();
        size++;
        while (size > 1 && t[index(1)] < timeMs - spanMs) {
            first = (first + 1) % t.length;
            size--;
        }
    }

    /** Where the robot was at {@code timeMs}, interpolated; the oldest or newest pose outside the span. Null if empty. */
    public Pose at(double timeMs) {
        if (size == 0) {
            return null;
        }
        int after = after(timeMs);
        if (after == 0) {
            return pose(0);
        }
        if (after == size) {
            return pose(size - 1);
        }
        int a = index(after - 1);
        int b = index(after);
        double f = (timeMs - t[a]) / (t[b] - t[a]);
        return new Pose(x[a] + (x[b] - x[a]) * f, y[a] + (y[b] - y[a]) * f, h[a] + wrap(h[b] - h[a]) * f);
    }

    /** How fast the robot was turning at {@code timeMs}, rad/s, from the poses either side. 0 with fewer than two. */
    public double turnRate(double timeMs) {
        if (size < 2) {
            return 0;
        }
        int after = Math.max(1, Math.min(size - 1, after(timeMs)));
        int a = index(after - 1);
        int b = index(after);
        double dt = t[b] - t[a];
        return dt <= 0 ? 0 : wrap(h[b] - h[a]) / (dt / 1000);
    }

    public void clear() {
        size = 0;
        first = 0;
    }

    /** The first stored position whose time is after {@code timeMs}: 0 … size. */
    private int after(double timeMs) {
        int n = size;
        while (n > 0 && t[index(n - 1)] > timeMs) {
            n--;
        }
        return n;
    }

    private Pose pose(int n) {
        int i = index(n);
        return new Pose(x[i], y[i], h[i]);
    }

    private int index(int n) {
        return (first + n) % t.length;
    }

    private static double wrap(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }
}
