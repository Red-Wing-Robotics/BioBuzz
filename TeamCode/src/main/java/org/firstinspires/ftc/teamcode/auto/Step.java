package org.firstinspires.ftc.teamcode.auto;

/**
 * One unit of an autonomous routine, driven non-blockingly by {@link AutoOpMode}: {@link #start}
 * once, {@link #update} every loop until it returns true, then {@link #end}. Compose steps with
 * the factories in {@link Steps}. Never sleep or loop inside a step.
 */
public interface Step {

    /** Called once when the step becomes active. */
    default void start(AutoContext ctx) {}

    /** Called every loop while active. Return {@code true} once the step is finished. */
    boolean update(AutoContext ctx);

    /** Called once when the step finishes ({@code interrupted == false}) or is cut short. */
    default void end(AutoContext ctx, boolean interrupted) {}

    /** Shown on Driver Station telemetry while active. */
    default String name() {
        return getClass().getSimpleName();
    }
}
