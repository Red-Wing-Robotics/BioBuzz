package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.ForesightConfig;

/**
 * Ashurbanipal's Pedro values (the second robot). Shared values are in {@link Constants}.
 *
 * NOT TUNED YET. Every value here is a copy of {@link Belteshazzar}'s, so the robot runs but will
 * track and follow paths badly. Before its first auto: measure the pod offsets, then run the
 * Pinpoint and Foresight tuners on this robot (pedro/readme.md), paste the results here, and set
 * {@link #TUNED} to true.
 */
final class Ashurbanipal {

    private Ashurbanipal() {}

    static final boolean TUNED = false;

    // ---- Pinpoint pod offsets ---------------------------------------------------------------
    // TODO: measure. These pods sit differently from Belteshazzar's; these are its values.
    static final double X_POD_OFFSET = Belteshazzar.X_POD_OFFSET;
    static final double Y_POD_OFFSET = Belteshazzar.Y_POD_OFFSET;

    // ---- Foresight ----------------------------------------------------------------------------
    // TODO: run the Foresight Tuner on this robot and paste its config here.
    static final ForesightConfig FORESIGHT = Belteshazzar.FORESIGHT;
}
