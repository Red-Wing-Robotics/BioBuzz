package org.firstinspires.ftc.teamcode.automaps;

import com.pedropathing.math.Pose;

/** Convex polygons in field inches: the robot's footprint, field zones, and overlap tests. */
final class Geometry {

    private Geometry() {}

    /** A convex polygon. */
    static final class Poly {
        final double[] x;
        final double[] y;

        Poly(double[] x, double[] y) {
            this.x = x;
            this.y = y;
        }

        static Poly rect(double x0, double y0, double x1, double y1) {
            return new Poly(new double[] {x0, x1, x1, x0}, new double[] {y0, y0, y1, y1});
        }

        /** A regular polygon around (cx, cy). */
        static Poly regular(double cx, double cy, double radius, int sides, double startDeg) {
            double[] xs = new double[sides];
            double[] ys = new double[sides];
            for (int i = 0; i < sides; i++) {
                double a = Math.toRadians(startDeg + 360.0 * i / sides);
                xs[i] = cx + radius * Math.cos(a);
                ys[i] = cy + radius * Math.sin(a);
            }
            return new Poly(xs, ys);
        }

        /** This polygon rotated 180° about the field centre: the other alliance's copy. */
        Poly rotated(double fieldSize) {
            double[] xs = new double[x.length];
            double[] ys = new double[y.length];
            for (int i = 0; i < x.length; i++) {
                xs[i] = fieldSize - x[i];
                ys[i] = fieldSize - y[i];
            }
            return new Poly(xs, ys);
        }

        double minX() { return min(x); }
        double maxX() { return max(x); }
        double minY() { return min(y); }
        double maxY() { return max(y); }
    }

    /** The robot's footprint at {@code pose}: length along the heading, width across it. */
    static Poly footprint(Pose pose, double length, double width) {
        double c = Math.cos(pose.heading());
        double s = Math.sin(pose.heading());
        double hl = length / 2;
        double hw = width / 2;
        double[][] local = {{hl, hw}, {-hl, hw}, {-hl, -hw}, {hl, -hw}};
        double[] xs = new double[4];
        double[] ys = new double[4];
        for (int i = 0; i < 4; i++) {
            xs[i] = pose.x() + local[i][0] * c - local[i][1] * s;
            ys[i] = pose.y() + local[i][0] * s + local[i][1] * c;
        }
        return new Poly(xs, ys);
    }

    /**
     * True if the polygons overlap or come within {@code margin} inches of each other
     * (separating-axis test; both must be convex).
     */
    static boolean overlaps(Poly a, Poly b, double margin) {
        return !separated(a, b, margin) && !separated(b, a, margin);
    }

    /** Smallest distance from any corner to the nearest wall; negative means past the wall. */
    static double wallGap(Poly p, double fieldSize) {
        double gap = Double.MAX_VALUE;
        for (int i = 0; i < p.x.length; i++) {
            gap = Math.min(gap, Math.min(Math.min(p.x[i], fieldSize - p.x[i]), Math.min(p.y[i], fieldSize - p.y[i])));
        }
        return gap;
    }

    private static boolean separated(Poly a, Poly b, double margin) {
        int n = a.x.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            double nx = -(a.y[j] - a.y[i]);
            double ny = a.x[j] - a.x[i];
            double len = Math.hypot(nx, ny);
            if (len == 0) {
                continue;
            }
            nx /= len;
            ny /= len;
            double aMin = Double.MAX_VALUE, aMax = -Double.MAX_VALUE, bMin = Double.MAX_VALUE, bMax = -Double.MAX_VALUE;
            for (int k = 0; k < a.x.length; k++) {
                double d = a.x[k] * nx + a.y[k] * ny;
                aMin = Math.min(aMin, d);
                aMax = Math.max(aMax, d);
            }
            for (int k = 0; k < b.x.length; k++) {
                double d = b.x[k] * nx + b.y[k] * ny;
                bMin = Math.min(bMin, d);
                bMax = Math.max(bMax, d);
            }
            if (aMax + margin < bMin || bMax + margin < aMin) {
                return true;
            }
        }
        return false;
    }

    private static double min(double[] v) {
        double m = v[0];
        for (double d : v) m = Math.min(m, d);
        return m;
    }

    private static double max(double[] v) {
        double m = v[0];
        for (double d : v) m = Math.max(m, d);
        return m;
    }
}
