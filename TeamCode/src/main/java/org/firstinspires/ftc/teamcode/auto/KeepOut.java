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
        return new KeepOut(minX - inches, minY - inches, maxX + inches, maxY + inches);
    }

    /** True if (x, y) is strictly inside; the edge is outside. */
    public boolean contains(double x, double y) {
        return x > minX && x < maxX && y > minY && y < maxY;
    }

    /** True if the segment a → b passes through the inside. Running along an edge or touching a corner doesn't. */
    public boolean blocks(double ax, double ay, double bx, double by) {
        // Liang–Barsky: clip the segment to the rectangle; blocked if any length is left.
        double dx = bx - ax;
        double dy = by - ay;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {ax - minX, maxX - ax, ay - minY, maxY - ay};
        double enter = 0;
        double leave = 1;
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] <= 0) {
                    return false; // parallel to this edge, on it or outside
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
        return leave - enter > 1e-9;
    }

    /**
     * The shortest way from a to b that stays out of every zone, as the points to drive through:
     * a, the zone corners to go round, b. Just [a, b] when the straight line is clear. If a or b
     * is inside a zone, the way starts with a straight move out to its nearest edge, or ends with
     * a straight move in from one.
     */
    public static List<double[]> route(double ax, double ay, double bx, double by, List<KeepOut> zones) {
        double[] a = {ax, ay};
        double[] b = {bx, by};
        if (!blocked(a, b, zones)) {
            return Arrays.asList(a, b);
        }
        double[] from = exit(a, zones);
        double[] to = exit(b, zones);

        // Shortest path over the visibility graph: from, to, and every zone corner not inside another zone.
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
        double[] dist = new double[n];
        int[] prev = new int[n];
        boolean[] done = new boolean[n];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        Arrays.fill(prev, -1);
        dist[0] = 0;
        for (int round = 0; round < n; round++) {
            int u = -1;
            for (int i = 0; i < n; i++) {
                if (!done[i] && (u < 0 || dist[i] < dist[u])) {
                    u = i;
                }
            }
            if (dist[u] == Double.POSITIVE_INFINITY || u == 1) {
                break;
            }
            done[u] = true;
            for (int v = 0; v < n; v++) {
                if (done[v] || blocked(nodes.get(u), nodes.get(v), zones)) {
                    continue;
                }
                double d = dist[u] + Math.hypot(nodes.get(v)[0] - nodes.get(u)[0], nodes.get(v)[1] - nodes.get(u)[1]);
                if (d < dist[v]) {
                    dist[v] = d;
                    prev[v] = u;
                }
            }
        }
        if (dist[1] == Double.POSITIVE_INFINITY) {
            return Arrays.asList(a, b); // walled in: nothing better to offer
        }

        List<double[]> way = new ArrayList<>();
        for (int i = 1; i >= 0; i = prev[i]) {
            way.add(nodes.get(i));
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
