package org.firstinspires.ftc.teamcode.vision;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.DrivePlan;
import org.firstinspires.ftc.teamcode.auto.Field;
import org.firstinspires.ftc.teamcode.auto.KeepOut;
import org.firstinspires.ftc.teamcode.auto.RobotSize;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Where to stop for a ball, which ball to go for, and when to re-plan: the decisions in {@link DriveToBall}. */
public final class BallApproach {

    /** Which end of the robot faces the ball at the stop: where the intake is. */
    public enum Side { FRONT, BACK }

    /** How far round the ball, either way, {@link #stopFor} may swing the approach from straight on. */
    static final double MAX_SWING = Math.toRadians(90);
    static final double SWING_STEP = Math.toRadians(15);

    /** The ball to go for, the group it's in, where to stop for it, and the planned time to get there. */
    public static final class Choice {
        public final BallTracker.Ball ball;
        public final Group group;
        public final Pose stop;
        public final double seconds;
        /** Why this one, for the Driver Station, e.g. "3 together, 0.2 s farther than the nearest". Empty for a plain nearest pick. */
        public final String why;
        /**
         * Every ball it weighed, quickest first, the chosen one marked "→", e.g.
         * "→ POLLEN×2 (14, 3) group 3 1.1 s | POLLEN (21, 4) group 1 0.9 s". For the DS and the log.
         */
        public final String candidates;

        Choice(BallTracker.Ball ball, Group group, Pose stop, double seconds, String why, String candidates) {
            this.ball = ball;
            this.group = group;
            this.stop = stop;
            this.seconds = seconds;
            this.why = why;
            this.candidates = candidates;
        }

        private String describe() {
            return String.format(Locale.US, "%s%s (%.0f, %.0f) group %d %.1f s", ball.kind,
                    ball.blob() > 1 ? "×" + ball.blob() : "", ball.x(), ball.y(), group.count, seconds);
        }
    }

    /**
     * A ball and the wanted, legal balls within {@code clusterRadius} of it: how many balls in all
     * (each tracked ball counts as its {@link BallTracker.Ball#blob}, so one box holding three
     * touching balls counts three), and their middle, weighted the same way.
     */
    public static final class Group {
        public final int count;
        public final double x;
        public final double y;

        Group(int count, double x, double y) {
            this.count = count;
            this.x = x;
            this.y = y;
        }
    }

    private BallApproach() {}

    /**
     * Where to stop for a ball at (bx, by): {@code standoff} inches from it on the line back toward
     * the robot, with {@code side} facing it. A robot already on top of the ball keeps its heading.
     */
    public static Pose standoffPose(Pose robot, double bx, double by, double standoff, Side side) {
        double dx = robot.x() - bx;
        double dy = robot.y() - by;
        double d = Math.hypot(dx, dy);
        double ux;
        double uy;
        if (d < 1e-6) {
            double facing = robot.heading() + (side == Side.BACK ? Math.PI : 0);
            ux = -Math.cos(facing);
            uy = -Math.sin(facing);
        } else {
            ux = dx / d;
            uy = dy / d;
        }
        return standoffPose(Math.atan2(uy, ux), bx, by, standoff, side);
    }

    /** The stop {@code standoff} from the ball at (bx, by) in direction {@code fromBall} (radians), facing it. */
    static Pose standoffPose(double fromBall, double bx, double by, double standoff, Side side) {
        double px = bx + Math.cos(fromBall) * standoff;
        double py = by + Math.sin(fromBall) * standoff;
        double facing = fromBall + Math.PI;
        return new Pose(px, py, side == Side.BACK ? facing + Math.PI : facing);
    }

    /**
     * Where to stop for a ball: straight on from the robot if the robot may stop there
     * ({@link #whyNotStopAt}), else swung round the ball in 15° steps, nearest first, up to 90°
     * either way. A ball against a wall, approached at an angle, would put a front corner in the
     * wall; swinging round makes the robot come at it square. Null if no stop fits.
     */
    public static Pose stopFor(Pose robot, double bx, double by, Alliance alliance, DriveToBall.Settings settings) {
        Pose straight = standoffPose(robot, bx, by, settings.standoff, settings.side);
        return stopFrom(Math.atan2(straight.y() - by, straight.x() - bx), bx, by, alliance, settings);
    }

