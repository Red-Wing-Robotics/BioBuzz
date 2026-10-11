package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * How a driving assist gets from where the robot is to a target pose: the quickest way that keeps
 * every part of the robot at least a margin from the obstacles ({@link Field#HIVE_FLOOR}), round
 * them or through the lane between them. {@link Steps#driveTo} plans one each time it starts.
 * Pure Java with Pedro core: unit tested, and tools/routeview compiles it.
 *
 * <ul>
 * <li><b>Square near obstacles.</b> Within its reach of one ({@link RobotSize#REACH} plus the
 *     margin), the robot holds a heading square to the field (0°, 90°, 180° or 270°). Its
 *     footprint then lines up with the obstacles, so it passes them with half its width or
 *     length plus the margin: that is what makes the lane under the HIVE wide enough.</li>
 * <li><b>Turns where it can turn any way.</b> On the way before it gets near, if there's room;
 *     otherwise on the spot where it starts (after moving straight out, if it starts closer than
 *     the margin). Turning on the spot near an obstacle keeps a gap of half its width or length
 *     plus the margin, less its reach: 4.6 in for 18 × 18 with an 8.3 in margin.</li>
 * <li><b>Quickest</b> by {@link #SPEED}, {@link #ACCEL} and {@link #TURN_RATE}, with each corner
 *     costing the time to slow for it. Rough by design: calibrate by timing real runs.</li>
 * </ul>
 */
public final class DrivePlan {

    /** Cruising speed, in/s (the tuned maximum forward velocity is 69.6). The auto maps use it too. */
    public static final double SPEED = 50;
    /** Speeding up and braking, in/s² (the tuned natural forward deceleration). The auto maps use it too. */
    public static final double ACCEL = 37;
    /** Turning on the spot, rad/s. PLACEHOLDER: time a real turn. */
    public static final double TURN_RATE = Math.PI;

    /** A turn on the spot is done within this, radians. */
    static final double TURNED = Math.toRadians(3);
    /** Closer than this, inches, the robot is already there. */
    static final double ARRIVED = 1.0;

    /** Driving time: each leg at {@link #SPEED}, and each corner what it costs to slow for. */
    static final KeepOut.Cost TIME = new KeepOut.Cost() {
        @Override public double leg(double inches) { return inches / SPEED; }
        @Override public double corner(double radians) { return cornerSeconds(radians); }
    };

    /** One part of a plan: a path to follow, or a turn on the spot. */
    public static final class Part {
        /** The path to follow; null for a turn. */
        public final Path path;
        /** For a turn: the heading to turn to, radians, and where the plan expects the robot to be. */
        public final double heading;
        public final double x;
        public final double y;

        private Part(Path path, double heading, double x, double y) {
            this.path = path;
            this.heading = heading;
            this.x = x;
            this.y = y;
        }

        public boolean isTurn() {
            return path == null;
        }
    }

    /** What to do, in order. Empty if the robot is already there. */
    public final List<Part> parts;
    /** The way's points, start to target. */
    public final List<double[]> points;
    /** Estimated time, seconds. */
    public final double seconds;

    private DrivePlan(List<Part> parts, List<double[]> points, double seconds) {
        this.parts = Collections.unmodifiableList(parts);
        this.points = Collections.unmodifiableList(points);
        this.seconds = seconds;
    }

    /** The quickest way from {@code from} to {@code target} keeping every part of the robot {@code margin} from each obstacle. */
    public static DrivePlan plan(Pose from, Pose target, List<KeepOut> obstacles, double margin) {
        if (Math.hypot(target.x() - from.x(), target.y() - from.y()) < ARRIVED) {
            double turn = wrap(target.heading() - from.heading());
            List<Part> parts = new ArrayList<>();
            if (Math.abs(turn) >= TURNED) {
                parts.add(new Part(null, target.heading(), from.x(), from.y()));
            }
            return new DrivePlan(parts, line(from, target), Math.abs(turn) / TURN_RATE);
        }
        // Hold the square heading nearest where it ends up, or nearest where it starts, whichever is quicker.
        double toward = square(target.heading());
        DrivePlan best = holding(toward, from, target, obstacles, margin);
        double now = square(from.heading());
        if (Math.abs(wrap(now - toward)) > 1e-9) {
            DrivePlan other = holding(now, from, target, obstacles, margin);
            if (other.seconds < best.seconds - 1e-9) {
                best = other;
            }
        }
        return best;
    }

    /** The plan that holds {@code square} near the obstacles. */
    private static DrivePlan holding(double square, Pose from, Pose target, List<KeepOut> obstacles, double margin) {
        boolean frontAlongY = Math.abs(Math.sin(square)) > 0.5;
        double halfX = (frontAlongY ? RobotSize.WIDTH : RobotSize.LENGTH) / 2 + margin;
        double halfY = (frontAlongY ? RobotSize.LENGTH : RobotSize.WIDTH) / 2 + margin;
        List<KeepOut> pass = new ArrayList<>(); // where the centre can't go, holding `square`
        List<KeepOut> near = new ArrayList<>(); // where it can't turn
        for (KeepOut o : obstacles) {
            pass.add(o.grow(halfX, halfY));
            near.add(o.grow(RobotSize.REACH + margin));
        }
        List<double[]> points = KeepOut.route(from.x(), from.y(), target.x(), target.y(), pass, TIME);
        List<Piece> pieces = pieces(points, near);
        int first = -1;
        int last = -1;
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).near) {
                first = first < 0 ? i : first;
                last = i;
            }
        }
        double h0 = from.heading();
        double h1 = target.heading();
        double[] start = new double[pieces.size()];
        double[] change = new double[pieces.size()];
        int turnFirstAt = -1; // a turn on the spot before this piece
        boolean turnLast = false; // a turn on the spot at the end

        if (first < 0) {
            // Never near: turn evenly over the whole way, as it drives.
            spread(pieces, 0, pieces.size(), h0, wrap(h1 - h0), start, change);
        } else {
            double turnIn = wrap(square - h0);
            double turnOut = wrap(h1 - square);
            double lead = length(pieces, 0, first);
            double tail = length(pieces, last + 1, pieces.size());
            if (lead > 0 && lead >= SPEED * Math.abs(turnIn) / TURN_RATE) {
                spread(pieces, 0, first, h0, turnIn, start, change);
            } else if (Math.abs(turnIn) >= TURNED) {
                // Starting closer than the margin, the first piece is the straight move out: turn after it.
                turnFirstAt = inside(new double[] {from.x(), from.y()}, pass) ? 1 : 0;
                spread(pieces, 0, turnFirstAt, h0, 0, start, change);
                spread(pieces, turnFirstAt, first, square, 0, start, change);
            } else {
                spread(pieces, 0, first, square, 0, start, change);
            }
            spread(pieces, Math.max(first, turnFirstAt), last + 1, square, 0, start, change);
            if (tail > 0 && tail >= SPEED * Math.abs(turnOut) / TURN_RATE) {
                spread(pieces, last + 1, pieces.size(), square, turnOut, start, change);
            } else {
                spread(pieces, last + 1, pieces.size(), square, 0, start, change);
                turnLast = Math.abs(turnOut) >= TURNED;
            }
        }

        List<Part> parts = new ArrayList<>();
        List<Path> legs = new ArrayList<>();
        List<double[]> drive = new ArrayList<>();
        double seconds = 0;
        for (int i = 0; i <= pieces.size(); i++) {
            if (i == turnFirstAt) {
                seconds += follow(legs, drive, parts);
                double[] here = i < pieces.size() ? pieces.get(i).a : new double[] {target.x(), target.y()};
                parts.add(new Part(null, square, here[0], here[1]));
                seconds += Math.abs(wrap(square - h0)) / TURN_RATE;
            }
            if (i == pieces.size()) {
                break;
            }
            Piece p = pieces.get(i);
            final double h = start[i];
            final double dh = change[i];
            legs.add(Paths.line(Vector2D.cartesian(p.a[0], p.a[1]), Vector2D.cartesian(p.b[0], p.b[1]))
                    .heading((curve, t) -> h + t * dh));
            if (drive.isEmpty()) {
                drive.add(p.a);
            }
            drive.add(p.b);
        }
        seconds += follow(legs, drive, parts);
        if (turnLast) {
            parts.add(new Part(null, h1, target.x(), target.y()));
            seconds += Math.abs(wrap(h1 - square)) / TURN_RATE;
        }
        if (first < 0) {
            seconds = Math.max(seconds, Math.abs(wrap(h1 - h0)) / TURN_RATE); // it turns as it drives
        }
        return new DrivePlan(parts, points, seconds);
    }

    /** Moves the legs so far into one part to follow; returns its estimated time. */
    private static double follow(List<Path> legs, List<double[]> drive, List<Part> parts) {
        if (legs.isEmpty()) {
            return 0;
        }
        Path path = legs.size() == 1 ? legs.get(0) : Paths.path(legs.toArray(new Path[0]));
        parts.add(new Part(path, Double.NaN, Double.NaN, Double.NaN));
        double seconds = driveSeconds(drive);
        legs.clear();
        drive.clear();
        return seconds;
    }

    // ---- the way, in pieces near an obstacle or clear of all of them -----------------------------

    private static final class Piece {
        final double[] a;
        final double[] b;
        final boolean near;
        final double length;

        Piece(double[] a, double[] b, boolean near) {
            this.a = a;
            this.b = b;
            this.near = near;
            this.length = Math.hypot(b[0] - a[0], b[1] - a[1]);
        }
    }

    /** Each leg of the way, cut where it goes in or out of a {@code near} zone. */
    private static List<Piece> pieces(List<double[]> points, List<KeepOut> near) {
        List<Piece> out = new ArrayList<>();
        for (int i = 0; i + 1 < points.size(); i++) {
            double[] a = points.get(i);
            double[] b = points.get(i + 1);
            if (Math.hypot(b[0] - a[0], b[1] - a[1]) < 1e-6) {
                continue;
            }
            List<Double> cuts = new ArrayList<>(Arrays.asList(0.0, 1.0));
            for (KeepOut zone : near) {
                double[] in = zone.crossing(a[0], a[1], b[0], b[1]);
                if (in != null) {
                    cuts.add(in[0]);
                    cuts.add(in[1]);
                }
            }
            Collections.sort(cuts);
            for (int c = 0; c + 1 < cuts.size(); c++) {
                double t0 = cuts.get(c);
                double t1 = cuts.get(c + 1);
                if (t1 - t0 > 1e-9) {
                    out.add(new Piece(at(a, b, t0), at(a, b, t1), inside(at(a, b, (t0 + t1) / 2), near)));
                }
            }
        }
        return out;
    }

    /** Headings for pieces [from, to): starting at {@code heading}, changing by {@code turn} evenly along them. */
    private static void spread(List<Piece> pieces, int from, int to, double heading, double turn, double[] start, double[] change) {
        double total = length(pieces, from, to);
        double done = 0;
        for (int i = from; i < to; i++) {
            double share = total > 0 ? pieces.get(i).length / total : 0;
            start[i] = heading + turn * (total > 0 ? done / total : 0);
            change[i] = turn * share;
            done += pieces.get(i).length;
        }
    }

    private static double length(List<Piece> pieces, int from, int to) {
        double sum = 0;
        for (int i = from; i < to; i++) {
            sum += pieces.get(i).length;
        }
        return sum;
    }

    // ---- time --------------------------------------------------------------------------------

    /** Slowing from {@link #SPEED} to what it keeps round a corner (its speed along the new direction), and back. */
    static double cornerSeconds(double radians) {
        double kept = SPEED * Math.max(0, Math.cos(radians));
        return (SPEED - kept) * (SPEED - kept) / (ACCEL * SPEED);
    }

    /** Driving through these points, from a stop to a stop. */
    static double driveSeconds(List<double[]> points) {
        double length = 0;
        double corners = 0;
        for (int i = 0; i + 1 < points.size(); i++) {
            length += Math.hypot(points.get(i + 1)[0] - points.get(i)[0], points.get(i + 1)[1] - points.get(i)[1]);
            if (i > 0) {
                corners += cornerSeconds(KeepOut.turn(points.get(i - 1), points.get(i), points.get(i + 1)));
            }
        }
        double upAndDown = SPEED * SPEED / ACCEL; // inches to reach SPEED and stop again
        double straight = length >= upAndDown ? length / SPEED + SPEED / ACCEL : 2 * Math.sqrt(length / ACCEL);
        return straight + corners;
    }

    // ---- small helpers --------------------------------------------------------------------------

    /** The heading square to the field (a multiple of 90°) nearest {@code heading}. */
    static double square(double heading) {
        return Math.round(heading / (Math.PI / 2)) * (Math.PI / 2);
    }

    /** {@code angle} in (−π, π]. */
    static double wrap(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    private static boolean inside(double[] p, List<KeepOut> zones) {
        for (KeepOut z : zones) {
            if (z.contains(p[0], p[1])) {
                return true;
            }
        }
        return false;
    }

    private static double[] at(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
    }

    private static List<double[]> line(Pose from, Pose target) {
        return Arrays.asList(new double[] {from.x(), from.y()}, new double[] {target.x(), target.y()});
    }
}
