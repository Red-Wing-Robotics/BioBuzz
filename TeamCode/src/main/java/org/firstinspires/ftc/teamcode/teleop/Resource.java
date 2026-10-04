package org.firstinspires.ftc.teamcode.teleop;

/**
 * What an assist takes over while it runs. Manual control of a resource pauses while an assist
 * owns it, and two assists that share a resource can't run at once (the newer one wins).
 */
public enum Resource {
    DRIVE, INTAKE, SHOOTER, FEEDER, DIVERTER
}