    /**
     * Where to stop for a ball in a group: facing through the ball at the rest of the group, so the
     * intake can later drive on through them all, if the rest lies beyond the ball as the robot
     * sees it. If the rest is on the robot's side (the ball is the far end of the group), going
     * round to face back at it would cost a detour, so it comes straight on ({@link #stopFor}).
     * Swung round like {@link #stopFor} if a stop doesn't fit.
     */
    public static Pose stopForGroup(Pose robot, double bx, double by, Group group, Alliance alliance, DriveToBall.Settings settings) {
        double gx = bx - group.x;
        double gy = by - group.y;
        boolean restBeyond = (gx * (robot.x() - bx) + gy * (robot.y() - by)) > 0;
        if (group.count < 2 || Math.hypot(gx, gy) < 1e-6 || !restBeyond) {
            return stopFor(robot, bx, by, alliance, settings);
        }
        Pose stop = stopFrom(Math.atan2(gy, gx), bx, by, alliance, settings);
        return stop != null ? stop : stopFor(robot, bx, by, alliance, settings);
    }

    /** The first legal stop {@code standoff} from the ball, from {@code direct} (radians from the ball), swinging out 15° at a time. */
    private static Pose stopFrom(double direct, double bx, double by, Alliance alliance, DriveToBall.Settings settings) {
        for (double swing = 0; swing <= MAX_SWING + 1e-9; swing += SWING_STEP) {
            for (double sign : swing == 0 ? new double[] {1} : new double[] {1, -1}) {
                Pose stop = standoffPose(direct + sign * swing, bx, by, settings.standoff, settings.side);
                if (whyNotStopAt(stop, alliance, settings.auto, settings.margin) == null) {
                    return stop;
                }
            }
        }
        return null;
    }

    /**
     * Why the robot can't stop at {@code stop}, or null if it can: its footprint
     * ({@link RobotSize}) must be inside the walls, and in AUTO on our half (G402) and
     * {@code hiveMargin} clear of the HIVE outline.
     */
    public static String whyNotStopAt(Pose stop, Alliance alliance, boolean auto, double hiveMargin) {
        double c = Math.cos(stop.heading());
        double s = Math.sin(stop.heading());
        double hl = RobotSize.LENGTH / 2;
        double hw = RobotSize.WIDTH / 2;
        KeepOut hive = Field.HIVE.grow(hiveMargin);
        double[][] corners = {{hl, hw}, {-hl, hw}, {-hl, -hw}, {hl, -hw}};
        for (double[] k : corners) {
            double x = stop.x() + k[0] * c - k[1] * s;
            double y = stop.y() + k[0] * s + k[1] * c;
            if (x < 0 || x > Field.SIZE || y < 0 || y > Field.SIZE) {
                return "the robot wouldn't fit inside the walls there";
            }
            if (auto && !Field.onOwnHalf(alliance, x)) {
                return "the robot would cross the centre line there (G402)";
            }
            if (auto && hive.contains(x, y)) {
                return "the robot would stop on the HIVE there";
            }
        }
        if (auto) {
            double[][] hiveCorners = {{hive.minX, hive.minY}, {hive.maxX, hive.minY}, {hive.maxX, hive.maxY}, {hive.minX, hive.maxY}};
            for (double[] h : hiveCorners) {
                double[] r = CameraMount.toRobotFrame(stop, h[0], h[1]);
                if (Math.abs(r[0]) < hl && Math.abs(r[1]) < hw) {
                    return "the robot would stop on the HIVE there";
                }
            }
        }
        return null;
    }

