package org.firstinspires.ftc.teamcode.teleop;

import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.RobotHardware;
import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.pedro.Constants;

/**
 * Base class for driver-controlled OpModes. A subclass picks an alliance and a
 * {@link ControlScheme}, creates subsystems in {@link #onInit}, and turns {@link DriverCommands}
 * into subsystem calls in {@link #apply}. This class owns the loop, the gamepads, robot-centric
 * driving through the Follower, the endgame cue, and telemetry.
 *
 * Every loop: clear the bulk cache → snapshot both gamepads → the scheme maps them to commands →
 * drive → {@link #apply}. Nothing moves during INIT (G403); the Driver Station shows the control
 * card instead. The Pinpoint IMU calibrates at INIT, so keep the robot still.
 */
public abstract class TeleOpBase extends OpMode {

    protected RobotHardware robot;
    protected Follower follower;

    private final DriverInput input = new DriverInput();
    private final DriverCommands commands = new DriverCommands();
    private final MatchClock clock = new MatchClock();
    private final ElapsedTime sinceStart = new ElapsedTime();
    private final ElapsedTime sinceLastLoop = new ElapsedTime();
    private ControlScheme scheme;

    protected abstract Alliance alliance();

    /** The gamepad layout. Called once per INIT, so a scheme's toggles start off. */
    protected abstract ControlScheme scheme();

    /** Optional: create subsystems from {@link #robot}. Must not move anything (G403). */
    protected void onInit() {}

    /** Act on this loop's mechanism commands. Driving is already done. Runs once per loop. */
    protected abstract void apply(DriverCommands commands);

    @Override
    public final void init() {
        robot = new RobotHardware(hardwareMap);
        follower = Constants.create(hardwareMap); // recalibrates the Pinpoint IMU: keep the robot still
        scheme = scheme();
        onInit();
    }

    @Override
    public final void init_loop() {
        // Read during INIT too, so a button held through START isn't seen as a fresh press.
        readGamepads();
        telemetry.addData("alliance", alliance());
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
        follower.manual(new DrivePowers(commands.forward, commands.strafe, commands.turn));
        follower.update(); // reads the localizer, then applies the manual powers
        apply(commands);

        if (clock.endgameStarted()) { // 1:00 left: NECTAR may now go into FLOWERS (G410)
            gamepad1.rumbleBlips(3);
            gamepad2.rumbleBlips(3);
        }

        Pose pose = follower.pose();
        telemetry.addData("time left", "%s%s", clock.remainingText(), clock.isEndgame() ? "  ENDGAME" : "");
        telemetry.addData("pose", "x %.1f  y %.1f  heading %.0f°",
                pose.x(), pose.y(), Math.toDegrees(pose.heading()));
        telemetry.addData("drive", "fwd %.2f  strafe %.2f  turn %.2f%s",
                commands.forward, commands.strafe, commands.turn, commands.slow ? "  SLOW" : "");
        telemetry.addData("intake", "%.2f", commands.intake);
        telemetry.addData("shooter", "%s%s", commands.shooterOn ? "on" : "off", commands.fire ? "  FIRE" : "");
        telemetry.addData("loop", "%.0f ms", loopMs);
    }

    @Override
    public final void stop() {
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
