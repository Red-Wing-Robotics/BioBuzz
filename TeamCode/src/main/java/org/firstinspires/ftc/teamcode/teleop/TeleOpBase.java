package org.firstinspires.ftc.teamcode.teleop;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.RobotHardware;
import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.AutoContext;
import org.firstinspires.ftc.teamcode.auto.PoseHandoff;
import org.firstinspires.ftc.teamcode.auto.Waypoint;
import org.firstinspires.ftc.teamcode.pedro.Constants;

import java.util.List;
import java.util.Locale;

/**
 * Base class for driver-controlled OpModes. A subclass picks an alliance, a {@link ControlScheme}
 * and a start waypoint, creates subsystems in {@link #onInit}, defines its {@link Assists} in
 * {@link #registerAssists}, and turns {@link DriverCommands} into subsystem calls in
 * {@link #apply}. This class owns the loop, the gamepads, driving through the Follower, the
 * assists, the endgame cue, and telemetry.
 *
 * Every loop: clear the bulk cache → snapshot both gamepads → the scheme maps them to commands →
 * start/stop requested assists → drive sticks cancel driving assists → run assists → drive
 * manually unless an assist owns DRIVE (the heading aim, if engaged, supplies the turn) →
 * {@link #apply}.
 *
 * Start pose: where AUTO ended if it stopped within the last {@link PoseHandoff#MAX_AGE_SECONDS},
 * otherwise {@link #startWaypoint()}. Nothing moves during INIT (G403); the Driver Station shows
 * the start pose and the control card instead. The Pinpoint IMU calibrates at INIT, so keep the
 * robot still.
 */
public abstract class TeleOpBase extends OpMode {

    protected RobotHardware robot;
    protected Follower follower;

    private final DriverInput input = new DriverInput();
    private final DriverCommands commands = new DriverCommands();
    private final Assists assists = new Assists();
    private final MatchClock clock = new MatchClock();
    private final ElapsedTime sinceStart = new ElapsedTime();
    private final ElapsedTime sinceLastLoop = new ElapsedTime();
    private final HeadingAim headingAim = new HeadingAim();
    private ControlScheme scheme;
    private AutoContext ctx;
    private Pose initialPose;
    private String initialPoseSource;

    protected abstract Alliance alliance();

    /** The gamepad layout. Called once per INIT, so a scheme's toggles start off. */
    protected abstract ControlScheme scheme();

    /**
     * Where the robot starts when no AUTO ran just before this TeleOp: RED frame, mapped for BLUE
     * like auto waypoints (docs/coordinates.md). For practice, put the robot here before INIT.
     */
    protected abstract Waypoint startWaypoint();

    /** Start from the pose a just-finished AUTO handed over. Override to return false to always use {@link #startWaypoint()}. */
    protected boolean usePoseFromAuto() {
        return true;
    }

    /** Optional: create subsystems from {@link #robot}. Must not move anything (G403). */
    protected void onInit() {}

    /** Optional: define the assists this TeleOp's schemes may request. Runs after {@link #onInit}. */
    protected void registerAssists(Assists assists) {}

    /**
     * Act on this loop's mechanism commands. Skip any resource an assist owns
     * ({@link #assisting}). Driving is already done. Runs once per loop.
     */
    protected abstract void apply(DriverCommands commands);

    /**
     * Where {@code target} is, alliance frame (heading unused), for the heading aim; null if this
     * TeleOp doesn't support it. Called every loop the aim is engaged.
     */
    protected Pose aimPoint(AimTarget target) {
        return null;
    }

    /** {@link #startWaypoint()} in this OpMode's alliance frame, e.g. as an assist target. */
    protected final Pose startPose() {
        return alliance().pose(startWaypoint());
    }

    /** True while an assist owns {@code resource}; {@link #apply} should leave it alone. */
    protected final boolean assisting(Resource resource) {
        return assists.owns(resource);
    }

    @Override
    public final void init() {
        robot = new RobotHardware(hardwareMap);
        follower = Constants.create(hardwareMap); // recalibrates the Pinpoint IMU: keep the robot still
        ctx = new AutoContext(robot, follower, sinceStart::seconds);
        scheme = scheme();
        onInit();
        registerAssists(assists);

        PoseHandoff.Saved fromAuto = usePoseFromAuto() ? PoseHandoff.recent() : null;
        if (fromAuto != null) {
            initialPose = fromAuto.pose;
            initialPoseSource = String.format(Locale.US, "where AUTO ended, %.0f s ago", fromAuto.ageSeconds);
        } else {
            initialPose = startPose();
            initialPoseSource = "start waypoint";
        }
        follower.setPose(initialPose);
    }

