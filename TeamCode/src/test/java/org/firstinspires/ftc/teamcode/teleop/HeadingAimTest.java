package org.firstinspires.ftc.teamcode.teleop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.math.Pose;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HeadingAimTest {

    private static final double EPS = 1e-9;
    private static final PoseFactory DEG = PoseFactory.degrees();

    private double kP;
    private double kD;
    private double maxTurn;
    private double minDistance;

    @Before
    public void save() {
        kP = HeadingAim.kP;
        kD = HeadingAim.kD;
        maxTurn = HeadingAim.maxTurn;
        minDistance = HeadingAim.minDistance;
        HeadingAim.kP = 1.0;
        HeadingAim.kD = 0;
        HeadingAim.maxTurn = 0.8;
        HeadingAim.minDistance = 6;
    }

    @After
    public void restore() {
        HeadingAim.kP = kP;
        HeadingAim.kD = kD;
        HeadingAim.maxTurn = maxTurn;
        HeadingAim.minDistance = minDistance;
    }

    @Test
    public void facingTheTargetNeedsNoTurn() {
        HeadingAim aim = new HeadingAim();
        assertEquals(0, aim.turn(DEG.of(10, 10, 45), 50, 50, 0.02), EPS);
    }

    @Test
    public void targetCounterClockwiseTurnsCounterClockwise() {
        // Facing +x, target straight ahead and a little to the left (+y): small positive turn.
        HeadingAim aim = new HeadingAim();
        double turn = aim.turn(DEG.of(0, 0, 0), 100, 10, 0.02);
        assertEquals(Math.atan2(10, 100), turn, EPS);
        assertTrue(new HeadingAim().turn(DEG.of(0, 0, 0), 100, -10, 0.02) < 0);
    }

    @Test
    public void takesTheShortWayRoundAcrossPlusMinus180() {
        // Facing 170°, target at −170° (20° counter-clockwise): turn counter-clockwise, not 340° clockwise.
        Pose robot = DEG.of(0, 0, 170);
        double bearing = Math.toRadians(-170);
        HeadingAim aim = new HeadingAim();
        aim.turn(robot, 100 * Math.cos(bearing), 100 * Math.sin(bearing), 0.02);
        assertEquals(Math.toRadians(20), aim.error(), 1e-6);
    }

    @Test
    public void turnIsLimitedToMaxTurn() {
        assertEquals(-0.8, new HeadingAim().turn(DEG.of(0, 0, 0), 0, -100, 0.02), EPS);
        HeadingAim.maxTurn = 5; // a bad Panels edit
        assertEquals(-1.0, new HeadingAim().turn(DEG.of(0, 0, 0), 0, -100, 0.02), EPS);
    }

    @Test
    public void stopsTurningRightOnTopOfTheTarget() {
        assertEquals(0, new HeadingAim().turn(DEG.of(50, 50, 0), 52, 48, 0.02), EPS);
    }

    @Test
    public void derivativeDampsOnlyAfterTheFirstLoop() {
        HeadingAim.kP = 0;
        HeadingAim.kD = 0.1;
        HeadingAim aim = new HeadingAim();
        assertEquals("no kick on the first loop", 0, aim.turn(DEG.of(0, 0, 0), 100, 100, 0.02), EPS);
        // Error shrinks from 45° to 35° in 0.1 s: −100°/s, so the D term pushes back.
        assertEquals(0.1 * Math.toRadians(-100), aim.turn(DEG.of(0, 0, 10), 100, 100, 0.1), 1e-6);
        aim.reset();
        assertEquals(0, aim.turn(DEG.of(0, 0, 20), 100, 100, 0.1), EPS);
    }
}