    /**
     * Which ball to go for. Candidates are the wanted colours, within range, allowed by
     * {@link PickupRules}, with a legal stop; each is timed by {@link DrivePlan} round
     * {@code settings.obstacles}. Normally the quickest wins. With {@code settings.preferGroups},
     * among the balls at most {@code settings.similarSeconds} slower than the quickest, the one in
     * the biggest group wins; within a group, the ball with the most balls in its box (the blob,
     * not a lone ball beside it); then the quicker. So three balls together beat one ball a little
     * nearer, and the robot heads for the blob and stops facing the group. Null if there is no
     * candidate.
     */
    public static Choice choose(List<BallTracker.Ball> balls, Pose robot, Alliance alliance, DriveToBall.Settings settings) {
        List<Choice> candidates = new ArrayList<>();
        double quickest = Double.POSITIVE_INFINITY;
        for (BallTracker.Ball b : balls) {
            if (whyNotChosen(b, robot, alliance, settings) != null) {
                continue;
            }
            Group group = settings.preferGroups ? group(b, balls, alliance, settings) : new Group(1, b.x(), b.y());
            Pose stop = stopForGroup(robot, b.x(), b.y(), group, alliance, settings);
            double seconds = DrivePlan.plan(robot, stop, settings.obstacles, settings.margin).seconds;
            candidates.add(new Choice(b, group, stop, seconds, "", ""));
            quickest = Math.min(quickest, seconds);
        }
        Choice best = null;
        for (Choice c : candidates) {
            boolean close = !settings.preferGroups || c.seconds <= quickest + settings.similarSeconds;
            if (close && (best == null || better(c, best, settings.preferGroups))) {
                best = c;
            }
        }
        if (best == null) {
            return null;
        }
        candidates.sort((a, b) -> Double.compare(a.seconds, b.seconds));
        StringBuilder all = new StringBuilder();
        for (Choice c : candidates) {
            all.append(all.length() == 0 ? "" : " | ").append(c.ball == best.ball ? "→ " : "").append(c.describe());
        }
        String why = best.group.count < 2 ? "" : String.format(Locale.US, "%d together", best.group.count)
                + (best.seconds > quickest + 0.05 ? String.format(Locale.US, ", %.1f s farther than the nearest", best.seconds - quickest) : "");
        return new Choice(best.ball, best.group, best.stop, best.seconds, why, all.toString());
    }

    /** {@code a} beats {@code b}: bigger group, then more balls in its own box, then quicker. */
    private static boolean better(Choice a, Choice b, boolean preferGroups) {
        if (preferGroups) {
            if (a.group.count != b.group.count) {
                return a.group.count > b.group.count;
            }
            if (a.ball.blob() != b.ball.blob()) {
                return a.ball.blob() > b.ball.blob();
            }
        }
        return a.seconds < b.seconds;
    }

    /**
     * {@code ball}'s group: it and every other ball within {@code settings.clusterRadius} that is a
     * wanted colour and allowed by {@link PickupRules}, each counted as its
     * {@link BallTracker.Ball#blob}. Range and stops don't matter here: once the robot is at the
     * group, those balls are within reach.
     */
    public static Group group(BallTracker.Ball ball, List<BallTracker.Ball> balls, Alliance alliance, DriveToBall.Settings settings) {
        int count = 0;
        double sx = 0;
        double sy = 0;
        for (BallTracker.Ball b : balls) {
            boolean member = b == ball || (b.id != ball.id
                    && Math.hypot(b.x() - ball.x(), b.y() - ball.y()) <= settings.clusterRadius
                    && settings.colors.wants(b.kind, alliance)
                    && PickupRules.whyNot(b.kind, b.x(), b.y(), alliance, settings.auto, settings.hiveMargin) == null);
            if (member) {
                count += b.blob();
                sx += b.x() * b.blob();
                sy += b.y() * b.blob();
            }
        }
        return count == 0 ? new Group(1, ball.x(), ball.y()) : new Group(count, sx / count, sy / count);
    }

    /** Why {@link #choose} passes over this ball, or null if it would consider it. */
    public static String whyNotChosen(BallTracker.Ball b, Pose robot, Alliance alliance, DriveToBall.Settings settings) {
        if (!settings.colors.wants(b.kind, alliance)) {
            return "not one of the colours asked for (" + settings.colors + ")";
        }
        double distance = Math.hypot(b.x() - robot.x(), b.y() - robot.y());
        if (distance > settings.within) {
            return String.format(Locale.US, "%.0f in away, more than within %.0f", distance, settings.within);
        }
        String rule = PickupRules.whyNot(b.kind, b.x(), b.y(), alliance, settings.auto, settings.hiveMargin);
        if (rule != null) {
            return rule;
        }
        if (stopFor(robot, b.x(), b.y(), alliance, settings) == null) {
            return "nowhere to stop for it: " + whyNotStopAt(standoffPose(robot, b.x(), b.y(), settings.standoff, settings.side),
                    alliance, settings.auto, settings.margin);
        }
        return null;
    }

    /**
     * Re-plan for a ball whose estimate has moved {@code moved} inches since the last plan, made
     * {@code sinceLastPlanMs} ago, with the robot {@code robotToBall} inches from it: only if it
     * moved enough, not too soon after the last plan, and not once the robot has committed.
     */
    public static boolean shouldReplan(double moved, double sinceLastPlanMs, double robotToBall, DriveToBall.Settings settings) {
        return robotToBall > settings.commit && moved > settings.replanMove && sinceLastPlanMs >= settings.replanEveryMs;
    }
}
