package org.firstinspires.ftc.teamcode.auto;

/**
 * The robot's footprint, inches: everything, including whatever sticks out, around the point
 * Pedro's pose tracks (taken to be the middle). Driving assists keep it clear of the HIVE
 * ({@link DrivePlan}) and the auto maps draw it. Pure Java: tools/routeview compiles it too.
 *
 * PLACEHOLDERS: 18 × 18 is the R102 starting-size maximum. Measure the real robot.
 */
public final class RobotSize {

    /** Front (where the launcher fires) to back. */
    public static final double LENGTH = 18;

    /** Side to side. */
    public static final double WIDTH = 18;

    /** Turned any way, no part of the robot is further than this from its centre. */
    public static final double REACH = Math.hypot(LENGTH, WIDTH) / 2;

    private RobotSize() {}
}
