package org.firstinspires.ftc.teamcode.subsystems;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * A mechanism or sensor an OpMode registers with {@code add(...)} (AutoOpMode, TeleOpBase), which
 * then runs it: {@link #periodic} every loop, {@link #status} on the Driver Station from INIT on,
 * and {@link #stop} when the OpMode ends. Nothing here may block or sleep, and nothing may move
 * during INIT (G403): only {@link #status} runs then.
 */
public interface Subsystem {

    /** Once per loop after START, after the bulk cache is cleared and before the routine or assists run. */
    default void periodic() {}

    /** Driver Station lines, every loop including INIT. May read sensors; must not move anything. */
    default void status(Telemetry telemetry) {}

    /** When the OpMode stops. */
    default void stop() {}
}
