package org.firstinspires.ftc.teamcode.vision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.firstinspires.ftc.teamcode.auto.Alliance;
import org.junit.Test;

public class PickupRulesTest {

    @Test
    public void opponentNectarIsNeverWanted() {
        for (PickupRules.Colors c : PickupRules.Colors.values()) {
            assertFalse(c.wants(BallKind.BLUE_NECTAR, Alliance.RED));
            assertFalse(c.wants(BallKind.RED_NECTAR, Alliance.BLUE));
        }
        assertTrue(PickupRules.Colors.OURS.wants(BallKind.RED_NECTAR, Alliance.RED));
        assertTrue(PickupRules.Colors.OURS.wants(BallKind.POLLEN, Alliance.BLUE));
        assertFalse(PickupRules.Colors.POLLEN.wants(BallKind.BLUE_NECTAR, Alliance.BLUE));
        assertFalse(PickupRules.Colors.OUR_NECTAR.wants(BallKind.POLLEN, Alliance.RED));
    }

    @Test
    public void routeArgsNameTheColours() {
        assertEquals(PickupRules.Colors.POLLEN, PickupRules.Colors.parse("pollen"));
        assertEquals(PickupRules.Colors.OUR_NECTAR, PickupRules.Colors.parse("nectar"));
        assertEquals(PickupRules.Colors.OURS, PickupRules.Colors.parse("ours"));
        assertNull(PickupRules.Colors.parse("yellow"));
    }

    @Test
    public void eachRuleSaysWhy() {
        assertTrue(PickupRules.whyNot(BallKind.BLUE_NECTAR, 30, 30, Alliance.RED, false, 0).contains("G408"));
        assertTrue(PickupRules.whyNot(BallKind.POLLEN, -1, 30, Alliance.RED, false, 0).contains("outside"));
        assertTrue(PickupRules.whyNot(BallKind.POLLEN, 72, 72, Alliance.RED, false, 0).contains("G409"));
        assertTrue("near the HIVE with a margin",
                PickupRules.whyNot(BallKind.POLLEN, 45, 72, Alliance.RED, false, 3).contains("G409"));
        assertNull("TeleOp may cross the field", PickupRules.whyNot(BallKind.POLLEN, 100, 20, Alliance.RED, false, 0));
        assertTrue(PickupRules.whyNot(BallKind.POLLEN, 100, 20, Alliance.RED, true, 0).contains("G402"));
        assertNull(PickupRules.whyNot(BallKind.POLLEN, 30, 20, Alliance.RED, true, 0));
    }

    @Test
    public void blueOwnsTheHalfAboveTheCentreLine() {
        assertNull(PickupRules.whyNot(BallKind.BLUE_NECTAR, 120, 120, Alliance.BLUE, true, 0));
        assertTrue(PickupRules.whyNot(BallKind.POLLEN, 30, 120, Alliance.BLUE, true, 0).contains("G402"));
    }
}
