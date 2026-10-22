package org.firstinspires.ftc.teamcode.vision;

import java.util.List;

/** Where the balls are: what {@link DriveToBall} reads. The Vision subsystem is one; tests fake it. */
public interface Balls {

    /** The balls seen often enough to trust, alliance frame. */
    List<BallTracker.Ball> confirmed();

    /** The tracked ball with this id, or null once it has been forgotten. */
    BallTracker.Ball find(int id);
}
