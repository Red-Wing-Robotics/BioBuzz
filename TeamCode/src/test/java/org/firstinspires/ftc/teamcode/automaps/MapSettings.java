package org.firstinspires.ftc.teamcode.automaps;

/**
 * The numbers behind docs/autos.pdf. Change them here and in docs/auto-maps.md together.
 */
final class MapSettings {

    /** Robot footprint, inches. 18 × 18 is the R102 starting-configuration maximum; use the real size once measured. */
    static final double ROBOT_LENGTH = 18;
    static final double ROBOT_WIDTH = 18;

    /**
     * Time model for one {@code follow}: accelerate at {@link #ACCEL} up to {@link #SPEED},
     * decelerate at the same rate, then {@link #SETTLE} seconds to settle. Rough by design;
     * calibrate by timing real runs.
     */
    static final double SPEED = 50;     // in/s (tuned max forward velocity is 69.6)
    static final double ACCEL = 37;     // in/s² (tuned natural forward deceleration)
    static final double SETTLE = 0.25;  // s

    static final double AUTO_SECONDS = 30;

    /** Points (RULES.md §4). HIVE tips need launch actions, which no route has yet. */
    static final int LEAVE_POINTS = 3;
    static final int PARK_POINTS = 5;
    static final int TIP_POINTS = 20;

    /**
     * How far a {@code seek} without a {@code within} arg looks, inches: the default of
     * VisionTuning.within (subsystems/, which the maps can't use). Change both together.
     */
    static final double SEEK_WITHIN = 36;

    /** Samples per path when drawing it and checking where the robot goes. */
    static final int PATH_SAMPLES = 60;

    private MapSettings() {}
}
