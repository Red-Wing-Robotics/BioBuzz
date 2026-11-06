package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.JsonAutoOpMode;

/**
 * The routine and waypoints are in assets/autos/my_route.json; this class only names the route
 * and registers one OpMode per alliance.
 */
public abstract class MyRouteAuto extends JsonAutoOpMode {

    @Override
    protected String routeName() {
        return "my_route";
    }

    @Autonomous(name = "Red: My Route", group = "comp", preselectTeleOp = "Red: TeleOp")
    public static class Red extends MyRouteAuto {
        @Override protected Alliance alliance() { return Alliance.RED; }
    }

    @Autonomous(name = "Blue: My Route", group = "comp", preselectTeleOp = "Blue: TeleOp")
    public static class Blue extends MyRouteAuto {
        @Override protected Alliance alliance() { return Alliance.BLUE; }
    }
}
