package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.Field;
import org.firstinspires.ftc.teamcode.auto.Step;
import org.firstinspires.ftc.teamcode.auto.Steps;
import org.firstinspires.ftc.teamcode.auto.Waypoint;
import org.firstinspires.ftc.teamcode.subsystems.Shooter;
import org.firstinspires.ftc.teamcode.teleop.AimTarget;
import org.firstinspires.ftc.teamcode.teleop.Assist;
import org.firstinspires.ftc.teamcode.teleop.Assists;
import org.firstinspires.ftc.teamcode.teleop.ControlScheme;
import org.firstinspires.ftc.teamcode.teleop.DriverCommands;
import org.firstinspires.ftc.teamcode.teleop.Resource;
import org.firstinspires.ftc.teamcode.teleop.ShotResult;
import org.firstinspires.ftc.teamcode.teleop.TeleOpBase;
import org.firstinspires.ftc.teamcode.teleop.schemes.AimScheme;
import org.firstinspires.ftc.teamcode.teleop.schemes.AimShooterScheme;
import org.firstinspires.ftc.teamcode.teleop.schemes.SoloScheme;
import org.firstinspires.ftc.teamcode.teleop.schemes.TwoDriverScheme;
import org.firstinspires.ftc.teamcode.util.CsvLog;
import org.firstinspires.ftc.teamcode.util.ShotRecord;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Locale;

/**
 * The competition TeleOp. Button assignments live in the schemes (teleop/schemes/), not here:
 * this class sets the start position, defines the assists, wires {@link DriverCommands} to
 * subsystems, and registers one OpMode per variant. The autos preselect the Red/Blue variant of
 * their alliance.
 */
public abstract class CompTeleOp extends TeleOpBase {

    /**
     * Where the robot starts for practice when no AUTO ran just before (after one, TeleOp starts
     * where it ended). TeleOp's own spot, not any auto's START: there are many autos. Against the
     * red alliance wall, facing the blue one. RED frame (docs/coordinates.md); listed in
     * docs/field.md. A variant can override {@link #startWaypoint()} to start elsewhere.
     */
    private static final Waypoint START = new Waypoint(8, 60, 0);

    /**
     * The shot log (A / X in the Aim Shooter TeleOps): one row per logged shot, appended across
     * sessions. Copy it off the hub with {@code adb pull /sdcard/FIRST/data/shots.csv}.
     */
    private static final CsvLog SHOT_LOG =
            new CsvLog(new File(new File(AppUtil.FIRST_FOLDER, "data"), "shots.csv"), ShotRecord.HEADER);

    private Shooter shooter;
    private int shotsLogged;
    private String shotLogStatus;

    @Override
    protected Waypoint startWaypoint() {
        return START;
    }

    @Override
    protected ControlScheme scheme() {
        return new TwoDriverScheme();
    }

    @Override
    protected void onInit() {
        shooter = new Shooter(robot);
    }

    @Override
    protected void registerAssists(Assists assists) {
        // ShootingSpots and HiveClearance are read at each press, so Panels edits apply on the next press.
        assists.define(Assist.RETURN_TO_START, EnumSet.of(Resource.DRIVE),
                () -> Steps.driveTo("start", startPose(), Field.HIVE_FLOOR, HiveClearance.margin));
        assists.define(Assist.SHOOT_LEFT, EnumSet.of(Resource.DRIVE),
                () -> driveToAndHold("left spot", ShootingSpots.left));
        assists.define(Assist.SHOOT_RIGHT, EnumSet.of(Resource.DRIVE),
                () -> driveToAndHold("right spot", ShootingSpots.right));
    }

    /** Our HIVE's CELL centres ({@link HiveCells}, read every loop so Panels edits apply at once). */
    @Override
    protected Pose aimPoint(AimTarget target) {
        return alliance().pose(target == AimTarget.LEFT_CELL ? HiveCells.left : HiveCells.right);
    }

    /**
     * Drive to a spot the quickest way, round the HIVE or under it, then hold it against bumps
     * while the operator fires, until a drive stick moves.
     */
    private Step driveToAndHold(String name, Waypoint spot) {
        Pose target = alliance().pose(spot);
        return Steps.sequence(Steps.driveTo(name, target, Field.HIVE_FLOOR, HiveClearance.margin), Steps.hold(name, target));
    }

