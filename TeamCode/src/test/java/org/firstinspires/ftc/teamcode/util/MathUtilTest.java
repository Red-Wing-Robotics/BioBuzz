package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MathUtilTest {

    private static final double EPS = 1e-9;

    @Test
    public void deadbandZeroesSmallInputs() {
        assertEquals(0.0, MathUtil.deadband(0.0, 0.05), EPS);
        assertEquals(0.0, MathUtil.deadband(0.04, 0.05), EPS);
        assertEquals(0.0, MathUtil.deadband(-0.04, 0.05), EPS);
    }

    @Test
    public void deadbandReachesFullScaleAndIsContinuous() {
        assertEquals(1.0, MathUtil.deadband(1.0, 0.05), EPS);
        assertEquals(-1.0, MathUtil.deadband(-1.0, 0.05), EPS);
        // Just past the threshold the output is just above zero — no jump.
        assertEquals(0.0, MathUtil.deadband(0.05, 0.05), EPS);
        assertEquals(0.5, MathUtil.deadband(0.525, 0.05), EPS);
    }
}
