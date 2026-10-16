package org.firstinspires.ftc.teamcode.teleop;

/**
 * What a scheme can ask the robot to keep its front pointed at while the driver drives
 * ({@link DriverCommands#aim}). Where each one is on the field is up to the TeleOp
 * ({@code TeleOpBase.aimPoint}); {@link HeadingAim} turns the robot.
 */
public enum AimTarget {
    /** Our HIVE's CELL on the drivers' left (the far-wall end for RED). */
    LEFT_CELL("left CELL"),
    /** Our HIVE's CELL on the drivers' right (the audience end for RED; up at the start of the match). */
    RIGHT_CELL("right CELL");

    public final String label;

    AimTarget(String label) {
        this.label = label;
    }
}
