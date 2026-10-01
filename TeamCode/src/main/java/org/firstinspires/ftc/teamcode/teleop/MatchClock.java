package org.firstinspires.ftc.teamcode.teleop;

import java.util.Locale;

/**
 * TELEOP timing: 2:00 long, endgame in the last 60 s, when NECTAR may enter FLOWERS (G410) and
 * all remaining NECTAR may be entered (G426). Hardware-free; {@link TeleOpBase} feeds it the
 * seconds since START.
 *
 * The clock starts when the drivers press START, not with the field timer, so it can only run
 * late — the safe direction for G410. It drives a cue for the drivers, not a lockout.
 */
public final class MatchClock {

    public static final double TELEOP_SECONDS = 120;
    public static final double ENDGAME_SECONDS_LEFT = 60;

    private double elapsed;
    private boolean endgameStarted;

    /** Call once per loop with the seconds since START. */
    public void update(double elapsedSeconds) {
        boolean wasEndgame = isEndgame();
        elapsed = elapsedSeconds;
        endgameStarted = !wasEndgame && isEndgame();
    }

    public double remaining() {
        return Math.max(0.0, TELEOP_SECONDS - elapsed);
    }

    public boolean isEndgame() {
        return remaining() <= ENDGAME_SECONDS_LEFT;
    }

    /** True only in the update where the endgame began: the moment to cue the drivers. */
    public boolean endgameStarted() {
        return endgameStarted;
    }

    /** Time left as m:ss, rounded up like a match timer. */
    public String remainingText() {
        int seconds = (int) Math.ceil(remaining());
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }
}
