package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.api.Paths;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.RobotHardware;
import org.firstinspires.ftc.teamcode.RobotId;
import org.firstinspires.ftc.teamcode.pedro.Constants;
import org.firstinspires.ftc.teamcode.subsystems.Subsystem;
import org.firstinspires.ftc.teamcode.subsystems.Subsystems;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for declarative autonomous OpModes. Subclasses declare an alliance, a start
 * waypoint, and a {@link Step} routine built from {@link Steps} factories and the path helpers
 * here; this class owns the Follower, the loop, and telemetry.
 *
 * The routine is built during INIT, so waypoints edited in Panels take effect on the next INIT.
 * Nothing moves during INIT (G403/G304); the Pinpoint IMU calibrates, so keep the robot still.
 *
 * Subsystems are created in {@link #createSubsystems} and registered with {@link #add}. Every
 * loop: clear the bulk cache → each subsystem's {@code periodic()} → the routine →
 * {@code follower.update()}. Their status shows on the Driver Station from INIT on, and they stop
 * when the OpMode does.
 */
public abstract class AutoOpMode extends OpMode {

    protected RobotHardware robot;
    protected Follower follower;

    private final ElapsedTime timer = new ElapsedTime();
    private final Subsystems subsystems = new Subsystems();
    private final Map<String, String> initLines = new LinkedHashMap<>();
    private AutoContext ctx;
    private Step routine;
    private boolean finished;

    protected abstract Alliance alliance();

    protected abstract Waypoint startWaypoint();

    /** Build the routine. Called once per INIT, after {@link #follower} exists. */
    protected abstract Step routine();

    /**
     * Optional: create subsystems from {@link #robot} and {@link #follower}, registering each with
     * {@link #add}. Runs first in INIT, so everything after can use them. Must not move anything (G403).
     */
    protected void createSubsystems() {}

    /** Optional: load data. Runs after {@link #createSubsystems}, before the start pose and routine are read. */
    protected void onInit() {}

    /** Register a subsystem: it runs every loop, shows its status, and stops with the OpMode. Returns it. */
    protected final <T extends Subsystem> T add(T subsystem) {
        return subsystems.add(subsystem);
    }

    /** A Driver Station line shown for the whole of INIT. An empty caption shows the value alone. */
    protected final void initLine(String caption, String value) {
        initLines.put(caption, value);
    }

    @Override
    public final void init() {
        RobotId robotId = RobotId.onHub(); // stops INIT, saying how to fix it, if the hub has none
        robot = new RobotHardware(hardwareMap);
        follower = Constants.create(hardwareMap, robotId);
        ctx = new AutoContext(robot, follower, timer::seconds);
        finished = false;
        createSubsystems();
        onInit();
        follower.setPose(pose(startWaypoint()));
        routine = routine();
        initLine("robot", Constants.describe(robotId));
        initLine("alliance", alliance().toString());
        initLine("start", startWaypoint().toString());
        initLine("", "Waypoints edited in Panels apply on re-INIT.");
        showInit();
    }

    @Override
    public final void init_loop() {
        showInit(); // telemetry clears every loop: the INIT lines go again, with the subsystems' status
    }

    @Override
    public final void start() {
        timer.reset();
        routine.start(ctx);
    }

    @Override
    public final void loop() {
        robot.clearBulkCache();
        subsystems.periodic();
        if (!finished) {
            finished = routine.update(ctx);
            if (finished) {
                routine.end(ctx, false);
            }
        }
        follower.update();

        Pose pose = follower.pose();
        telemetry.addData("step", finished ? "done" : routine.name());
        telemetry.addData("time", "%.1f s", timer.seconds());
        telemetry.addData("pose", "x %.1f  y %.1f  heading %.0f°",
                pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        subsystems.status(telemetry);
    }

    @Override
    public final void stop() {
        if (routine != null && !finished) {
            routine.end(ctx, true);
        }
        subsystems.stop(); // each one guarded: the pose below is always saved
        if (follower != null) {
            PoseHandoff.save(follower.pose()); // a TeleOp INIT'd soon after starts from here
            follower.stop();
        }
    }

    private void showInit() {
        for (Map.Entry<String, String> line : initLines.entrySet()) {
            if (line.getKey().isEmpty()) {
                telemetry.addLine(line.getValue());
            } else {
                telemetry.addData(line.getKey(), line.getValue());
            }
        }
        subsystems.status(telemetry);
    }

    // ---- path helpers, all in this OpMode's alliance frame ----------------------------

    protected final Pose pose(Waypoint waypoint) {
        return alliance().pose(waypoint);
    }

    /** Straight line; heading interpolates from {@code from}'s heading to {@code to}'s. */
    protected final Path line(Waypoint from, Waypoint to) {
        Pose a = pose(from), b = pose(to);
        return Paths.line(a, b).linear(a, b);
    }

    /**
     * Bezier curve using the waypoints as control points (the middle ones pull the curve; the
     * robot passes only through the first and last). Heading interpolates first → last.
     */
    protected final Path curve(Waypoint... controlPoints) {
        Pose[] poses = poses(controlPoints);
        return Paths.curve(poses).linear(poses[0], poses[poses.length - 1]);
    }

    /** Smooth curve that passes through every waypoint. Heading interpolates first → last. */
    protected final Path through(Waypoint... waypoints) {
        Pose[] poses = poses(waypoints);
        return Paths.through(poses).linear(poses[0], poses[poses.length - 1]);
    }

    private Pose[] poses(Waypoint[] waypoints) {
        Pose[] poses = new Pose[waypoints.length];
        for (int i = 0; i < waypoints.length; i++) {
            poses[i] = pose(waypoints[i]);
        }
        return poses;
    }
}
