package org.firstinspires.ftc.teamcode.teleop;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DriveFeelTest {

    private static final double EPS = 1e-9;

    private double deadband;
    private double exponent;

    @Before
    public void save() {
        deadband = DriveFeel.deadband;
        exponent = DriveFeel.exponent;
        DriveFeel.deadband = 0.05;
        DriveFeel.exponent = 1.0;
    }

    @After
    public void restore() {
        DriveFeel.deadband = deadband;
        DriveFeel.exponent = exponent;
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
}
