package org.firstinspires.ftc.teamcode.subsystems;

import com.google.gson.JsonObject;
import com.pedropathing.math.Pose;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.RobotHardware;
import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.AutoClearance;
import org.firstinspires.ftc.teamcode.auto.KeepOut;
import org.firstinspires.ftc.teamcode.auto.Step;
import org.firstinspires.ftc.teamcode.auto.Steps;
import org.firstinspires.ftc.teamcode.vision.BallKind;
import org.firstinspires.ftc.teamcode.vision.BallTracker;
import org.firstinspires.ftc.teamcode.vision.Balls;
import org.firstinspires.ftc.teamcode.vision.Detection;
import org.firstinspires.ftc.teamcode.vision.DriveToBall;
import org.firstinspires.ftc.teamcode.vision.Frame;
import org.firstinspires.ftc.teamcode.vision.PickupRules;
import org.firstinspires.ftc.teamcode.vision.PoseHistory;
import org.firstinspires.ftc.teamcode.vision.ToBallArgs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The Limelight 3A. With {@link Job#BALLS} it runs Hive-Vision's ball model (vision/readme.md):
 * each new frame becomes balls on the field ({@link BallTracker}), and {@link #toBall} drives to
 * one. The only class that touches the SDK's Limelight types; the decisions are in vision/, which
 * is hardware-free and unit-tested.
 *
 * Kept off the loop: {@code getLatestResult()} doesn't block (the SDK polls the camera on its own
 * thread), and is read once per loop. The pipeline switch, a blocking HTTP request, runs once at
 * INIT on its own thread. With no Limelight in the robot configuration it reports "no camera" and
 * sees nothing, so an OpMode using it still runs.
 */
public final class Vision implements Subsystem, Balls {

    /** Which Limelight pipeline an OpMode runs. Chosen at INIT: a switch takes too long mid-match. */
    public enum Job {
        /** Hive-Vision's ball detector. */
        BALLS,
        /** AprilTags. Only selects the pipeline: nothing reads tags yet. */
        TAGS
    }

    private static final String TAG = "Vision";

    private final Limelight3A limelight;
    private final Supplier<Pose> pose;
    private final Alliance alliance;
    private final Job job;
    private final int pipeline;
    private final PoseHistory history = new PoseHistory(600, 256);
    private final BallTracker.Settings trackerSettings = new BallTracker.Settings();
    private final BallTracker tracker = new BallTracker(trackerSettings);
    private volatile String switchProblem;

    private Frame latest;
    private double latencyMs;
    private double lastFrameId = Double.NaN;
    private double fpsWindowStart = Double.NaN;
    private int fpsFrames;
    private double fps;
    /** The latest vision drive built, and whether its result has been logged. */
    private DriveToBall lastDrive;
    private boolean lastDriveLogged;
    private String loggedCandidates;

    /** {@code pose} is where the robot is now, alliance frame: the Follower's {@code pose()}. */
    public Vision(RobotHardware robot, Supplier<Pose> pose, Alliance alliance, Job job) {
        this.limelight = robot.limelight();
        this.pose = pose;
        this.alliance = alliance;
        this.job = job;
        this.pipeline = job == Job.BALLS ? VisionTuning.ballPipeline : VisionTuning.tagPipeline;
        if (limelight == null) {
            return;
        }
        // The Limelight lives in the hardware map from one OpMode to the next; the poll rate only
        // changes while it is stopped.
        limelight.stop();
        limelight.setPollRateHz(VisionTuning.pollRateHz);
        limelight.start();
        final Limelight3A camera = limelight;
        final int index = pipeline;
        Thread switcher = new Thread(() -> {
            // An HTTP request that can block for seconds if the camera stalls: never on the OpMode
            // thread, which INITs TeleOp inside the 8 s AUTO→TELEOP transition.
            try {
                if (!camera.pipelineSwitch(index)) {
                    switchProblem = "couldn't switch to pipeline " + index;
                }
            } catch (RuntimeException e) {
                switchProblem = "couldn't switch to pipeline " + index + ": " + e.getMessage();
            }
            if (switchProblem != null) {
                RobotLog.ww(TAG, "%s", switchProblem);
            }
        }, "vision-pipeline");
        switcher.setDaemon(true);
        switcher.start();
    }

    @Override
    public void periodic() {
        logDriveResult();
        double now = nowMs();
        Pose p = pose.get();
        if (p != null) {
            history.add(now, p);
        }
        if (limelight == null || job != Job.BALLS) {
            return;
        }
        LLResult r = limelight.getLatestResult();
        if (r == null || r.getPipelineIndex() != pipeline || r.getTimestamp() == lastFrameId) {
            return;
        }
        lastFrameId = r.getTimestamp();
        countFrame(now);
        latencyMs = r.getStaleness() + r.getCaptureLatency() + r.getTargetingLatency() + VisionTuning.extraMs;
        double capture = now - latencyMs;

        List<Detection> detections = new ArrayList<>();
        List<LLResultTypes.DetectorResult> found = r.isValid() ? r.getDetectorResults() : null;
        if (found != null) {
            for (LLResultTypes.DetectorResult d : found) {
                BallKind kind = BallKind.of(d.getClassName(), d.getClassId());
                if (kind == null) {
                    continue;
                }
                // The SDK documents 0–100, Hive-Vision found 0–1: accept either.
                double confidence = d.getConfidence() > 1 ? d.getConfidence() / 100 : d.getConfidence();
                detections.add(new Detection(kind, d.getTargetXDegreesNoCrosshair(), d.getTargetYDegreesNoCrosshair(),
                        confidence, d.getTargetArea()));
            }
        }
        latest = new Frame(lastFrameId, capture, detections);
        VisionTuning.apply(trackerSettings);
        tracker.update(latest, history.at(capture), history.turnRate(capture), VisionTuning.mount(), now);
    }

    @Override
    public void status(Telemetry telemetry) {
        if (limelight == null) {
            telemetry.addData("vision", "no camera (no \"%s\" in the robot configuration)", RobotHardware.LIMELIGHT);
            return;
        }
        if (switchProblem != null) {
            telemetry.addData("vision", "%s", switchProblem);
            return;
        }
        if (!limelight.isConnected()) {
            telemetry.addData("vision", "camera not answering");
            return;
        }
        LLResult r = limelight.getLatestResult();
        if (r == null || r.getPipelineIndex() != pipeline) {
            telemetry.addData("vision", "waiting for pipeline %d (%s)", pipeline, job);
            return;
        }
        if (job == Job.TAGS || latest == null) {
            telemetry.addData("vision", "%s pipeline %d ready", job, pipeline);
            return;
        }
        telemetry.addData("vision", "%.0f fps · %d ball(s), %d sure · %.0f ms behind",
                fps, tracker.balls().size(), tracker.confirmed().size(), latencyMs);
        telemetry.addData("vision frame", "%s", tracker.lastFrameSummary());
        if (lastDrive != null && lastDrive.candidates() != null) {
            telemetry.addData("vision pick", "%s", lastDrive.candidates());
        }
        if (lastDrive != null && lastDrive.outcome() != DriveToBall.Outcome.RUNNING) {
            telemetry.addData("vision drive", "%s: %s", lastDrive.outcome(), lastDrive.name());
        }
    }

    @Override
    public void stop() {
        if (limelight != null) {
            limelight.stop();
        }
    }

    // ---- what routes and assists use --------------------------------------------------------

    @Override
    public List<BallTracker.Ball> confirmed() {
        return tracker.confirmed();
    }

    @Override
    public BallTracker.Ball find(int id) {
        return tracker.find(id);
    }

    /**
     * A route's {@code "seek"} (register it as {@code r.seek("vision.ball", vision::toBall)}): drive
     * to a ball under AUTO's rules, args as in {@link ToBallArgs}. Bad args fail at INIT.
     */
    public Step toBall(JsonObject args) {
        return driveToBall(ToBallArgs.parse(args, autoSettings()));
    }

    /** A vision drive with these settings, e.g. for a TeleOp assist (see {@link #teleOpSettings}). */
    public Step driveToBall(DriveToBall.Settings settings) {
        DriveToBall.Settings s = settings.copy();
        DriveToBall drive = new DriveToBall(this, pose, stop -> Steps.driveTo("ball", stop, s.obstacles, s.margin), alliance, s);
        // A route builds its seeks at INIT, so this shows the latest one built (or run) last.
        lastDrive = drive;
        lastDriveLogged = false;
        return drive;
    }

    /** AUTO's settings: our half only, the HIVE outline avoided ({@link AutoClearance}). */
    public DriveToBall.Settings autoSettings() {
        DriveToBall.Settings s = VisionTuning.approach();
        s.auto = true;
        s.hiveMargin = AutoClearance.MARGIN;
        s.obstacles = AutoClearance.obstacles(alliance);
        s.margin = AutoClearance.MARGIN;
        return s;
    }

    /** TeleOp's settings: no own-half limit; the drive plans round {@code obstacles}, {@code margin} clear. */
    public DriveToBall.Settings teleOpSettings(List<KeepOut> obstacles, double margin) {
        DriveToBall.Settings s = VisionTuning.approach();
        s.auto = false;
        s.obstacles = obstacles;
        s.margin = margin;
        return s;
    }

    /**
     * A route's condition: a confirmed ball of these colours within reach that AUTO may go for,
     * e.g. {@code r.condition("vision.seesPollen", () -> vision.sees(Colors.POLLEN))}.
     */
    public boolean sees(PickupRules.Colors colors) {
        Pose here = pose.get();
        if (here == null) {
            return false;
        }
        for (BallTracker.Ball b : tracker.confirmed()) {
            if (colors.wants(b.kind, alliance)
                    && Math.hypot(b.x() - here.x(), b.y() - here.y()) <= VisionTuning.within
                    && PickupRules.whyNot(b.kind, b.x(), b.y(), alliance, true, AutoClearance.MARGIN) == null) {
                return true;
            }
        }
        return false;
    }

    // ---- for the vision bench ---------------------------------------------------------------

    /** The latest frame taken in, or null before the first. */
    public Frame latestFrame() {
        return latest;
    }

    /** How old the latest frame's image was when it was taken in, ms. */
    public double latencyMs() {
        return latencyMs;
    }

    /** New frames per second, as the robot receives them. */
    public double fps() {
        return fps;
    }

    public BallTracker tracker() {
        return tracker;
    }

    /** Forget every tracked ball. */
    public void forget() {
        tracker.clear();
    }

    /** A finished vision drive goes in the robot log: the route moves on at once, so the DS shows it only briefly. */
    private void logDriveResult() {
        if (lastDrive != null && lastDrive.candidates() != null && !lastDrive.candidates().equals(loggedCandidates)) {
            loggedCandidates = lastDrive.candidates();
            RobotLog.ii(TAG, "vision pick: %s", loggedCandidates);
        }
        if (lastDrive != null && !lastDriveLogged && lastDrive.outcome() != DriveToBall.Outcome.RUNNING) {
            RobotLog.ii(TAG, "vision drive %s: %s (frame: %s)", lastDrive.outcome(), lastDrive.name(), tracker.lastFrameSummary());
            lastDriveLogged = true;
        }
    }

    private void countFrame(double now) {
        if (Double.isNaN(fpsWindowStart)) {
            fpsWindowStart = now;
        }
        fpsFrames++;
        if (now - fpsWindowStart >= 1000) {
            fps = fpsFrames * 1000 / (now - fpsWindowStart);
            fpsFrames = 0;
            fpsWindowStart = now;
        }
    }

    private static double nowMs() {
        return System.nanoTime() / 1e6;
    }
}
