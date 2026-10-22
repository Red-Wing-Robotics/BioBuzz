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
 * <li><b>Look</b> for up to {@link Settings#lookSeconds} for a confirmed, legal ball
 *     ({@link PickupRules}). None: it finishes at once with {@link Outcome#NO_TARGET}, so the
 *     route carries on.</li>
 * <li><b>Drive</b> to the quickest one ({@link BallApproach#choose}), re-planning while the
 *     ball's position firms up ({@link BallApproach#shouldReplan}). If the ball turns out to be
 *     somewhere it may not go, it stops with {@link Outcome#ABANDONED}.</li>
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
        /** Closer than this to the ball, inches, the target is frozen. */
        public double commit = 18;
        /** Re-plan when the ball's estimate has moved this far, inches... */
        public double replanMove = 2;
        /** ...but no more often than this, ms. */
        public double replanEveryMs = 250;
        /** Prefer a ball in a bigger group when it's about as quick to reach ({@link BallApproach#choose}). */
        public boolean preferGroups = true;
        /** Balls within this of each other, inches, count as one group: touching, or nearly. */
        public double clusterRadius = 6;
        /** A bigger group wins if it's at most this much slower to reach than the quickest ball, seconds. */
        public double similarSeconds = 0.5;
        /** Which end of the robot takes the ball in. */
        public BallApproach.Side side = BallApproach.Side.FRONT;
        /** AUTO's rules: our half only (G402), and no stopping near the HIVE. */
        public boolean auto = true;
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
            s.commit = commit;
            s.replanMove = replanMove;
            s.replanEveryMs = replanEveryMs;
            s.preferGroups = preferGroups;
            s.clusterRadius = clusterRadius;
            s.similarSeconds = similarSeconds;
            s.side = side;
            s.auto = auto;
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
    private int targetId;
    private BallKind targetKind;
    private double targetX;
    private double targetY;
    private int groupCount = 1;
    private String chosenWhy = "";
    private String candidates;
    private boolean committed;
    private Step driving;

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
            BallApproach.Choice choice = BallApproach.choose(balls.confirmed(), robot.get(), alliance, settings);
            if (choice == null) {
                if (now - startTime >= settings.lookSeconds) {
                    return finish(Outcome.NO_TARGET, whyNone());
                }
                return false;
            }
            targetId = choice.ball.id;
            targetKind = choice.ball.kind;
            targetX = choice.ball.x();
            targetY = choice.ball.y();
            groupCount = choice.group.count;
            chosenWhy = choice.why;
            candidates = choice.candidates;
            begin(choice.stop, ctx, now);
        } else if (!committed) {
            BallTracker.Ball ball = balls.find(targetId);
            Pose here = robot.get();
            if (ball != null) {
                String bad = PickupRules.whyNot(ball.kind, ball.x(), ball.y(), alliance, settings.auto, settings.hiveMargin);
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
                    Pose stop = BallApproach.stopForGroup(here, targetX, targetY, group, alliance, settings);
                    if (stop == null) {
                        return abandon(ctx, BallApproach.whyNotChosen(ball, here, alliance, settings));
                    }
                    driving.end(ctx, false);
                    begin(stop, ctx, now);
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

    /** Why no ball was chosen: none confirmed, or what ruled out the nearest one. */
    private String whyNone() {
        Pose here = robot.get();
        BallTracker.Ball nearest = null;
        for (BallTracker.Ball b : balls.confirmed()) {
            if (nearest == null || Math.hypot(b.x() - here.x(), b.y() - here.y()) < Math.hypot(nearest.x() - here.x(), nearest.y() - here.y())) {
                nearest = b;
            }
        }
        if (nearest == null) {
            return String.format(Locale.US, "no ball confirmed in %.1f s", settings.lookSeconds);
        }
        return String.format(Locale.US, "nearest ball, %s at (%.0f, %.0f): %s", nearest.kind, nearest.x(), nearest.y(),
                BallApproach.whyNotChosen(nearest, here, alliance, settings));
    }

    private void begin(Pose stop, AutoContext ctx, double now) {
        driving = drive.apply(stop);
        driving.start(ctx);
        planTime = now;
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
