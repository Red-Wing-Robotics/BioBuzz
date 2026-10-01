package org.firstinspires.ftc.teamcode.teleop;

/** Which gamepad a {@link Binding} reads. By convention gamepad1 drives and gamepad2 runs mechanisms. */
public enum Pad {
    GAMEPAD1("gp1"),
    GAMEPAD2("gp2");

    /** Short name for the control card. */
    public final String label;

    Pad(String label) {
        this.label = label;
    }
}
