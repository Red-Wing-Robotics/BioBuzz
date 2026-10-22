/*
 * Adapted from Hive-Vision's BallTracker (https://github.com/hivevisionftc/Hive-Vision,
 * ftc_ball_chase_lib/final/BallTracker.java), Copyright (c) 2026 Harjas Sidhu, MIT License: see
 * THIRD_PARTY_NOTICES.md at the repo root. Kept: the confidence gate, colour-bound matching, and
 * confirmation over consecutive distinct camera frames. Changed: balls are tracked as field
 * positions rather than image angles, so a ball keeps its identity while the robot drives and
 * turns, and is remembered after it leaves the image.
 */
package org.firstinspires.ftc.teamcode.vision;

import com.pedropathing.math.Pose;

import org.firstinspires.ftc.teamcode.auto.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * The balls the camera has seen, as positions on the field in the alliance frame
 * (docs/coordinates.md). Each new frame is placed on the field from the robot's pose when it was
 * taken ({@link PoseHistory}); a detection near a tracked ball of the same kind is that ball seen
 * again, anything else is a new ball.
 *
 * A ball is <b>confirmed</b> once it has been seen in {@link Settings#confirmFrames} consecutive
 * distinct frames, which filters out one-frame mistakes such as a red bumper read as NECTAR. It is
 * forgotten when it should be in view but hasn't been seen for {@link Settings#lostMs}, or when it
 * hasn't been seen at all for {@link Settings#memoryMs}. Out of view (behind the robot, or too
 * close for the camera) it is remembered, which is how an approach finishes once the ball drops
 * below the image.
 *
 * Hardware-free: the Vision subsystem feeds it, tests feed it made-up frames.
 */
public final class BallTracker {

    /** The gates. Plain fields, so Vision can copy its Panels-tunable values in every frame. */
    public static final class Settings {
        /** Detections less sure than this, 0–1, are ignored. Hive-Vision's MIN_CONF. */
        public double minConfidence = 0.44;
        /** Consecutive distinct frames before a ball is trusted. */
        public int confirmFrames = 3;
        /** A detection this close, inches, to a tracked ball of the same kind is that ball. Hive-Vision's MERGE_RADIUS. */
        public double matchRadius = 4;
        /** Detections farther than this from the tracking centre, inches, are ignored: range error grows fast with distance. */
        public double maxRange = 40;
        /** Frames taken while the robot turns faster than this, degrees/s, are skipped: the pose at capture is least certain then. */
        public double maxTurnRateDeg = 120;
        /** Forget a ball that should be in view but hasn't been seen for this long, ms. */
        public double lostMs = 400;
        /** Forget a ball not seen at all for this long, ms, even out of view. */
        public double memoryMs = 3000;
        /**
         * A ball placed up to this far past a wall, inches, is taken to be against it: range
         * error is a few inches near the image's far edge, and balls often lie against walls.
         * Farther out it isn't on the field (someone's shirt, the next field) and is ignored.
         */
        public double wallTolerance = 4;
        /**
         * One ball's box area, as a multiple of {@link CameraMount#ballAreaPct}: how much bigger or
         * smaller the model's boxes are than the ball. Calibrate with one ball on the vision bench.
         */
        public double blobAreaScale = 1.0;
        /** The most balls one box is taken to hold. */
        public int maxBlob = 6;
        /**
         * The confidence a box at least {@link #blobBalls} balls big needs, 0–1. A model trained on
         * single balls scores a clump of touching balls lower, and on 2026-10-22 dropped a blob as
         * unsure. Such a box still has to be confirmed over {@link #confirmFrames} frames.
         */
        public double blobMinConfidence = 0.25;
        /** A box this many balls big, or more, gets {@link #blobMinConfidence}. */
        public double blobBalls = 1.6;
    }

    /** One ball on the field. */
    public static final class Ball {
        public final int id;
        public final BallKind kind;
        private double x;
        private double y;
        private double confidence;
        private double lastSeenMs;
        private int streak;
        private int sightings;
        private boolean confirmed;
        private double blobRatio = 1;
        private int blob = 1;
        private double minX;
        private double maxX;
        private double minY;
        private double maxY;

        Ball(int id, BallKind kind, double[] at, double confidence, double seenMs) {
            this.id = id;
            this.kind = kind;
            this.x = at[0];
            this.y = at[1];
            this.minX = maxX = at[0];
            this.minY = maxY = at[1];
            this.confidence = confidence;
            this.lastSeenMs = seenMs;
            this.streak = 1;
            this.sightings = 1;
        }

