package org.firstinspires.ftc.teamcode.teleop;

/**
 * Names of the assists a scheme can request with {@code out.press(...)} / {@code out.hold(...)}.
 * What each one does is defined per TeleOp in {@code registerAssists}. A request for a name the
 * running TeleOp doesn't define shows a notice on the Driver Station and does nothing.
 */
public final class Assist {

    /** Drive back to the TeleOp's start waypoint. */
    public static final String RETURN_TO_START = "return to start";

    /** Drive to the shooting spot on the drivers' left of the HIVE and hold it. */
    public static final String SHOOT_LEFT = "left shooting spot";

    /** Drive to the shooting spot on the drivers' right of the HIVE and hold it. */
    public static final String SHOOT_RIGHT = "right shooting spot";

    /** Drive to a ball the camera sees and stop short of it (vision/DriveToBall). */
    public static final String TO_BALL = "drive to a ball";

    /** Forget every ball the camera has tracked (the vision bench). */
    public static final String FORGET_BALLS = "forget balls";

    private Assist() {}
}
