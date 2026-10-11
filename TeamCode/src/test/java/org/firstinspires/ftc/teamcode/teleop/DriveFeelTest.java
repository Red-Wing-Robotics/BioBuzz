package org.firstinspires.ftc.teamcode.teleop;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DriveFeelTest {

    private static final double EPS = 1e-9;

    private double deadband;
    private double exponent;
    private double slowScale;

    @Before
    public void save() {
        deadband = DriveFeel.deadband;
        exponent = DriveFeel.exponent;
        slowScale = DriveFeel.slowScale;
        DriveFeel.deadband = 0.05;
        DriveFeel.exponent = 1.0;
        DriveFeel.slowScale = 0.35;
    }

    @After
    public void restore() {
        DriveFeel.deadband = deadband;
        DriveFeel.exponent = exponent;
        DriveFeel.slowScale = slowScale;
    }

    @Test
    public void linearCurveIsJustTheDeadband() {
        assertEquals(0.0, DriveFeel.shape(0.03), EPS);
        assertEquals(0.5, DriveFeel.shape(0.525), EPS);
        assertEquals(-1.0, DriveFeel.shape(-1.0), EPS);
    }

    @Test
    public void curveKeepsSignAndFullScale() {
        DriveFeel.exponent = 2.0;
        assertEquals(0.25, DriveFeel.shape(0.525), EPS);
        assertEquals(-0.25, DriveFeel.shape(-0.525), EPS);
        assertEquals(1.0, DriveFeel.shape(1.0), EPS);
        assertEquals(-1.0, DriveFeel.shape(-1.0), EPS);
    }

    @Test
    public void centeredStickNeverDrives() {
        DriveFeel.exponent = 0.0; // a bad Panels edit must not make pow(0, 0) = 1
        assertEquals(0.0, DriveFeel.shape(0.0), EPS);
        assertEquals(0.0, DriveFeel.shape(0.02), EPS);
    }

    @Test
    public void badPanelsEditsStayDrivable() {
        DriveFeel.deadband = 1.0; // would be 0 / 0 at full stick
        assertEquals(1.0, DriveFeel.shape(1.0), EPS);
        assertEquals(0.0, DriveFeel.shape(DriveFeel.MAX_DEADBAND - 0.01), EPS);

        DriveFeel.deadband = 0.05;
        double small = (0.1 - 0.05) / 0.95; // a light touch after the deadband
        for (double bad : new double[] {0.0, -1.0}) {
            DriveFeel.exponent = bad; // 0 would make any touch full power
            assertEquals(Math.pow(small, DriveFeel.MIN_EXPONENT), DriveFeel.shape(0.1), EPS);
        }

        DriveFeel.slowScale = -0.5;
        assertEquals(0.0, DriveFeel.slowModeScale(), EPS);
        DriveFeel.slowScale = 3;
        assertEquals(1.0, DriveFeel.slowModeScale(), EPS);
    }
}