        /** Field position, inches, alliance frame. */
        public double x() { return x; }
        public double y() { return y; }
        /** The confidence of the latest sighting, 0–1. */
        public double confidence() { return confidence; }
        public double lastSeenMs() { return lastSeenMs; }
        /** Frames it has been seen in. */
        public int sightings() { return sightings; }
        public boolean confirmed() { return confirmed; }
        /**
         * How many balls this is, from its box's size: 1 for a lone ball, more when touching balls
         * come as one box. A smoothed estimate, rounded.
         */
        public int blob() { return blob; }

        /**
         * How far apart its sightings have been, inches: the diagonal of the box round them all. A
         * ball that hasn't moved and a well-measured mount keep this small; the vision bench's
         * spin test uses it to tune the latency.
         */
        public double spread() {
            return Math.hypot(maxX - minX, maxY - minY);
        }

        private void seen(double[] at, double conf, double seenMs, int confirmFrames) {
            // Each new sighting moves the estimate halfway: later ones are usually closer, so better.
            x += (at[0] - x) / 2;
            y += (at[1] - y) / 2;
            minX = Math.min(minX, at[0]);
            maxX = Math.max(maxX, at[0]);
            minY = Math.min(minY, at[1]);
            maxY = Math.max(maxY, at[1]);
            confidence = conf;
            lastSeenMs = seenMs;
            streak++;
            sightings++;
            confirmed |= streak >= confirmFrames;
        }

        private void missed() {
            streak = 0;
        }

        /** A box {@code ratio} times one ball's: smoothed like the position, then rounded to a count. */
        void sized(double ratio, int maxBlob, boolean first) {
            if (Double.isNaN(ratio)) {
                return;
            }
            blobRatio = first ? ratio : blobRatio + (ratio - blobRatio) / 2;
            blob = (int) Math.max(1, Math.min(maxBlob, Math.round(blobRatio)));
        }
    }

    private final Settings settings;
    private final List<Ball> balls = new ArrayList<>();
    private double lastFrame = Double.NaN;
    private int nextId = 1;
    private String lastSummary = "no frame yet";

    public BallTracker(Settings settings) {
        this.settings = settings;
    }

    /**
     * Take in one frame, if it is new: {@code robotAtCapture} is where the robot was when it was
     * taken (null if unknown), {@code turnRate} how fast it was turning then (rad/s), and
     * {@code nowMs} the current time on the same clock as {@link Frame#captureMs}.
     *
     * @return false if this frame was already taken in
     */
    public boolean update(Frame frame, Pose robotAtCapture, double turnRate, CameraMount mount, double nowMs) {
        if (frame.id == lastFrame) {
            return false;
        }
        lastFrame = frame.id;
        boolean steady = robotAtCapture != null && Math.abs(Math.toDegrees(turnRate)) <= settings.maxTurnRateDeg;

        List<Ball> seen = new ArrayList<>();
        List<String> unsureNotes = new ArrayList<>();
        int unsure = 0;
        int edge = 0;
        int far = 0;
        int outside = 0;
        if (steady) {
            for (Detection d : frame.detections) {
                if (mount.nearEdge(d.txDeg, d.tyDeg)) {
                    edge++;
                    continue;
                }
                double[] r = mount.toRobot(d.txDeg, d.tyDeg, d.kind.centreHeight());
                if (r == null || Math.hypot(r[0], r[1]) > settings.maxRange) {
                    far++;
                    continue;
                }
                double ratio = d.areaPct > 0
                        ? d.areaPct / (mount.ballAreaPct(r[0], r[1], d.kind) * settings.blobAreaScale)
                        : Double.NaN;
                boolean blob = !Double.isNaN(ratio) && ratio >= settings.blobBalls;
                if (d.confidence < (blob ? settings.blobMinConfidence : settings.minConfidence)) {
                    unsure++;
                    unsureNotes.add(String.format(Locale.US, "%.0f%%%s", d.confidence * 100,
                            Double.isNaN(ratio) ? "" : String.format(Locale.US, " ≈%.1f balls", ratio)));
                    continue;
                }
                double[] at = onField(CameraMount.toField(robotAtCapture, r), d.kind);
                if (at == null) {
                    outside++;
                    continue;
                }
                Ball match = nearest(d.kind, at, seen);
                if (match != null) {
                    match.seen(at, d.confidence, frame.captureMs, settings.confirmFrames);
                    match.sized(ratio, settings.maxBlob, false);
                    seen.add(match);
                } else {
                    Ball b = new Ball(nextId++, d.kind, at, d.confidence, frame.captureMs);
                    b.confirmed = settings.confirmFrames <= 1;
                    b.sized(ratio, settings.maxBlob, true);
                    balls.add(b);
                    seen.add(b);
                }
            }
        }
        lastSummary = summary(frame.detections.size(), robotAtCapture == null ? "no robot pose" : !steady ? "turning too fast" : null,
                frame.detections.size() - unsure - edge - far - outside, unsureNotes, edge, far, outside);

        for (Iterator<Ball> it = balls.iterator(); it.hasNext(); ) {
            Ball b = it.next();
            if (seen.contains(b)) {
                continue;
            }
            double unseen = nowMs - b.lastSeenMs;
            boolean inView = steady && inView(b, robotAtCapture, mount);
            if (inView) {
                b.missed();
            }
            if (unseen > settings.memoryMs || (inView && unseen > settings.lostMs)) {
                it.remove();
            }
        }
        return true;
    }

