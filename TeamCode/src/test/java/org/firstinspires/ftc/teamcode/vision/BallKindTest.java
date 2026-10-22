package org.firstinspires.ftc.teamcode.vision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.junit.Test;

public class BallKindTest {

    @Test
    public void theLabelWinsOverTheClassId() {
        assertEquals(BallKind.BLUE_NECTAR, BallKind.of("blue_nectar", 0));
        assertNull("a label this model doesn't have", BallKind.of("robot_bumper", 1));
        assertEquals("no label: the id", BallKind.RED_NECTAR, BallKind.of("", 1));
        assertNull(BallKind.of(null, 7));
    }

    @Test
    public void centresAreHalfTheDiameterNotHiveVisionsThreeInches() {
        assertEquals(1.4, BallKind.POLLEN.centreHeight(), 1e-9);
        assertEquals(1.8, BallKind.RED_NECTAR.centreHeight(), 1e-9);
    }

    @Test
    public void opponentNectarDependsOnTheAlliance() {
        assertTrue(BallKind.BLUE_NECTAR.isOpponentNectar(Alliance.RED));
        assertTrue(BallKind.RED_NECTAR.isOpponentNectar(Alliance.BLUE));
        assertTrue(!BallKind.POLLEN.isOpponentNectar(Alliance.RED) && !BallKind.POLLEN.isOpponentNectar(Alliance.BLUE));
    }
}
