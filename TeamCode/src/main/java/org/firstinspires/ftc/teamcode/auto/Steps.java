package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.api.Paths;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Factories for building autonomous routines declaratively. Import statically:
 * {@code import static org.firstinspires.ftc.teamcode.auto.Steps.*;}
 *
 * <pre>
 * sequence(
 *     parallel(follow("to shoot", line(START, SHOOT)), shooter.spinUp()),
 *     shooter.fire(3),
 *     withTimeout(4, follow("to park", line(SHOOT, PARK))))
 * </pre>
 */
public final class Steps {

    private Steps() {}

    // ---- composition ----------------------------------------------------------------

    /** Runs steps one after another. Steps that finish instantly don't cost a loop. */
    public static Step sequence(Step... steps) {
        return new Sequence(Arrays.asList(steps));
    }

    /** Runs steps at the same time; finishes when all of them have finished. */
    public static Step parallel(Step... steps) {
        return new Parallel(Arrays.asList(steps), false);
    }

    /** Runs steps at the same time; finishes when the first one does and interrupts the rest. */
    public static Step race(Step... steps) {
        return new Parallel(Arrays.asList(steps), true);
    }

    /** Interrupts {@code step} if it hasn't finished within {@code seconds}. */
    public static Step withTimeout(double seconds, Step step) {
        return named(step.name() + " (≤" + seconds + "s)", race(step, waitSeconds(seconds)));
    }

    public static Step named(String name, Step step) {
        return new Named(name, step);
    }

    // ---- primitives -----------------------------------------------------------------

    public static Step waitSeconds(double seconds) {
        return new WaitSeconds(seconds);
    }

    public static Step waitUntil(String name, BooleanSupplier condition) {
        return new WaitUntil(name, condition);
    }

    /** Runs {@code action} once and finishes immediately. */
    public static Step instant(String name, Runnable action) {
        return new Instant(name, action);
    }

    // ---- driving --------------------------------------------------------------------

    /** Follows a Pedro path; finishes when the follower reports the path complete. */
    public static Step follow(String name, Path path) {
        return new FollowPath(name, path);
    }

    /** Holds a pose (in the alliance frame) until interrupted, e.g. by {@link #withTimeout}. */
    public static Step hold(String name, Pose pose) {
        return new HoldPose(name, pose);
    }

    /**
     * Drives a straight line from wherever the robot is when the step starts to {@code target}
     * (alliance frame), turning to its heading on the way. For TeleOp assists, whose start isn't
     * known in advance. Finishes at once if the robot is already within 1 in of the target.
     */
    public static Step driveTo(String name, Pose target) {
        return new DriveTo(name, target);
    }

    // ---- implementations ------------------------------------------------------------

    static final class Sequence implements Step {
        private final List<Step> steps;
        private int index;

        Sequence(List<Step> steps) {
            this.steps = steps;
        }

        @Override
        public void start(AutoContext ctx) {
            index = 0;
            if (!steps.isEmpty()) {
                steps.get(0).start(ctx);
            }
        }

        @Override
        public boolean update(AutoContext ctx) {
            while (index < steps.size()) {
                Step current = steps.get(index);
                if (!current.update(ctx)) {
                    return false;
                }
                current.end(ctx, false);
                index++;
                if (index < steps.size()) {
                    steps.get(index).start(ctx);
                }
            }
            return true;
        }

        @Override
        public void end(AutoContext ctx, boolean interrupted) {
            if (interrupted && index < steps.size()) {
                steps.get(index).end(ctx, true);
            }
        }

        @Override
        public String name() {
            return index < steps.size() ? steps.get(index).name() : "done";
        }
    }

    static final class Parallel implements Step {
        private final List<Step> steps;
        private final boolean finishOnFirst;
        private boolean[] done;

        Parallel(List<Step> steps, boolean finishOnFirst) {
            this.steps = steps;
            this.finishOnFirst = finishOnFirst;
        }

        @Override
        public void start(AutoContext ctx) {
            done = new boolean[steps.size()];
            for (Step step : steps) {
                step.start(ctx);
            }
        }

        @Override
        public boolean update(AutoContext ctx) {
            boolean allDone = true;
            for (int i = 0; i < steps.size(); i++) {
                if (done[i]) {
                    continue;
                }
                if (steps.get(i).update(ctx)) {
                    done[i] = true;
                    steps.get(i).end(ctx, false);
                    if (finishOnFirst) {
                        interruptRemaining(ctx);
                        return true;
                    }
                } else {
                    allDone = false;
                }
            }
            return allDone;
        }