    @Override
    protected void apply(DriverCommands c) {
        if (!assisting(Resource.SHOOTER)) {
            shooter.changeVelocity(c.shooterVelocityChange);
            shooter.setOn(c.shooterOn);
        }
        telemetry.addData("shooter velocity", "target %.0f  top %.0f  bottom %.0f ticks/s",
                shooter.targetVelocity(), shooter.topVelocity(), shooter.bottomVelocity());
        if (c.logShot != null) {
            logShot(c);
        }
        if (shotLogStatus != null) {
            telemetry.addData("shot log", shotLogStatus);
        }
        // Intake and feeder subsystems are not written yet. As each one lands, create it in
        // onInit() and drive it from here unless an assist owns it, e.g.
        //   if (!assisting(Resource.INTAKE)) intake.setPower(c.intake);
    }

    /**
     * Append this moment to the shot log: pose, distance to the aimed-at CELL (else the nearer
     * one), flywheel speeds and the operator's verdict. One short file write, only on a press.
     */
    private void logShot(DriverCommands c) {
        Pose pose = follower.pose();
        AimTarget cell = c.aim;
        if (cell == null) {
            cell = distanceTo(AimTarget.LEFT_CELL, pose) <= distanceTo(AimTarget.RIGHT_CELL, pose)
                    ? AimTarget.LEFT_CELL : AimTarget.RIGHT_CELL;
        }
        ShotRecord r = new ShotRecord();
        r.time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        r.matchTimeLeftSeconds = matchTimeLeft();
        r.robot = robotId().label;
        r.alliance = alliance().toString();
        r.cell = cell.label;
        r.aiming = aimEngaged();
        r.x = pose.x();
        r.y = pose.y();
        r.headingDegrees = Math.toDegrees(pose.heading());
        r.distance = distanceTo(cell, pose);
        r.targetVelocity = shooter.targetVelocity();
        r.topVelocity = shooter.topVelocity();
        r.bottomVelocity = shooter.bottomVelocity();
        r.aimErrorDegrees = aimErrorDegrees();
        r.result = c.logShot.label;
        try {
            SHOT_LOG.append(r.csvRow());
            shotsLogged++;
            shotLogStatus = String.format(Locale.US, "#%d %s logged: %.1f in, %.0f ticks/s",
                    shotsLogged, c.logShot == ShotResult.HIT ? "HIT" : "MISS", r.distance, r.targetVelocity);
            gamepad2.rumbleBlips(1);
        } catch (IOException e) {
            shotLogStatus = "FAILED to write " + SHOT_LOG.file() + ": " + e.getMessage();
            gamepad2.rumble(1000);
        }
    }

    private double distanceTo(AimTarget cell, Pose pose) {
        Pose at = aimPoint(cell);
        return ShotRecord.distance(pose.x(), pose.y(), at.x(), at.y());
    }

    @TeleOp(name = "Red: TeleOp", group = "comp")
    public static class Red extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.RED; }
    }

    @TeleOp(name = "Blue: TeleOp", group = "comp")
    public static class Blue extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.BLUE; }
    }

    /** The match layout plus heading aim at our HIVE's CELLs on the driver's X / B ({@link AimScheme}). */
    @TeleOp(name = "Red: Aim TeleOp", group = "comp")
    public static class RedAim extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.RED; }
        @Override protected ControlScheme scheme() { return new AimScheme(); }
    }

    @TeleOp(name = "Blue: Aim TeleOp", group = "comp")
    public static class BlueAim extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.BLUE; }
        @Override protected ControlScheme scheme() { return new AimScheme(); }
    }

    /**
     * Aim TeleOp plus the operator's flywheel speed controls ({@link AimShooterScheme}): gp2 right
     * bumper toggles both shooters, d-pad up / down changes their speed.
     */
    @TeleOp(name = "Red: Aim Shooter TeleOp", group = "comp")
    public static class RedAimShooter extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.RED; }
        @Override protected ControlScheme scheme() { return new AimShooterScheme(); }
    }

    @TeleOp(name = "Blue: Aim Shooter TeleOp", group = "comp")
    public static class BlueAimShooter extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.BLUE; }
        @Override protected ControlScheme scheme() { return new AimShooterScheme(); }
    }

    /** One person on gamepad1, for practice and testing. Plays as RED. */
    @TeleOp(name = "Solo: TeleOp", group = "test")
    public static class Solo extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.RED; }
        @Override protected ControlScheme scheme() { return new SoloScheme(); }
    }
}
