package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.Field;
import org.firstinspires.ftc.teamcode.auto.KeepOut;
import org.firstinspires.ftc.teamcode.auto.Step;
import org.firstinspires.ftc.teamcode.auto.Steps;
import org.firstinspires.ftc.teamcode.auto.Waypoint;
import org.firstinspires.ftc.teamcode.teleop.Assist;
import org.firstinspires.ftc.teamcode.teleop.Assists;
import org.firstinspires.ftc.teamcode.teleop.ControlScheme;
import org.firstinspires.ftc.teamcode.teleop.DriverCommands;
import org.firstinspires.ftc.teamcode.teleop.Resource;
import org.firstinspires.ftc.teamcode.teleop.TeleOpBase;
import org.firstinspires.ftc.teamcode.teleop.schemes.SoloScheme;
import org.firstinspires.ftc.teamcode.teleop.schemes.TwoDriverScheme;

import java.util.EnumSet;

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

    @Override
    protected Waypoint startWaypoint() {
        return START;
    }

    @Override
    protected ControlScheme scheme() {
        return new TwoDriverScheme();
    }

    @Override
    protected void registerAssists(Assists assists) {
        // ShootingSpots and HiveClearance are read at each press, so Panels edits apply on the next press.
        assists.define(Assist.RETURN_TO_START, EnumSet.of(Resource.DRIVE),
                () -> Steps.driveTo("start", startPose(), hive()));
        assists.define(Assist.SHOOT_LEFT, EnumSet.of(Resource.DRIVE),
                () -> driveToAndHold("left spot", ShootingSpots.left));
        assists.define(Assist.SHOOT_RIGHT, EnumSet.of(Resource.DRIVE),
                () -> driveToAndHold("right spot", ShootingSpots.right));
    }

    /** Drive to a spot, then hold it against bumps while the operator fires, until a drive stick moves. */
    private Step driveToAndHold(String name, Waypoint spot) {
        Pose target = alliance().pose(spot);
        return Steps.sequence(Steps.driveTo(name, target, hive()), Steps.hold(name, target));
    }

    /** Where the robot's centre must not go: the HIVE plus {@link HiveClearance}. */
    private static KeepOut hive() {
        return Field.HIVE.grow(HiveClearance.inches);
    }

    @Override
    protected void apply(DriverCommands c) {
        // Mechanism subsystems (intake, shooter, feeders) are not written yet. As each one lands,
        // create it in onInit() and drive it from here unless an assist owns it, e.g.
        //   if (!assisting(Resource.INTAKE)) intake.setPower(c.intake);
    }

    @TeleOp(name = "Red: TeleOp", group = "comp")
    public static class Red extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.RED; }
    }

    @TeleOp(name = "Blue: TeleOp", group = "comp")
    public static class Blue extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.BLUE; }
    }

    /** One person on gamepad1, for practice and testing. Plays as RED. */
    @TeleOp(name = "Solo: TeleOp", group = "test")
    public static class Solo extends CompTeleOp {
        @Override protected Alliance alliance() { return Alliance.RED; }
        @Override protected ControlScheme scheme() { return new SoloScheme(); }
    }
}
