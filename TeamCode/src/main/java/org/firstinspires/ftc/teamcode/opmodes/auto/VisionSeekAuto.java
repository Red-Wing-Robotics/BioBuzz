package org.firstinspires.ftc.teamcode.opmodes.auto;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.firstinspires.ftc.teamcode.auto.JsonAutoOpMode;
import org.firstinspires.ftc.teamcode.auto.route.ActionRegistry;
import org.firstinspires.ftc.teamcode.subsystems.Vision;
import org.firstinspires.ftc.teamcode.vision.PickupRules;

/**
 * Tries the camera in AUTO: drive to our GARDEN, drive to a POLLEN the camera sees, then park.
 * There's no intake yet, so it stops short of the ball. A test, not a match auto; the route is
 * assets/autos/vision_seek.json. Its registrations are the pattern for any auto using vision.
 */
public abstract class VisionSeekAuto extends JsonAutoOpMode {

    private Vision vision;

    @Override
    protected String routeName() {
        return "vision_seek";
    }

    @Override
    protected void createSubsystems() {
        vision = add(new Vision(robot, follower::pose, alliance(), Vision.Job.BALLS));
    }

    @Override
    protected void registerActions(ActionRegistry r) {
        r.seek("vision.ball", vision::toBall);
        r.condition("vision.seesPollen", () -> vision.sees(PickupRules.Colors.POLLEN));
        r.condition("vision.seesOurs", () -> vision.sees(PickupRules.Colors.OURS));
    }

    @Autonomous(name = "Red: Vision Seek (test)", group = "test", preselectTeleOp = "Red: TeleOp")
    public static class Red extends VisionSeekAuto {
        @Override protected Alliance alliance() { return Alliance.RED; }
    }

    @Autonomous(name = "Blue: Vision Seek (test)", group = "test", preselectTeleOp = "Blue: TeleOp")
    public static class Blue extends VisionSeekAuto {
        @Override protected Alliance alliance() { return Alliance.BLUE; }
    }
}
