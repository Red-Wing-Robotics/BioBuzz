package org.firstinspires.ftc.teamcode.auto;

import com.pedropathing.follower.Follower;

import org.firstinspires.ftc.teamcode.RobotHardware;

import java.util.function.DoubleSupplier;

/** What a running {@link Step} can touch. Unit tests pass {@code null} hardware and a fake clock. */
public final class AutoContext {
    public final RobotHardware robot;
    public final Follower follower;
    private final DoubleSupplier clock;

    public AutoContext(RobotHardware robot, Follower follower, DoubleSupplier clock) {
        this.robot = robot;
        this.follower = follower;
        this.clock = clock;
    }

    /** Seconds since the routine started. */
    public double time() {
        return clock.getAsDouble();
    }
}
