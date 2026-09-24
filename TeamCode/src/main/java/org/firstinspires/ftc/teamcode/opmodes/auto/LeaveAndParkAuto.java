package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.JsonAutoOpMode;

/**
 * Minimum-value auto: leave the wall (3 pts) and park in the LOADING ZONE (5 pts).
 * The routine and waypoints are in assets/autos/leave_and_park.json; this class only names the
 * route and registers one OpMode per alliance.
 */
public abstract class LeaveAndParkAuto extends JsonAutoOpMode {

    @Override
    protected String routeName() {
        return "leave_and_park";
    }

    @Autonomous(name = "Red: Leave + Park", group = "comp", preselectTeleOp = "Drive TeleOp")
    public static class Red extends LeaveAndParkAuto {
        @Override protected Alliance alliance() { return Alliance.RED; }
    }

    @Autonomous(name = "Blue: Leave + Park", group = "comp", preselectTeleOp = "Drive TeleOp")
    public static class Blue extends LeaveAndParkAuto {
        @Override protected Alliance alliance() { return Alliance.BLUE; }
    }
}
