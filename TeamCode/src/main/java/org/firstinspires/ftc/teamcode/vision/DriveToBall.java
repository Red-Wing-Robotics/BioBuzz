package org.firstinspires.ftc.teamcode.vision;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.AutoContext;
import org.firstinspires.ftc.teamcode.auto.KeepOut;
import org.firstinspires.ftc.teamcode.auto.Step;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Drive to a ball the camera sees, and stop {@link Settings#standoff} short of it with the intake
 * side facing it. Never sleeps or loops: the OpMode's loop drives it like any other {@link Step}.
 *
 * <ol>
 * <li><b>Look</b> for at least {@link Settings#settleSeconds}, so every ball in view has had
 *     time to be confirmed, and up to {@link Settings#lookSeconds}, for a confirmed, legal ball
 *     ({@link PickupRules}). None: it finishes with {@link Outcome#NO_TARGET}, so the route
 *     carries on.</li>
 * <li><b>Drive</b> to the best one ({@link BallApproach#choose}), re-planning while the
 *     ball's position firms up ({@link BallApproach#shouldReplan}), and switching to a bigger
 *     group if one is confirmed on the way. If the ball turns out to be somewhere it may not go,
 *     it stops with {@link Outcome#ABANDONED}.</li>
 * <li><b>Commit</b> within {@link Settings#commit} of the ball: the target is frozen and the
 *     drive finishes on odometry, since the ball soon drops out of the bottom of the image.</li>
 * </ol>
 *
 * The drive itself is a Step from {@code drive} for each stop pose; on the robot that is
 * {@code Steps.driveTo}, planned round the obstacles in the settings. A re-plan ends the old drive
 * as finished, not interrupted, so the follower keeps moving until the new path replaces its own.
 */
public final class DriveToBall implements Step {

    public enum Outcome { RUNNING, ARRIVED, NO_TARGET, ABANDONED }

    /** How a vision drive behaves. Defaults come from VisionTuning; route args override some ({@link ToBallArgs}). */
    public static final class Settings {
        public PickupRules.Colors colors = PickupRules.Colors.OURS;
        /** Only balls this close to the robot's tracking centre, inches. */
        public double within = 36;
        /** Stop with the tracking centre this far from the ball's centre, inches. */
        public double standoff = 9;
        /** How long to wait for a ball before giving up, seconds. */
        public double lookSeconds = 1.0;
        /**
         * Look at least this long before choosing, seconds. A ball is confirmed after 3 frames,
         * so choosing at the first confirmed ball meant choosing before the rest were (2026-10-29).
         */
        public double settleSeconds = 0.5;
        /** Closer than this to the ball, inches, the target is frozen. */
        public double commit = 18;
        /** Re-plan when the ball's estimate has moved this far, inches... */
        public double replanMove = 2;
        /** ...but no more often than this, ms. */
        public double replanEveryMs = 250;
        /** True if this drive keeps to our half (AUTO with {@link #ownHalfOnly}). */
        public boolean keepsToOwnHalf() {
            return auto && ownHalfOnly;
        }

        /** Path speed for the drive, as a fraction of the robot's top speed (1 = full). */
        public double speed = 1;
        /** Prefer a ball in a bigger group when it's about as quick to reach ({@link BallApproach#choose}). */
        public boolean preferGroups = true;
        /** Balls within this of each other, inches, count as one group: touching, or nearly. */
        public double clusterRadius = 6;
        /** A bigger group wins if it's at most this much slower to reach than the quickest ball, seconds. */
        public double similarSeconds = 0.5;
        /** Every part of the robot keeps this far, inches, from a ball it isn't going for: NECTAR on a POLLEN seek ({@link BallApproach#whyInTheWay}). */
        public double avoidMargin = 3;
        /** Which end of the robot takes the ball in. */
        public BallApproach.Side side = BallApproach.Side.FRONT;
        /** AUTO's rules: no stopping near the HIVE, and (with {@link #ownHalfOnly}) our half only. */
        public boolean auto = true;
        /** In AUTO, balls and stops on our half only (G402). Off only for testing, never in a comp build (Vision). */
        public boolean ownHalfOnly = true;
        /** The HIVE's outline grown by this, inches, for the G409 check on a ball. */
        public double hiveMargin = 0;
        /** What the drive plans round, and how far from them it keeps every part of the robot. */
        public List<KeepOut> obstacles = Collections.emptyList();
        public double margin = 0;

        public Settings copy() {
            Settings s = new Settings();
            s.colors = colors;
            s.within = within;
            s.standoff = standoff;
            s.lookSeconds = lookSeconds;
            s.settleSeconds = settleSeconds;
            s.commit = commit;
            s.replanMove = replanMove;
            s.replanEveryMs = replanEveryMs;
            s.speed = speed;
            s.preferGroups = preferGroups;
            s.clusterRadius = clusterRadius;
            s.similarSeconds = similarSeconds;
            s.avoidMargin = avoidMargin;
            s.side = side;
            s.auto = auto;
            s.ownHalfOnly = ownHalfOnly;
            s.hiveMargin = hiveMargin;
            s.obstacles = obstacles;
            s.margin = margin;
            return s;
        }
    }

    private final Balls balls;
    private final Supplier<Pose> robot;
    private final Function<Pose, Step> drive;
    private final Alliance alliance;
    private final Settings settings;

    private Outcome outcome = Outcome.RUNNING;
    private String why;
    private double startTime;
    private double planTime;
    private double chooseTime;
    private int targetId;
    private BallKind targetKind;
    private double targetX;
    private double targetY;
    private int groupCount = 1;
    private String chosenWhy = "";
    private String candidates;
    private boolean committed;
    private Step driving;
    private Pose stop;

    public DriveToBall(Balls balls, Supplier<Pose> robot, Function<Pose, Step> drive, Alliance alliance, Settings settings) {
        this.balls = balls;
        this.robot = robot;
        this.drive = drive;
        this.alliance = alliance;
        this.settings = settings;
    }

    @Override
    public void start(AutoContext ctx) {
        outcome = Outcome.RUNNING;
        why = null;
        startTime = ctx.time();
        driving = null;
        candidates = null;
        committed = false;
    }

    @Override
    public boolean update(AutoContext ctx) {
        double now = ctx.time();
        if (driving == null) {
            if (now - startTime < settings.settleSeconds) {
                return false;
            }
            BallApproach.Choice choice = BallApproach.choose(balls.confirmed(), balls.atEdge(), robot.get(), alliance, settings);
            if (choice == null) {
                if (now - startTime >= Math.max(settings.lookSeconds, settings.settleSeconds)) {
                    return finish(Outcome.NO_TARGET, whyNone());
                }
                return false;
            }
            take(choice, "");
            begin(choice.stop, ctx, now);
        } else if (!committed) {
            BallTracker.Ball ball = balls.find(targetId);
            Pose here = robot.get();
            List<BallTracker.Ball> avoid = BallApproach.avoided(balls.confirmed(), balls.atEdge(), alliance, settings);
            BallApproach.Choice bigger = biggerGroup(here, now);
            String inTheWay = bigger == null ? BallApproach.whyInTheWay(here, stop, avoid, settings) : null;
            if (bigger != null) {
                take(bigger, "switched: ");
                driving.end(ctx, false);
                begin(bigger.stop, ctx, now);
            } else if (inTheWay != null) {
                // A ball to keep clear of, confirmed since the pick: choose again round it.
                BallApproach.Choice again = BallApproach.choose(balls.confirmed(), balls.atEdge(), here, alliance, settings);
                if (again == null) {
                    return abandon(ctx, inTheWay);
                }
                take(again, "round a ball in the way: ");
                driving.end(ctx, false);
                begin(again.stop, ctx, now);
            } else if (ball != null) {
                String bad = PickupRules.whyNot(ball.kind, ball.x(), ball.y(), alliance, settings.keepsToOwnHalf(), settings.hiveMargin);
                if (bad != null) {
                    return abandon(ctx, "the ball is " + bad);
                }
                double moved = Math.hypot(ball.x() - targetX, ball.y() - targetY);
                double toBall = Math.hypot(here.x() - ball.x(), here.y() - ball.y());
                if (BallApproach.shouldReplan(moved, (now - planTime) * 1000, toBall, settings)) {
                    targetX = ball.x();
                    targetY = ball.y();
                    BallApproach.Group group = settings.preferGroups
                            ? BallApproach.group(ball, balls.confirmed(), alliance, settings)
                            : new BallApproach.Group(1, targetX, targetY);
                    groupCount = group.count;
                    Pose next = BallApproach.stopForGroup(here, targetX, targetY, group, alliance, settings, avoid);
                    if (next == null) {
                        return abandon(ctx, BallApproach.whyNotChosen(ball, here, alliance, settings, avoid));
                    }
                    driving.end(ctx, false);
                    begin(next, ctx, now);
                }
            }
            committed = Math.hypot(here.x() - targetX, here.y() - targetY) <= settings.commit;
        }
        if (driving.update(ctx)) {
            driving.end(ctx, false);
            return finish(Outcome.ARRIVED, null);
        }
        return false;
    }

    @Override
    public void end(AutoContext ctx, boolean interrupted) {
        if (interrupted && outcome == Outcome.RUNNING && driving != null) {
            driving.end(ctx, true);
        }
    }

    @Override
    public String name() {
        switch (outcome) {
            case ARRIVED:
                return "at the " + kindName() + (groupCount > 1 ? " (" + groupCount + " together)" : "");
            case NO_TARGET:
            case ABANDONED:
                return "no ball: " + why;
            default:
                if (driving == null) {
                    return "looking for a ball";
                }
                return String.format(Locale.US, "to the %s at (%.0f, %.0f)%s%s", kindName(), targetX, targetY,
                        chosenWhy.isEmpty() ? "" : ": " + chosenWhy, committed ? ", closing in" : "");
        }
    }

    public Outcome outcome() {
        return outcome;
    }

    /** Every ball it weighed when it picked, the chosen one marked "→"; null before it picks. */
    public String candidates() {
        return candidates;
    }

    /** Why it stopped without a ball (NO_TARGET, ABANDONED), else null. */
    public String why() {
        return why;
    }

    /** Why no ball was chosen: none confirmed, or what ruled out the nearest one (of the colours asked for, if any). */
    private String whyNone() {
        Pose here = robot.get();
        BallTracker.Ball nearest = null;
        for (BallTracker.Ball b : balls.confirmed()) {
            boolean wanted = settings.colors.wants(b.kind, alliance);
            boolean nearestWanted = nearest != null && settings.colors.wants(nearest.kind, alliance);
            if (nearest == null || (wanted && !nearestWanted) || (wanted == nearestWanted
                    && Math.hypot(b.x() - here.x(), b.y() - here.y()) < Math.hypot(nearest.x() - here.x(), nearest.y() - here.y()))) {
                nearest = b;
            }
        }
        if (nearest == null) {
            return String.format(Locale.US, "no ball confirmed in %.1f s", settings.lookSeconds);
        }
        return String.format(Locale.US, "nearest ball, %s at (%.0f, %.0f): %s", nearest.kind, nearest.x(), nearest.y(),
                BallApproach.whyNotChosen(nearest, here, alliance, settings, BallApproach.avoided(balls.confirmed(), balls.atEdge(), alliance, settings)));
    }

    private void take(BallApproach.Choice choice, String prefix) {
        targetId = choice.ball.id;
        targetKind = choice.ball.kind;
        targetX = choice.ball.x();
        targetY = choice.ball.y();
        groupCount = choice.group.count;
        chosenWhy = prefix + choice.why;
        candidates = prefix + choice.candidates;
    }

    /**
     * On the way, at most every {@link Settings#replanEveryMs}: a choice in a bigger group than the
     * target's, away from it, that has been confirmed since the pick. Null if there is none, or
     * groups don't count.
     */
    private BallApproach.Choice biggerGroup(Pose here, double now) {
        if (!settings.preferGroups || (now - chooseTime) * 1000 < settings.replanEveryMs) {
            return null;
        }
        chooseTime = now;
        BallApproach.Choice c = BallApproach.choose(balls.confirmed(), balls.atEdge(), here, alliance, settings);
        if (c == null || c.ball.id == targetId || c.group.count <= groupCount
                || Math.hypot(c.ball.x() - targetX, c.ball.y() - targetY) <= settings.clusterRadius) {
            return null;
        }
        return c;
    }

    private void begin(Pose stop, AutoContext ctx, double now) {
        this.stop = stop;
        driving = drive.apply(stop);
        driving.start(ctx);
        planTime = now;
        chooseTime = now;
    }

    private boolean abandon(AutoContext ctx, String reason) {
        driving.end(ctx, true);
        return finish(Outcome.ABANDONED, reason);
    }

    private boolean finish(Outcome result, String reason) {
        outcome = result;
        why = reason;
        return true;
    }

    private String kindName() {
        return targetKind == null ? "ball" : targetKind.name().replace('_', ' ');
    }
}