    @Override
    public final void init_loop() {
        // Read during INIT too, so a button held through START isn't seen as a fresh press.
        readGamepads();
        telemetry.addData("alliance", alliance());
        telemetry.addData("start", "%s: x %.1f  y %.1f  heading %.0f°", initialPoseSource,
                initialPose.x(), initialPose.y(), Math.toDegrees(initialPose.heading()));
        telemetry.addData("controls", scheme.name());
        for (Binding binding : scheme.bindings()) {
            telemetry.addData(binding.input(), binding.action);
        }
    }

    @Override
    public final void start() {
        sinceStart.reset();
        sinceLastLoop.reset();
    }

    @Override
    public final void loop() {
        double loopMs = sinceLastLoop.milliseconds();
        sinceLastLoop.reset();
        robot.clearBulkCache();
        readGamepads();
        clock.update(sinceStart.seconds());

        commands.clear();
        scheme.map(input, commands);
        assists.request(commands, ctx, clock.isEndgame());
        if (commands.forward != 0 || commands.strafe != 0 || commands.turn != 0) {
            assists.cancel(Resource.DRIVE, ctx); // the driver always wins
        }
        assists.update(ctx);
        Pose aimAt = commands.aim != null && !assists.owns(Resource.DRIVE) ? aimPoint(commands.aim) : null;
        if (aimAt == null) {
            headingAim.reset();
        }
        if (!assists.owns(Resource.DRIVE)) {
            double turn = aimAt != null
                    ? headingAim.turn(follower.pose(), aimAt.x(), aimAt.y(), loopMs / 1000.0)
                    : commands.turn;
            follower.manual(new DrivePowers(commands.forward, commands.strafe, turn));
        }
        follower.update(); // reads the localizer, then applies manual powers or the assist's path
        apply(commands);

        if (clock.endgameStarted()) { // 1:00 left: NECTAR may now go into FLOWERS (G410)
            gamepad1.rumbleBlips(3);
            gamepad2.rumbleBlips(3);
        }

        Pose pose = follower.pose();
        telemetry.addData("time left", "%s%s", clock.remainingText(), clock.isEndgame() ? "  ENDGAME" : "");
        telemetry.addData("pose", "x %.1f  y %.1f  heading %.0f°",
                pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        List<String> active = assists.describe();
        telemetry.addData("assists", active.isEmpty() ? "none" : String.join(", ", active));
        String notice = assists.notice(ctx.time());
        if (notice != null) {
            telemetry.addData("assist", notice);
        }
        if (commands.aim != null) {
            telemetry.addData("aim", aimAt != null
                    ? String.format(Locale.US, "%s, off by %.0f°", commands.aim.label, Math.toDegrees(headingAim.error()))
                    : commands.aim.label + (assists.owns(Resource.DRIVE) ? " (paused: assist driving)" : " (not available)"));
        }
        telemetry.addData("drive", "fwd %.2f  strafe %.2f  turn %.2f%s",
                commands.forward, commands.strafe, commands.turn, commands.slow ? "  SLOW" : "");
        telemetry.addData("intake", "%.2f", commands.intake);
        telemetry.addData("shooter", "%s%s", commands.shooterOn ? "on" : "off", commands.fire ? "  FIRE" : "");
        telemetry.addData("loop", "%.0f ms", loopMs);
    }

    @Override
    public final void stop() {
        if (ctx != null) {
            assists.cancelAll(ctx);
        }
        if (follower != null) {
            follower.stop();
        }
    }

    private void readGamepads() {
        copy(gamepad1, input.gamepad1);
        copy(gamepad2, input.gamepad2);
    }

    /** SDK gamepad → snapshot. Stick Y is negated: the SDK reports pushing up as negative. */
    private static void copy(Gamepad from, GamepadState to) {
        to.nextLoop();
        to.set(Axis.LEFT_STICK_X, from.left_stick_x)
                .set(Axis.LEFT_STICK_Y, -from.left_stick_y)
                .set(Axis.RIGHT_STICK_X, from.right_stick_x)
                .set(Axis.RIGHT_STICK_Y, -from.right_stick_y)
                .set(Axis.LEFT_TRIGGER, from.left_trigger)
                .set(Axis.RIGHT_TRIGGER, from.right_trigger)
                .set(Button.A, from.a)
                .set(Button.B, from.b)
                .set(Button.X, from.x)
                .set(Button.Y, from.y)
                .set(Button.DPAD_UP, from.dpad_up)
                .set(Button.DPAD_DOWN, from.dpad_down)
                .set(Button.DPAD_LEFT, from.dpad_left)
                .set(Button.DPAD_RIGHT, from.dpad_right)
                .set(Button.LEFT_BUMPER, from.left_bumper)
                .set(Button.RIGHT_BUMPER, from.right_bumper)
                .set(Button.LEFT_STICK_BUTTON, from.left_stick_button)
                .set(Button.RIGHT_STICK_BUTTON, from.right_stick_button)
                .set(Button.BACK, from.back);
    }
}