        @Override
        public void end(AutoContext ctx, boolean interrupted) {
            if (interrupted) {
                interruptRemaining(ctx);
            }
        }

        private void interruptRemaining(AutoContext ctx) {
            for (int i = 0; i < steps.size(); i++) {
                if (!done[i]) {
                    done[i] = true;
                    steps.get(i).end(ctx, true);
                }
            }
        }

        @Override
        public String name() {
            List<String> active = new ArrayList<>();
            for (int i = 0; i < steps.size(); i++) {
                if (done == null || !done[i]) {
                    active.add(steps.get(i).name());
                }
            }
            return (finishOnFirst ? "race" : "parallel") + active;
        }
    }

    static final class Named implements Step {
        private final String name;
        private final Step step;

        Named(String name, Step step) {
            this.name = name;
            this.step = step;
        }

        @Override public void start(AutoContext ctx) { step.start(ctx); }
        @Override public boolean update(AutoContext ctx) { return step.update(ctx); }
        @Override public void end(AutoContext ctx, boolean interrupted) { step.end(ctx, interrupted); }
        @Override public String name() { return name; }
    }

    static final class WaitSeconds implements Step {
        private final double seconds;
        private double startTime;

        WaitSeconds(double seconds) {
            this.seconds = seconds;
        }

        @Override public void start(AutoContext ctx) { startTime = ctx.time(); }
        @Override public boolean update(AutoContext ctx) { return ctx.time() - startTime >= seconds; }
        @Override public String name() { return "wait " + seconds + "s"; }
    }

    static final class WaitUntil implements Step {
        private final String name;
        private final BooleanSupplier condition;

        WaitUntil(String name, BooleanSupplier condition) {
            this.name = name;
            this.condition = condition;
        }

        @Override public boolean update(AutoContext ctx) { return condition.getAsBoolean(); }
        @Override public String name() { return "until " + name; }
    }

    static final class Instant implements Step {
        private final String name;
        private final Runnable action;
        private boolean ran;

        Instant(String name, Runnable action) {
            this.name = name;
            this.action = action;
        }

        @Override public void start(AutoContext ctx) { ran = false; }

        @Override
        public boolean update(AutoContext ctx) {
            if (!ran) {
                ran = true;
                action.run();
            }
            return true;
        }

        @Override public String name() { return name; }
    }

    static final class FollowPath implements Step {
        private final String name;
        private final Path path;

        FollowPath(String name, Path path) {
            this.name = name;
            this.path = path;
        }

        @Override public void start(AutoContext ctx) { ctx.follower.follow(path); }

        /** The Follower leaves FOLLOW mode by itself (to HOLD or IDLE) once the path is done. */
        @Override public boolean update(AutoContext ctx) { return !ctx.follower.following(); }

        @Override
        public void end(AutoContext ctx, boolean interrupted) {
            if (interrupted) {
                ctx.follower.stop();
            }
        }

        @Override public String name() { return "follow " + name; }
    }

    static final class HoldPose implements Step {
        private final String name;
        private final Pose pose;

        HoldPose(String name, Pose pose) {
            this.name = name;
            this.pose = pose;
        }

        @Override public void start(AutoContext ctx) { ctx.follower.hold(pose); }
        @Override public boolean update(AutoContext ctx) { return false; }
        @Override public void end(AutoContext ctx, boolean interrupted) { ctx.follower.stop(); }
        @Override public String name() { return "hold " + name; }
    }

    static final class DriveTo implements Step {
        private static final double ARRIVED_INCHES = 1.0;

        private final String name;
        private final Pose target;
        private boolean moving;

        DriveTo(String name, Pose target) {
            this.name = name;
            this.target = target;
        }

        @Override
        public void start(AutoContext ctx) {
            Pose from = ctx.follower.pose();
            // A zero-length path has no direction to follow, so "already there" is simply done.
            moving = Math.hypot(target.x() - from.x(), target.y() - from.y()) >= ARRIVED_INCHES;
            if (moving) {
                ctx.follower.follow(Paths.line(from, target).linear(from, target));
            }
        }

        @Override
        public boolean update(AutoContext ctx) {
            return !moving || !ctx.follower.following();
        }

        @Override
        public void end(AutoContext ctx, boolean interrupted) {
            if (interrupted && moving) {
                ctx.follower.stop();
            }
        }

        @Override public String name() { return "drive to " + name; }
    }
}
