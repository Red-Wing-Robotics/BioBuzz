package org.firstinspires.ftc.teamcode.auto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A rectangle of the field the robot's centre must stay out of, in the alliance frame
 * (docs/coordinates.md): field-fixed, never mirrored. {@link Steps#driveTo} drives around these.
 * {@link Field#HIVE} is one; grow it by the robot's reach first. Pure Java: tools/routeview
 * compiles it too.
 */
public final class KeepOut {

    /** How far route points sit outside a zone's edge, so a leg along that edge doesn't count as crossing it. */
    private static final double EDGE_GAP = 0.01;

    public final double minX;
    public final double minY;
    public final double maxX;
    public final double maxY;

    public KeepOut(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    /** This rectangle grown by {@code inches} on every side. */
    public KeepOut grow(double inches) {
        return grow(inches, inches);
    }

    /** This rectangle grown by {@code x} on its left and right and by {@code y} on its bottom and top. */
    public KeepOut grow(double x, double y) {
        return new KeepOut(minX - x, minY - y, maxX + x, maxY + y);
    }

    /** True if (x, y) is strictly inside; the edge is outside. */
    public boolean contains(double x, double y) {
        return x > minX && x < maxX && y > minY && y < maxY;
    }

    /** True if the segment a → b passes through the inside. Running along an edge or touching a corner doesn't. */
    public boolean blocks(double ax, double ay, double bx, double by) {
        double[] in = crossing(ax, ay, bx, by);
        return in != null && in[1] - in[0] > 1e-9;
    }

    /**
     * Where the segment a → b is inside: {enter, leave} as fractions of the way from a (0) to b
     * (1), or null if it never is. A segment along an edge isn't.
     */
    public double[] crossing(double ax, double ay, double bx, double by) {
        // Liang–Barsky: clip the segment to the rectangle.
        double dx = bx - ax;
        double dy = by - ay;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {ax - minX, maxX - ax, ay - minY, maxY - ay};
        double enter = 0;
        double leave = 1;
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] <= 0) {
                    return null; // parallel to this edge, on it or outside
                }
            } else {
                double t = q[i] / p[i];
                if (p[i] < 0) {
                    enter = Math.max(enter, t);
                } else {
                    leave = Math.min(leave, t);
                }
            }
        }
        return leave > enter ? new double[] {enter, leave} : null;
    }

    /** What {@link #route} keeps lowest: a cost for each straight leg and for each change of direction. */
    public interface Cost {
        /** A straight leg this many inches long. */
        double leg(double inches);

        /** Changing direction by this many radians (0 to π) where two legs meet. */
        double corner(double radians);
    }

    /** Distance alone: the shortest way. */
    public static final Cost SHORTEST = new Cost() {
        @Override public double leg(double inches) { return inches; }
        @Override public double corner(double radians) { return 0; }
    };

    /** The shortest way from a to b that stays out of every zone: {@link #route(double, double, double, double, List, Cost)}. */
    public static List<double[]> route(double ax, double ay, double bx, double by, List<KeepOut> zones) {
        return route(ax, ay, bx, by, zones, SHORTEST);
    }

    /**
     * The way from a to b of least {@code cost} that stays out of every zone, as the points to
     * drive through: a, the zone corners to go round, b. Just [a, b] when the straight line is
     * clear. If a or b is inside a zone, the way starts with a straight move out to its nearest
     * edge, or ends with a straight move in from one.
     */
    public static List<double[]> route(double ax, double ay, double bx, double by, List<KeepOut> zones, Cost cost) {
        double[] a = {ax, ay};
        double[] b = {bx, by};
        if (!blocked(a, b, zones)) {
            return Arrays.asList(a, b);
        }
        double[] from = exit(a, zones);
        double[] to = exit(b, zones);

        // Least cost over the visibility graph: from, to, and every zone corner not inside another
        // zone. A corner's cost depends on the legs either side, so the search is over (node, the
        // node before it): state [u][p + 1], with p = -1 at the start.
        List<double[]> nodes = new ArrayList<>();
        nodes.add(from);
        nodes.add(to);
        for (KeepOut zone : zones) {
            for (double[] corner : zone.corners()) {
                if (!inside(corner, zones)) {
                    nodes.add(corner);
                }
            }
        }
        int n = nodes.size();
        double[][] best = new double[n][n + 1];
        int[][] back = new int[n][n + 1];
        boolean[][] done = new boolean[n][n + 1];
        for (double[] row : best) {
            Arrays.fill(row, Double.POSITIVE_INFINITY);
        }
        best[0][0] = 0;
        int end = -1; // the state [1][end] that reached b
        while (true) {
            int u = -1;
            int pu = -1;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j <= n; j++) {
                    if (!done[i][j] && best[i][j] < Double.POSITIVE_INFINITY && (u < 0 || best[i][j] < best[u][pu])) {
                        u = i;
                        pu = j;
                    }
                }
            }
            if (u < 0) {
                break;
            }
            if (u == 1) {
                end = pu;
                break;
            }
            done[u][pu] = true;
            double[] at = nodes.get(u);
            for (int v = 0; v < n; v++) {
                if (v == u || blocked(at, nodes.get(v), zones)) {
                    continue;
                }
                double[] next = nodes.get(v);
                double c = best[u][pu] + cost.leg(Math.hypot(next[0] - at[0], next[1] - at[1]))
                        + (pu > 0 ? cost.corner(turn(nodes.get(pu - 1), at, next)) : 0);
                if (c < best[v][u + 1]) {
                    best[v][u + 1] = c;
                    back[v][u + 1] = pu;
                }
            }
        }
        if (end < 0) {
            return Arrays.asList(a, b); // walled in: nothing better to offer
        }

        List<double[]> way = new ArrayList<>();
        for (int node = 1, p = end; node >= 0; ) {
            way.add(nodes.get(node));
            int before = back[node][p];
            node = p - 1;
            p = before;
        }
        Collections.reverse(way);
        if (from != a) {
            way.add(0, a);
        }
        if (to != b) {
            way.add(b);
        }
        return way;
    }

    /** How far the direction turns at b, going a → b → c: radians, 0 (straight on) to π (straight back). */
    static double turn(double[] a, double[] b, double[] c) {
        double ux = b[0] - a[0], uy = b[1] - a[1];
        double vx = c[0] - b[0], vy = c[1] - b[1];
        return Math.abs(Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy));
    }

    private double[][] corners() {
        return new double[][] {
                {minX - EDGE_GAP, minY - EDGE_GAP}, {maxX + EDGE_GAP, minY - EDGE_GAP},
                {maxX + EDGE_GAP, maxY + EDGE_GAP}, {minX - EDGE_GAP, maxY + EDGE_GAP}};
    }

    /** The nearest point just outside the zone holding {@code p}, or {@code p} itself if it's in none. */
    private static double[] exit(double[] p, List<KeepOut> zones) {
        for (KeepOut z : zones) {
            if (z.contains(p[0], p[1])) {
                double left = p[0] - z.minX;
                double right = z.maxX - p[0];
                double below = p[1] - z.minY;
                double above = z.maxY - p[1];
                double nearest = Math.min(Math.min(left, right), Math.min(below, above));
                if (nearest == left) {
                    return new double[] {z.minX - EDGE_GAP, p[1]};
                } else if (nearest == right) {
                    return new double[] {z.maxX + EDGE_GAP, p[1]};
                } else if (nearest == below) {
                    return new double[] {p[0], z.minY - EDGE_GAP};
                }
                return new double[] {p[0], z.maxY + EDGE_GAP};
            }
        }
        return p;
    }

    private static boolean inside(double[] p, List<KeepOut> zones) {
        for (KeepOut z : zones) {
            if (z.contains(p[0], p[1])) {
                return true;
            }
        }
        return false;
    }

    private static boolean blocked(double[] a, double[] b, List<KeepOut> zones) {
        for (KeepOut z : zones) {
            if (z.blocks(a[0], a[1], b[0], b[1])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("KeepOut(x %.1f–%.1f, y %.1f–%.1f)", minX, maxX, minY, maxY);
    }
}
