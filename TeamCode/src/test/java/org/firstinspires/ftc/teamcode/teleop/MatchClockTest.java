package org.firstinspires.ftc.teamcode.teleop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MatchClockTest {

    private final MatchClock clock = new MatchClock();

    @Test
    public void countsDownFromTwoMinutes() {
        clock.update(0);
        assertEquals("2:00", clock.remainingText());
        clock.update(55.2);
        assertEquals("1:05", clock.remainingText()); // 64.8 s rounds up, like the field timer
        clock.update(130);
        assertEquals(0.0, clock.remaining(), 1e-9);
        assertEquals("0:00", clock.remainingText());
    }

    @Test
    public void endgameIsTheLastMinute() {
        clock.update(59.9);
        assertFalse(clock.isEndgame());
        clock.update(60);
        assertTrue(clock.isEndgame());
    }

    @Test
    public void endgameCueFiresOnce() {
        clock.update(59);
        assertFalse(clock.endgameStarted());
        clock.update(60.02);
        assertTrue(clock.endgameStarted());
        clock.update(60.04);
        assertFalse(clock.endgameStarted());
    }
}
