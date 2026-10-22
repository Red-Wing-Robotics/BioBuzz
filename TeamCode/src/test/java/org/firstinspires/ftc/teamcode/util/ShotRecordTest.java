package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ShotRecordTest {

    private static ShotRecord sample() {
        ShotRecord r = new ShotRecord();
        r.time = "2026-10-17 15:04:05";
        r.matchTimeLeftSeconds = 83.25;
        r.robot = "Belteshazzar";
        r.alliance = "RED";
        r.cell = "left CELL";
        r.aiming = true;
        r.x = 30.123;
        r.y = 60;
        r.headingDegrees = -12.34;
        r.distance = 48.456;
        r.targetVelocity = 1600;
        r.topVelocity = 1587.6;
        r.bottomVelocity = 1612.2;
        r.aimErrorDegrees = 1.25;
        r.result = "hit";
        return r;
    }

    @Test
    public void rowMatchesTheHeaderColumns() {
        String row = sample().csvRow();
        assertEquals("2026-10-17 15:04:05,83.3,Belteshazzar,RED,left CELL,yes,30.12,60.00,-12.3,48.46,"
                + "1600,1588,1612,1.3,hit", row);
        assertEquals(ShotRecord.HEADER.split(",").length, row.split(",", -1).length);
    }

    @Test
    public void aimErrorIsBlankWhenNotAiming() {
        ShotRecord r = sample();
        r.aiming = false;
        String[] cols = r.csvRow().split(",", -1);
        assertEquals("no", cols[5]);
        assertEquals("", cols[13]);
    }

    @Test
    public void textWithCommasOrQuotesIsQuoted() {
        assertEquals("plain", ShotRecord.text("plain"));
        assertEquals("\"a, b\"", ShotRecord.text("a, b"));
        assertEquals("\"say \"\"hi\"\"\"", ShotRecord.text("say \"hi\""));
        assertEquals("", ShotRecord.text(null));
    }

    @Test
    public void distanceIsStraightLineOnTheFloor() {
        assertEquals(5.0, ShotRecord.distance(1, 2, 4, 6), 1e-9);
    }
}