    /**
     * What happened to the latest frame's detections, for the Driver Station, e.g.
     * "4 seen, 0 used: 4 at the image edge". Says at once why the camera's balls aren't being used.
     */
    public String lastFrameSummary() {
        return lastSummary;
    }

    /** Every tracked ball, confirmed or not. */
    public List<Ball> balls() {
        return Collections.unmodifiableList(balls);
    }

    /** The balls seen in enough consecutive frames to trust. */
    public List<Ball> confirmed() {
        List<Ball> out = new ArrayList<>();
        for (Ball b : balls) {
            if (b.confirmed) {
                out.add(b);
            }
        }
        return out;
    }

    /** The tracked ball with this id, or null once it has been forgotten. */
    public Ball find(int id) {
        for (Ball b : balls) {
            if (b.id == id) {
                return b;
            }
        }
        return null;
    }

    /** Forget every ball. */
    public void clear() {
        balls.clear();
    }

    /** The tracked ball of {@code kind} nearest {@code at} within the match radius, not already matched this frame. */
    private Ball nearest(BallKind kind, double[] at, List<Ball> taken) {
        Ball best = null;
        double bestGap = settings.matchRadius;
        for (Ball b : balls) {
            if (b.kind != kind || taken.contains(b)) {
                continue;
            }
            double gap = Math.hypot(b.x - at[0], b.y - at[1]);
            if (gap <= bestGap) {
                best = b;
                bestGap = gap;
            }
        }
        return best;
    }

    /** {@code at}, kept inside the walls if it is just past one; null if it is well outside the field. */
    private double[] onField(double[] at, BallKind kind) {
        double r = kind.diameter / 2;
        double t = settings.wallTolerance;
        if (at[0] < -t || at[0] > Field.SIZE + t || at[1] < -t || at[1] > Field.SIZE + t) {
            return null;
        }
        return new double[] {Math.max(r, Math.min(Field.SIZE - r, at[0])), Math.max(r, Math.min(Field.SIZE - r, at[1]))};
    }

    private static String summary(int seen, String skipped, int used, List<String> unsure, int edge, int far, int outside) {
        if (seen == 0) {
            return "no balls in the frame";
        }
        if (skipped != null) {
            return seen + " seen, none used: " + skipped;
        }
        StringBuilder s = new StringBuilder().append(seen).append(" seen, ").append(used).append(" used");
        String sep = ": ";
        if (edge > 0) {
            s.append(sep).append(edge).append(" at the image edge");
            sep = ", ";
        }
        if (!unsure.isEmpty()) {
            s.append(sep).append(unsure.size()).append(" unsure (").append(String.join(", ", unsure)).append(')');
            sep = ", ";
        }
        if (far > 0) {
            s.append(sep).append(far).append(" too far");
            sep = ", ";
        }
        if (outside > 0) {
            s.append(sep).append(outside).append(" beyond the walls");
        }
        return s.toString();
    }

    private boolean inView(Ball b, Pose robot, CameraMount mount) {
        double[] r = CameraMount.toRobotFrame(robot, b.x, b.y);
        return Math.hypot(r[0], r[1]) <= settings.maxRange && mount.sees(r[0], r[1], b.kind.centreHeight());
    }
}
