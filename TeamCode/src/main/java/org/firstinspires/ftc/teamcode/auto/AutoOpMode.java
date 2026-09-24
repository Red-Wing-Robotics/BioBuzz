package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.api.Paths;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.RobotHardware;
import org.firstinspires.ftc.teamcode.pedro.Constants;

/**
 * Base class for declarative autonomous OpModes. Subclasses declare an alliance, a start
 * waypoint, and a {@link Step} routine built from {@link Steps} factories and the path helpers
 * here; this class owns the Follower, the loop, and telemetry.
 *
 * The routine is built during INIT, so waypoints edited in Panels take effect on the next INIT.
 * Nothing moves during INIT (G403/G304); the Pinpoint IMU calibrates, so keep the robot still.
 */
public abstract class AutoOpMode extends OpMode {

    protected RobotHardware robot;
    protected Follower follower;

    private final ElapsedTime timer = new ElapsedTime();
    private AutoContext ctx;
    private Step routine;
    private boolean finished;

    protected abstract Alliance alliance();

    protected abstract Waypoint startWaypoint();

    /** Build the routine. Called once per INIT, after {@link #follower} exists. */
    protected abstract Step routine();

    /** Optional: load data, create subsystems. Runs after the Follower exists, before the start pose and routine are read. */
    protected void onInit() {}

    @Override
    public final void init() {
        robot = new RobotHardware(hardwareMap);
        follower = Constants.create(hardwareMap);
        ctx = new AutoContext(robot, follower, timer::seconds);
        finished = false;
        onInit();
        follower.setPose(pose(startWaypoint()));
        routine = routine();
        telemetry.addData("alliance", alliance());
        telemetry.addData("start", startWaypoint());
        telemetry.addLine("Waypoints edited in Panels apply on re-INIT.");
    }

    @Override
    public final void start() {
        timer.reset();
        routine.start(ctx);
    }

    @Override
    public final void loop() {
        robot.clearBulkCache();
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
    }

    @Override
    public final void stop() {
        if (routine != null && !finished) {
            routine.end(ctx, true);
        }
        if (follower != null) {
            follower.stop();
        }
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
