package org.firstinspires.ftc.teamcode.subsystems;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

import org.firstinspires.ftc.teamcode.RobotHardware;

/**
 * The four continuous rotation feeder servos, {@link RobotHardware#TOP_FEEDER},
 * {@link RobotHardware#UP_MID_FEEDER}, {@link RobotHardware#DOWN_MID_FEEDER} and
 * {@link RobotHardware#BOTTOM_FEEDER}, each switched on (at {@link #FEED_POWER}) or off
 * independently.
 */
public class Feeders {

    /** Power a feeder runs at when on, −1..1. */
    public static final double FEED_POWER = 1.0;

    // TODO verify on the robot (docs/hardware.md): each feeder must move the element toward the shooter.
    private static final DcMotorSimple.Direction TOP_DIRECTION = DcMotorSimple.Direction.FORWARD;
    private static final DcMotorSimple.Direction UP_MID_DIRECTION = DcMotorSimple.Direction.FORWARD;
    private static final DcMotorSimple.Direction DOWN_MID_DIRECTION = DcMotorSimple.Direction.FORWARD;
    private static final DcMotorSimple.Direction BOTTOM_DIRECTION = DcMotorSimple.Direction.FORWARD;

    private final Feeder top;
    private final Feeder upMid;
    private final Feeder downMid;
    private final Feeder bottom;

    /** Configures the servos; moves nothing (G403). */
    public Feeders(RobotHardware robot) {
        top = new Feeder(robot, RobotHardware.TOP_FEEDER, TOP_DIRECTION);
        upMid = new Feeder(robot, RobotHardware.UP_MID_FEEDER, UP_MID_DIRECTION);
        downMid = new Feeder(robot, RobotHardware.DOWN_MID_FEEDER, DOWN_MID_DIRECTION);
        bottom = new Feeder(robot, RobotHardware.BOTTOM_FEEDER, BOTTOM_DIRECTION);
    }

    /** Spin each feeder that is true and stop the rest. */
    public void set(boolean topOn, boolean upMidOn, boolean downMidOn, boolean bottomOn) {
        top.setOn(topOn);
        upMid.setOn(upMidOn);
        downMid.setOn(downMidOn);
        bottom.setOn(bottomOn);
    }

    /** One servo; writes only when the command changes, to keep the loop short. */
    private static final class Feeder {
        private final CRServo servo;
        private Boolean commanded;

        Feeder(RobotHardware robot, String name, DcMotorSimple.Direction direction) {
            servo = robot.getHardwareMap().get(CRServo.class, name);
            servo.setDirection(direction);
        }

        void setOn(boolean on) {
            if (commanded != null && commanded == on) {
                return;
            }
            commanded = on;
            servo.setPower(on ? FEED_POWER : 0);
        }
    }
}
