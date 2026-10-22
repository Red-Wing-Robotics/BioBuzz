package org.firstinspires.ftc.teamcode.teleop;

/** How a shot the operator logs ({@link DriverCommands#logShot}) turned out. */
public enum ShotResult {
    HIT("hit"),
    MISS("miss");

    /** As written in the shot log's result column. */
    public final String label;

    ShotResult(String label) {
        this.label = label;
    }
}
